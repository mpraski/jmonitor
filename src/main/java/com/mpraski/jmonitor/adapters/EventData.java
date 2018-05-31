package com.mpraski.jmonitor.adapters;

import com.mpraski.jmonitor.event.EventType;

public final class EventData {
	public EventData(EventType type, String tag, String signature, String monitor) {
		this.type = type;
		this.tag = tag;
		this.signature = signature;
		this.monitor = monitor;
	}

	public EventData(EventType type, String tag, String signature, String monitor, String name, String desc,
			String owner) {
		this.type = type;
		this.tag = tag;
		this.signature = signature;
		this.monitor = monitor;
		this.name = name;
		this.desc = desc;
		this.owner = owner;
	}

	private final EventType type;
	private final String tag;
	private final String signature;
	private final String monitor;
	private String name;
	private String desc;
	private String owner;

	public EventType getType() {
		return type;
	}

	public String getTag() {
		return tag;
	}

	public String getSignature() {
		return signature;
	}

	public String getMonitor() {
		return monitor;
	}

	public String getName() {
		return name;
	}

	public String getDesc() {
		return desc;
	}

	public String getOwner() {
		return owner;
	}
}
