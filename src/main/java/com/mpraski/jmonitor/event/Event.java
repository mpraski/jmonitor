package com.mpraski.jmonitor.event;

public final class Event {
	private final String tag;
	private final EventType type;
	private final EventContext context;

	public Event(String tag, EventType type, EventContext context) {
		this.tag = tag;
		this.type = type;
		this.context = context;
	}

	public String getTag() {
		return tag;
	}

	public EventType getType() {
		return type;
	}

	public EventContext getContext() {
		return context;
	}
}
