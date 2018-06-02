package com.mpraski.jmonitor.pattern;

import java.util.Comparator;
import java.util.Objects;

public final class EventMonitor implements Comparable<EventMonitor> {
	private final String monitor;
	private final EventOrder order;

	public EventMonitor(String monitor, EventOrder order) {
		this.monitor = monitor;
		this.order = order;
	}

	private EventMonitor(EventMonitor e) {
		this.monitor = e.monitor;
		this.order = e.order;
	}

	public String getMonitor() {
		return monitor;
	}

	public EventOrder getOrder() {
		return order;
	}

	public String getFieldName() {
		return monitor.replaceAll("[.]", "");
	}

	@Override
	public String toString() {
		return "Monitor: '" + Objects.toString(monitor, "null") + "', order: '" + Objects.toString(order, "null") + "'";
	}

	@Override
	public EventMonitor clone() {
		return new EventMonitor(this);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (!(o instanceof EventMonitor)) {
			return false;
		}

		EventMonitor c = (EventMonitor) o;

		return Objects.equals(monitor, c.getMonitor()) && Objects.equals(order, c.getOrder());
	}

	@Override
	public int hashCode() {
		return Objects.hash(monitor, order);
	}

	@Override
	public int compareTo(EventMonitor o) {
		return Comparator.comparing(EventMonitor::getMonitor).thenComparing(EventMonitor::getOrder).compare(this, o);
	}
}
