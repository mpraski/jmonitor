package com.mpraski.dummy;

import com.mpraski.jmonitor.Event;
import com.mpraski.jmonitor.InsteadMonitor;

public class DummyMonitor11 implements InsteadMonitor {

	@Override
	public Object doInstead(Event event) {
		System.out.println("DummyMonitor11 :: " + event);

		event.getArguments()[0] = 42;

		return event.passThrough(event.getArguments());
	}

}
