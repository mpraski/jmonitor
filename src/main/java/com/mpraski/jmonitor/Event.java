package com.mpraski.jmonitor;

public final class Event {
	public Event(
			String tag,
			EventType type,
			EventOrder order,
			String source,
			int lineNumber,
			Object target,
			Object[] arguments,
			StackTraceElement[] callstack) {
		this.tag = tag;
		this.type = type;
		this.order = order;
		this.source = source;
		this.lineNumber = lineNumber;
		this.target = target;
		this.arguments = arguments;
		this.callstack = callstack;
	}

	public Event(
			String tag,
			EventType type,
			EventOrder order,
			String source,
			int lineNumber,
			InsteadAction action,
			Object[] arguments,
			StackTraceElement[] callstack) {
		this.tag = tag;
		this.type = type;
		this.order = order;
		this.source = source;
		this.lineNumber = lineNumber;
		this.target = null;
		this.action = action;
		this.arguments = arguments;
		this.callstack = callstack;
	}

	// Optional tag of the event
	private final String tag;
	// Specified type of event
	private final EventType type;
	// Order of the event
	private final EventOrder order;
	// Name of the source file
	private final String source;
	// Number of line where instrumented operation was executed
	private final int lineNumber;
	// Value of the event target - if any
	private final Object target;
	// Arguments passed to the event target
	private final Object[] arguments;
	// Call stack of the thread of the methods that caused the event
	private final StackTraceElement[] callstack;

	private InsteadAction action;

	public Object passThrough() {
		return action.doAction(null);
	}

	public Object passThrough(Object[] arguments) {
		return action.doAction(arguments);
	}

	public String getTag() {
		return tag;
	}

	public EventType getType() {
		return type;
	}

	public EventOrder getOrder() {
		return order;
	}

	public String getSourceFile() {
		return source;
	}

	public int getLineNumber() {
		return lineNumber;
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
		StringBuilder sb = new StringBuilder();

		sb.append("Tag: ");
		sb.append(tag);
		sb.append(", Type: ");
		sb.append(type);
		sb.append(", Order: ");
		sb.append(order);
		sb.append(", Source: ");
		sb.append(source);
		sb.append(", Line: ");
		sb.append(lineNumber);
		sb.append(", Target: ");
		sb.append(target);
		sb.append(", Arguments: [ ");

		if (arguments != null)
			for (Object o : arguments) {
				sb.append(o);
				sb.append(' ');
			}

		sb.append(']');

		return sb.toString();
	}
}
