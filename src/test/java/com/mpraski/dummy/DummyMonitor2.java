package com.mpraski.dummy;

import com.mpraski.jmonitor.Event;
import com.mpraski.jmonitor.Monitor;

public class DummyMonitor2 implements Monitor {

	@Override
	public void onEvent(Event event) {
		System.out.println("Received event2: " + event);
	}

}
