package com.mpraski.jmonitor.event;

public final class Event {
	public Event(String tag, EventType type, String signature, Object target, Object value, Object[] arguments,
			Object passThrough) {
		super();
		this.tag = tag;
		this.type = type;
		this.signature = signature;
		this.target = target;
		this.value = value;
		this.arguments = arguments;
		this.passThrough = passThrough;
	}

	private final String tag;
	private final EventType type;
	private final String signature;
	private final Object target;
	private final Object value;
	private final Object[] arguments;
	// private final StackFrame[] callstack;
	private final Object passThrough;

}
