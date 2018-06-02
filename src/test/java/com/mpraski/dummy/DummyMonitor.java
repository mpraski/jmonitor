package com.mpraski.dummy;

import com.mpraski.jmonitor.common.Monitor;
import com.mpraski.jmonitor.event.Event;

public class DummyMonitor implements Monitor {

	@Override
	public void onEvent(Event event) {
		System.out.println("Received event: " + (String) event.getTarget());
	}

}
