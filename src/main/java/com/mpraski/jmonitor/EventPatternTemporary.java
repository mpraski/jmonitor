package com.mpraski.jmonitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class EventPatternTemporary implements Cloneable {
	private final String tag;
	private final EventType type;

	private final Map<String, Boolean> ofPattern;
	private final Map<String, Boolean> inPattern;
	private final Map<String, Boolean> fromPattern;

	private final Set<EventMonitor> monitors;

	public EventPatternTemporary(EventPattern p) {
		this.tag = p.getTag();
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

	public EventPatternTemporary(String tag, EventType type, String inPattern, String fromPattern, String ofPattern) {
		this.tag = tag;
		this.type = type;

		this.ofPattern = new HashMap<>();
		this.ofPattern.put(ofPattern, true);

		this.inPattern = new HashMap<>();
		this.inPattern.put(inPattern, true);

		this.fromPattern = new HashMap<>();
		this.fromPattern.put(fromPattern, true);

		this.monitors = new HashSet<>();
	}

	public EventPatternTemporary(
			String tag,
			EventType type,
			Map<String, Boolean> inPattern,
			Map<String, Boolean> fromPattern,
			Map<String, Boolean> ofPattern) {
		this.tag = tag;
		this.type = type;

		this.ofPattern = ofPattern;

		this.inPattern = inPattern;
		this.fromPattern = fromPattern;

		this.monitors = new HashSet<>();
	}

	private EventPatternTemporary(EventPatternTemporary m, boolean negated) {
		this.tag = m.tag;
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

		return Objects.equals(type, c.getType()) && Objects.equals(tag, c.getTag())
				&& Objects.equals(monitors, c.getMonitors()) && Objects.equals(inPattern, c.getInPattern())
				&& Objects.equals(fromPattern, c.getFromPattern()) && Objects.equals(ofPattern, c.getOfPattern());
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(type);
		result = 31 * result + Objects.hashCode(tag);
		result = 31 * result + Objects.hashCode(monitors);
		result = 31 * result + Objects.hashCode(inPattern);
		result = 31 * result + Objects.hashCode(fromPattern);
		result = 31 * result + Objects.hashCode(ofPattern);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("Type: ");
		builder.append(type);
		builder.append(", inPattern: ");
		builder.append(inPattern);
		builder.append(", fromPattern: ");
		builder.append(fromPattern);
		builder.append(", ofPattern: ");
		builder.append(ofPattern);

		builder.append(System.lineSeparator());
		builder.append("-------------------------");
		builder.append(System.lineSeparator());

		monitors.forEach(m -> {
			builder.append(m);
			builder.append(System.lineSeparator());
		});

		return builder.toString();
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

	public String getTag() {
		return tag;
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
		Map<String, Boolean> m = new HashMap<>(in.size());
		in.forEach((k, v) -> m.put(k, !v));

		return m;
	}
}
