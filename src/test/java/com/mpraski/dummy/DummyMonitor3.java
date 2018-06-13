package com.mpraski.dummy;

import com.mpraski.jmonitor.Event;
import com.mpraski.jmonitor.Monitor;

public class DummyMonitor3 implements Monitor {

	@Override
	public void onEvent(Event event) {
		System.out.println("DummyMonitor3 :: " + event);
	}

}
