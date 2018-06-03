package com.mpraski.dummy;

import com.mpraski.jmonitor.common.Monitor;
import com.mpraski.jmonitor.event.Event;

public class DummyMonitor2 implements Monitor {

	@Override
	public void onEvent(Event event) {
		System.out.println("Received event: " + (Integer) event.getTarget());
	}

}
