package com.mpraski.jmonitor.adapters;

import org.objectweb.asm.Type;

import com.mpraski.jmonitor.EventOrder;
import com.mpraski.jmonitor.EventType;

public final class EventData {

	public EventData(EventType type, String tag, String monitor, EventOrder order) {
		this.type = type;
		this.tag = tag;
		this.monitor = monitor;
		this.order = order;
	}

	public EventData(
			EventType type,
			String tag,
			String monitor,
			EventOrder order,
			String name,
			String desc,
			String owner) {
		this.type = type;
		this.tag = tag;
		this.monitor = monitor;
		this.order = order;
		this.name = name;
		this.desc = desc;
		this.owner = owner;
	}

	public EventData(EventType type, String tag, String monitor, EventOrder order, String name) {
		this.type = type;
		this.tag = tag;
		this.monitor = monitor;
		this.order = order;
		this.name = name;
	}

	public EventData(
			EventType type,
			String tag,
			String monitor,
			EventOrder order,
			String name,
			int numArgs,
			String desc,
			Type retType) {
		this.type = type;
		this.tag = tag;
		this.monitor = monitor;
		this.order = order;
		this.name = name;
		this.numArgs = numArgs;
		this.desc = desc;
		this.retType = retType;
	}

	private final EventType type;
	private final EventOrder order;
	private final String tag;
	private final String monitor;
	private String name;
	private String desc;
	private String owner;
	private int numArgs;
	private Type retType;

	public Type getRetType() {
		return retType;
	}

	public EventType getType() {
		return type;
	}

	public String getTag() {
		return tag;
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("Type: ");
		builder.append(type);
		builder.append(", Tag: ");
		builder.append(tag);
		builder.append(", Monitor: ");
		builder.append(monitor);
		builder.append(", Name: ");
		builder.append(name);
		builder.append(", Desc: ");
		builder.append(desc);
		builder.append(", Owner: ");
		builder.append(owner);

		return builder.toString();
	}

	public EventOrder getOrder() {
		return order;
	}

	public int getNumArgs() {
		return numArgs;
	}
}
