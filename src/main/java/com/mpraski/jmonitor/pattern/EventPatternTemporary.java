package com.mpraski.jmonitor.pattern;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.mpraski.jmonitor.event.EventType;

public final class EventPatternTemporary {
	private final EventType type;

	private final Map<String, Boolean> ofPattern;
	private final Map<String, Boolean> inPattern;
	private final Map<String, Boolean> fromPattern;

	private final Set<EventMonitor> monitors;

	public EventPatternTemporary(EventPattern p) {
		this.type = p.getType();

		this.ofPattern = new HashMap<>();
		if (p.getOf() != null)
			this.ofPattern.put(p.getOf(), true);

		this.inPattern = new HashMap<>();
		if (p.getIn() != null)
			this.inPattern.put(p.getIn(), true);

		this.fromPattern = new HashMap<>();
		if (p.getFrom() != null)
			this.fromPattern.put(p.getFrom(), true);

		this.monitors = new HashSet<>();

		addMonitor(p.getBeforeMonitor());
		addMonitor(p.getAfterMonitor());
		addMonitor(p.getInsteadMonitor());
	}

	public EventPatternTemporary(EventType type, String inPattern, String fromPattern, String ofPattern) {
		this.type = type;

		this.ofPattern = new HashMap<>();
		this.ofPattern.put(ofPattern, true);

		this.inPattern = new HashMap<>();
		this.inPattern.put(inPattern, true);

		this.fromPattern = new HashMap<>();
		this.fromPattern.put(fromPattern, true);

		this.monitors = new HashSet<>();
	}

	public EventPatternTemporary(EventType type, Map<String, Boolean> inPattern, Map<String, Boolean> fromPattern,
			Map<String, Boolean> ofPattern) {
		this.type = type;

		this.ofPattern = ofPattern;

		this.inPattern = inPattern;
		this.fromPattern = fromPattern;

		this.monitors = new HashSet<>();
	}

	private EventPatternTemporary(EventPatternTemporary m, boolean negated) {
		this.type = m.type;

		if (negated) {
			this.ofPattern = copyNegated(m.ofPattern);

			this.inPattern = copyNegated(m.inPattern);
			this.fromPattern = copyNegated(m.fromPattern);
		} else {
			this.ofPattern = m.ofPattern;

			this.inPattern = m.inPattern;
			this.fromPattern = m.fromPattern;
		}

		this.monitors = m.monitors;
	}

	@Override
	public EventPatternTemporary clone() {
		return new EventPatternTemporary(this, false);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (!(o instanceof EventPatternTemporary)) {
			return false;
		}

		EventPatternTemporary c = (EventPatternTemporary) o;

		return Objects.equals(type, c.getType()) && Objects.equals(inPattern, c.getInPattern())
				&& Objects.equals(fromPattern, c.getFromPattern()) && Objects.equals(ofPattern, c.getOfPattern());
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();

		builder.append("Type: '" + this.type + "',\ninPattern: '" + Objects.toString(inPattern, "null")
				+ "',\nfromPattern: '" + Objects.toString(fromPattern, "null") + "',\nofPattern: '"
				+ Objects.toString(ofPattern, "null") + "'");

		builder.append("\n-------------------------\n");

		monitors.forEach(m -> {
			builder.append(m);
			builder.append("\n");
		});

		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, inPattern, fromPattern, ofPattern);
	}

	public Map<String, Boolean> getInPattern() {
		return inPattern;
	}

	public Map<String, Boolean> getFromPattern() {
		return fromPattern;
	}

	public Map<String, Boolean> getOfPattern() {
		return ofPattern;
	}

	public EventType getType() {
		return type;
	}

	public boolean hasMonitors() {
		return !monitors.isEmpty();
	}

	public void addMonitor(EventMonitor em) {
		if (em != null)
			monitors.add(em);
	}

	public Set<EventMonitor> getMonitors() {
		return monitors;
	}

	public EventPatternTemporary negate() {
		return new EventPatternTemporary(this, true);
	}

	private static Map<String, Boolean> copyNegated(Map<String, Boolean> in) {
		Map<String, Boolean> m = new HashMap<>();
		in.forEach((k, v) -> m.put(k, !v));

		return m;
	}
}
