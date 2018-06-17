package com.mpraski.dummy;

import com.mpraski.jmonitor.Event;
import com.mpraski.jmonitor.InsteadMonitor;

public class DummyMonitor10 implements InsteadMonitor {

	@Override
	public Object doInstead(Event event) {
		System.out.println("DummyMonitor10 :: " + event);

		event.getArguments()[0] = new String("Majonez");

		return event.passThrough(event.getArguments());
	}

}
