package com.mpraski.dummy;

import com.mpraski.jmonitor.Event;
import com.mpraski.jmonitor.InsteadMonitor;

public class DummyMonitor5 implements InsteadMonitor {

	@Override
	public Object doInstead(Event event) {
		System.out.println("Catching event5: " + event);

		return new Integer(10045);
	}

}
