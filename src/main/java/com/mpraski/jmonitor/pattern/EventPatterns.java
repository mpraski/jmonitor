package com.mpraski.jmonitor.pattern;

import com.mpraski.jmonitor.event.EventType;

public class EventPatterns {
	public static EventPattern onMethodCall() {
		return new EventPattern(EventType.METHOD_CALL);
	}

	public static EventPattern onFieldRead() {
		return new EventPattern(EventType.FIELD_READ);
	}

	public static EventPattern onFieldWrite() {
		return new EventPattern(EventType.FIELD_WRITE);
	}

	public static EventPattern onReturn() {
		return new EventPattern(EventType.RETURN);
	}

	public static EventPattern onThrow() {
		return new EventPattern(EventType.METHOD_CALL);
	}

	public static EventPattern onInstanceCreated() {
		return new EventPattern(EventType.INSTANCE);
	}

	public static EventPattern onArrayCreated() {
		return new EventPattern(EventType.INSTANCE_ARRAY);
	}

	public static EventPattern onMonitorEnter() {
		return new EventPattern(EventType.MONITOR_ENTER);
	}

	public static EventPattern onMonitorExit() {
		return new EventPattern(EventType.MONITOR_EXIT);
	}

	public static EventPattern onAnyEvent() {
		return new EventPattern(EventType.ANY);
	}
}
