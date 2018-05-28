package com.mpraski.jmonitor.common;

import com.mpraski.jmonitor.event.Event;

public interface Monitor {
	void onEvent(Event event);
}
