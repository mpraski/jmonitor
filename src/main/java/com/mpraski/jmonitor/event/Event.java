package com.mpraski.jmonitor.event;

import java.util.Objects;

public final class Event {
	public Event(String tag, EventType type, Object target, Object[] arguments, StackTraceElement[] callstack) {
		this.tag = tag;
		this.type = type;
		this.target = target;
		this.arguments = arguments;
		this.callstack = callstack;
	}

	// Optional tag of the event
	private final String tag;
	// Specified type of event
	private final EventType type;
	// Value of the event target - if any
	private final Object target;
	// Arguments passed to the event target
	private final Object[] arguments;
	// Call stack of the thread of the methods that caused the event
	private final StackTraceElement[] callstack;

	public Object passThrough() {
		return null;
	}

	public Object passThrough(Object[] args) {
		return null;
	}

	public String getTag() {
		return tag;
	}

	public EventType getType() {
		return type;
	}

	public Object getTarget() {
		return target;
	}

	public Object[] getArguments() {
		return arguments;
	}

	public StackTraceElement[] getCallstack() {
		return callstack;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		sb.append("Type: '" + this.type + "', Tag: '" + Objects.toString(tag, "null") + "', Target: '"
				+ Objects.toString(target, "null") + "', Arguments: [ ");

		if (arguments != null)
			for (Object o : arguments) {
				sb.append(o);
				sb.append(' ');
			}

		sb.append(']');

		return sb.toString();
	}
}
