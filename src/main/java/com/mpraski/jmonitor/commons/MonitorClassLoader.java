package com.mpraski.jmonitor.commons;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.mpraski.jmonitor.EventPatternCompiler;
import com.mpraski.jmonitor.EventPatternDefinitions;
import com.mpraski.jmonitor.EventPatternMatcher;
import com.mpraski.jmonitor.EventType;
import com.mpraski.jmonitor.ResolverGenerator;
import com.mpraski.jmonitor.adapters.MonitorClassAdapter;
import com.mpraski.jmonitor.instead.InsteadActionGenerator;

public final class MonitorClassLoader extends ClassLoader {
	static {
		ClassLoader.registerAsParallelCapable();
	}

	private final Map<String, Class<?>> transformedClasses = new ConcurrentHashMap<>();
	private final Map<String, Object> classLocks = new ConcurrentHashMap<>();

	private final List<EventPatternMatcher> matchers;
	private final Map<EventType, List<EventPatternMatcher>> mapped;

	private final static String RESOLVER_CLASS = "com.mpraski.jmonitor.Resolver";

	public MonitorClassLoader(ClassLoader parent, String definitionsName) {
		super(parent);

		Class<EventPatternDefinitions> defClass = loadClassOfType(definitionsName);

		if (defClass == null)
			throw new NullPointerException("Class of EventPatternDefinitions is null");

		EventPatternDefinitions defInstance = instantiateClassOfType(defClass);

		if (defInstance == null)
			throw new NullPointerException("Instance of EventPatternDefinitions is null");

		EventPatternCompiler compiler = new EventPatternCompiler();
		compiler.compile(defInstance.getEventPatterns());

		this.matchers = compiler.getMatchers();
		this.mapped = new EnumMap<>(EventType.class);

		for (EventType t : EventType.values())
			mapped.put(t, new ArrayList<>());
		for (EventPatternMatcher m : matchers)
			mapped.get(m.getType()).add(m);

		byte[] resolverBytes = ResolverGenerator.generate(compiler.getMonitors());
		if (resolverBytes == null)
			throw new NullPointerException("Could not generate Resolver class");

		if (defineClass(RESOLVER_CLASS, resolverBytes) == null)
			throw new NullPointerException("Could not define Resolver class");

		compiler.clear();
	}

	private Object getLockForName(String name) {
		return classLocks.computeIfAbsent(name, k -> new Object());
	}

	private <T> T instantiateClassOfType(Class<T> clazz) {
		T t = null;

		try {
			t = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			System.err.println("Error: Unable to instantiate class " + clazz.getName());
			e.printStackTrace();
		}

		return t;
	}

	// Assuming caller knows the type of loaded class
	@SuppressWarnings("unchecked")
	private <T> Class<T> loadClassOfType(String name) {
		Class<T> clazz = null;

		try {
			clazz = (Class<T>) super.loadClass(name, true);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return clazz;
	}

	private Class<?> defineClass(String name, byte[] b) {
		return defineClass(name, b, 0, b.length);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (name.startsWith("java") || name.startsWith("com.mpraski.jmonitor"))
			return super.loadClass(name, resolve);

		synchronized (getLockForName(name)) {
			Class<?> clazz = transformedClasses.get(name);

			if (clazz != null)
				return clazz;

			try (InputStream is = getResourceAsStream(name.replace('.', '/') + ".class")) {
				byte[] classBytes = new byte[is.available()];
				is.read(classBytes);

				ClassReader cr = new ClassReader(classBytes);
				ClassWriter cw = new ClassWriter(cr, 0);
				MonitorClassAdapter adapter = new MonitorClassAdapter(cw, matchers, mapped);
				cr.accept(adapter, ClassReader.EXPAND_FRAMES);

				clazz = defineClass(name, cw.toByteArray());

				for (InsteadActionGenerator g : adapter.getActionGenerators())
					defineClass(g.getName(), g.generate());
			} catch (IOException e) {
				System.err.println("Error: Unable to load definitions class " + name);
				e.printStackTrace();
			} catch (ClassFormatError e) {
				System.err.println("Error: Invalid definitions class " + name);
				e.printStackTrace();
			}

			if (resolve)
				resolveClass(clazz);

			transformedClasses.put(name, clazz);

			return clazz;
		}
	}

}
