package com.mpraski.dummy;

import com.mpraski.jmonitor.common.Monitor;
import com.mpraski.jmonitor.event.Event;

public class DummyMonitor2 implements Monitor {

	@Override
	public void onEvent(Event event) {
		System.out.println("Received event2: " + event.getTarget());
		System.out.println("Received event2 write: " + event.getArguments()[0]);
		// System.out.println("Target: " + event.getArguments()[0]);
		// for (StackTraceElement m : event.getCallstack()) {
		// System.out.println(m);
		// }
	}

}
