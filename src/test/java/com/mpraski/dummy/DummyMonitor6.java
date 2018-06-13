package com.mpraski.dummy;

import com.mpraski.jmonitor.Event;
import com.mpraski.jmonitor.InsteadMonitor;

public class DummyMonitor6 implements InsteadMonitor {

	@Override
	public Object doInstead(Event event) {
		System.out.println("DummyMonitor6 :: " + event);

		return event.getTarget();
	}

}
