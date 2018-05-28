package com.mpraski.jmonitor.common;

import java.util.HashMap;
import java.util.Map;

public final class Resolver {
	private final static Map<String, Monitor> monitors = new HashMap<>();
	private final static Map<String, InsteadMonitor> insteadMonitors = new HashMap<>();

	@SuppressWarnings("unchecked")
	public static Monitor getMonitor(String name) {
		if (monitors.containsKey(name))
			return monitors.get(name);

		Class<Monitor> clazz = null;
		try {
			clazz = (Class<Monitor>) Class.forName(name);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (clazz == null)
			return null;

		Monitor m = null;
		try {
			m = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			System.err.println("Error: Unable to instantiate class " + clazz.getName());
			e.printStackTrace();
		}

		if (m != null)
			monitors.put(name, m);

		return m;
	}

	@SuppressWarnings("unchecked")
	public static InsteadMonitor getInsteadMonitor(String name) {
		if (insteadMonitors.containsKey(name))
			return insteadMonitors.get(name);

		Class<InsteadMonitor> clazz = null;
		try {
			clazz = (Class<InsteadMonitor>) Class.forName(name);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (clazz == null)
			return null;

		InsteadMonitor m = null;
		try {
			m = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			System.err.println("Error: Unable to instantiate class " + clazz.getName());
			e.printStackTrace();
		}

		if (m != null)
			insteadMonitors.put(name, m);

		return m;
	}
}
