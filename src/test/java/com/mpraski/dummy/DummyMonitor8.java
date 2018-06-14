package com.mpraski.dummy;

import com.mpraski.jmonitor.Event;
import com.mpraski.jmonitor.InsteadMonitor;

public class DummyMonitor8 implements InsteadMonitor {

	@Override
	public Object doInstead(Event event) {
		System.out.println("DummyMonitor8 :: " + event);

		System.out.println("Value of passthrough: " + event.passThrough());

		return new Long(4321);
	}

}
