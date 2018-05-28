package com.mpraski.jmonitor.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mpraski.jmonitor.pattern.EventPatternDefinitions;

public final class MonitorClassLoader extends ClassLoader {
	static {
		ClassLoader.registerAsParallelCapable();
	}

	private final Map<String, Class<?>> transformedClasses = new ConcurrentHashMap<>();
	private final Map<String, Object> classLocks = new ConcurrentHashMap<>();

	private final static String[] FRAMEWORK_CLASSES = new String[] {};

	public MonitorClassLoader(String definitionsClass) {
		Class<EventPatternDefinitions> clazz = loadClassOfType(definitionsClass);
		EventPatternDefinitions definitions = instantiateClassOfType(clazz);
	}

	private Object getLockForName(String name) {
		return classLocks.computeIfAbsent(name, k -> new Object());
	}

	private void loadFrameworkClasses() {
		for (String name : FRAMEWORK_CLASSES) {
			synchronized (getLockForName(name)) {
				Class<?> clazz = null;
				try {
					clazz = getParent().loadClass(name);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}

				transformedClasses.put(name, clazz);
			}
		}
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

	private <T> Class<T> loadClassOfType(String name) {
		Class<T> clazz = null;
		InputStream is = null;
		byte[] classBytes;

		try {
			is = getResourceAsStream(name);
			classBytes = new byte[is.available()];
			is.read(classBytes);

			clazz = (Class<T>) defineClass(name, classBytes, 0, classBytes.length);
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

		return clazz;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getLockForName(name)) {
			Class<?> clazz = transformedClasses.get(name);

			if (clazz != null) {
				return clazz;
			}

			byte[] classBytes, transformedBytes;
			InputStream is = null;
			try {
				if (!name.startsWith("java.")) {
					is = getResourceAsStream(name);
					classBytes = new byte[is.available()];
					is.read(classBytes);

					// transformedBytes = classTransformer.transform(classBytes);

					// clazz = defineClass(name, transformedBytes, 0, transformedBytes.length);
				} else {
					clazz = findSystemClass(name);
				}
			} catch (IOException e) {
				System.err.println("Error: Unable to load definitions class " + name);
				e.printStackTrace();
			} catch (ClassFormatError e) {
				System.err.println("Error: Invalid definitions class " + name);
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				System.err.println("Error: Cannot find class " + name);
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
