package com.mpraski.jmonitor.common;

import com.mpraski.jmonitor.event.Event;

public interface InsteadMonitor {
	Object doInstead(Event event);
}
