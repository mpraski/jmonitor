package com.mpraski.jmonitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.mpraski.jmonitor.common.MonitorClassLoader;

public class TestDriver {
	public static void main(String[] args) {
		new TestDriver().run();
	}

	public void run() {
		MonitorClassLoader cl = new MonitorClassLoader(getClass().getClassLoader(),
				"com.mpraski.jmonitor.TestDefinitions");

		Thread.currentThread().setContextClassLoader(cl);

		Class loaded = null;
		try {
			loaded = cl.loadClass("com.mpraski.dummy.Example");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Method meth;
		try {
			meth = loaded.getMethod("main", String[].class);
			String[] params = null;
			meth.invoke(null, (Object) params);
		} catch (NoSuchMethodException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}