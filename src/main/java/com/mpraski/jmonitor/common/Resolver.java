package com.mpraski.jmonitor.common;

import java.util.HashMap;
import java.util.Map;

public final class Resolver {
	private final static Map<String, Monitor> monitors = new HashMap<>();
	private final static Map<String, InsteadMonitor> insteadMonitors = new HashMap<>();

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

		Monitor m = null;
		try {
			m = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			System.err.println("Error: Unable to instantiate class " + clazz.getName());
			e.printStackTrace();
		}

		monitors.put(name, m);

		return m;
	}

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

		InsteadMonitor m = null;
		try {
			m = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			System.err.println("Error: Unable to instantiate class " + clazz.getName());
			e.printStackTrace();
		}

		insteadMonitors.put(name, m);

		return m;
	}
}
