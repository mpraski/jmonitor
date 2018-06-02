package com.mpraski.jmonitor.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import com.mpraski.jmonitor.adapters.MonitorClassAdapter;
import com.mpraski.jmonitor.event.EventType;
import com.mpraski.jmonitor.pattern.EventPatternCompiler;
import com.mpraski.jmonitor.pattern.EventPatternDefinitions;
import com.mpraski.jmonitor.pattern.EventPatternMatcher;

public final class MonitorClassLoader extends ClassLoader {
	static {
		ClassLoader.registerAsParallelCapable();
	}

	private final Map<String, Class<?>> transformedClasses = new ConcurrentHashMap<>();
	private final Map<String, Object> classLocks = new ConcurrentHashMap<>();

	private final EventPatternCompiler compiler;
	private final List<EventPatternMatcher> matchers;
	private final Map<EventType, List<EventPatternMatcher>> mapped;

	private final static String RESOLVER_CLASS = "com.mpraski.jmonitor.common.Resolver";
	private final Class resolverClass;

	public MonitorClassLoader(ClassLoader parent, String definitionsName) {
		super(parent);

		Class<EventPatternDefinitions> defClass = loadClassOfType(definitionsName);

		if (defClass == null)
			throw new NullPointerException("Class of EventPatternDefinitions is null");

		EventPatternDefinitions defInstance = instantiateClassOfType(defClass);

		if (defInstance == null)
			throw new NullPointerException("Instance of EventPatternDefinitions is null");

		this.compiler = new EventPatternCompiler();
		this.compiler.compile(defInstance.getEventPatterns());

		this.matchers = compiler.getMatchers();
		this.mapped = matchers.stream().collect(Collectors.groupingBy(EventPatternMatcher::getType));

		byte[] resolverBytes = ResolverWriter.write(compiler.getMonitors());
		if (resolverBytes == null)
			throw new NullPointerException("Could not generate Resolver class");

		resolverClass = defineClass(RESOLVER_CLASS, resolverBytes);

		if (resolverClass == null)
			throw new NullPointerException("Could not define Resolver class");
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

	public Class defineClass(String name, byte[] b) {
		return defineClass(name, b, 0, b.length);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (name.startsWith("java") || name.startsWith("com.mpraski.jmonitor")) {
			if (name.equals(RESOLVER_CLASS)) {
				return resolverClass;
			}

			return super.loadClass(name, resolve);
		}

		synchronized (getLockForName(name)) {
			Class<?> clazz = transformedClasses.get(name);

			if (clazz != null) {
				return clazz;
			}

			byte[] classBytes;
			InputStream is = null;
			try {
				is = getResourceAsStream(name);
				classBytes = new byte[is.available()];
				is.read(classBytes);

				ClassReader cr = new ClassReader(classBytes);
				ClassWriter cw = new ClassWriter(cr, 0);
				ClassVisitor cv = new MonitorClassAdapter(cw, matchers, mapped);
				cr.accept(cv, 0);

				clazz = defineClass(name, cw.toByteArray());
			} catch (IOException e) {
				System.err.println("Error: Unable to load definitions class " + name);
				e.printStackTrace();
			} catch (ClassFormatError e) {
				System.err.println("Error: Invalid definitions class " + name);
				e.printStackTrace();
			} finally {
				if (is != null)
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}

			if (resolve) {
				resolveClass(clazz);
			}

			transformedClasses.put(name, clazz);

			return clazz;
		}
	}

}
