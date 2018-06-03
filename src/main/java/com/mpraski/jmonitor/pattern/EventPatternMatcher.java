package com.mpraski.jmonitor.pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.mpraski.jmonitor.event.EventType;

public final class EventPatternMatcher {
	private final String tag;
	private final EventType type;
	private final Set<EventMonitor> monitors;

	private final List<Pattern> inPatternPositive, fromPatternPositive, ofPatternPositive;
	private final Pattern inPatternNegative, fromPatternNegative, ofPatternNegative;

	private final int hash;

	public EventPatternMatcher(EventPatternTemporary t) {
		this.tag = t.getTag();
		this.type = t.getType();
		this.monitors = t.getMonitors();

		this.inPatternPositive = new ArrayList<>();
		this.fromPatternPositive = new ArrayList<>();
		this.ofPatternPositive = new ArrayList<>();

		final StringBuilder sb = new StringBuilder();

		for (Map.Entry<String, Boolean> e : t.getInPattern().entrySet()) {
			if (e.getValue()) {
				inPatternPositive.add(Pattern.compile(e.getKey()));
			} else {
				sb.append("(");
				sb.append(e.getKey());
				sb.append(")|");
			}
		}

		if (sb.length() == 0) {
			this.inPatternNegative = null;
		} else {
			sb.setLength(sb.length() - 1);
			this.inPatternNegative = Pattern.compile(sb.toString());
		}

		sb.setLength(0);

		for (Map.Entry<String, Boolean> e : t.getFromPattern().entrySet()) {
			if (e.getValue()) {
				fromPatternPositive.add(Pattern.compile(e.getKey()));
			} else {
				sb.append("(");
				sb.append(e.getKey());
				sb.append(")|");
			}
		}

		if (sb.length() == 0) {
			this.fromPatternNegative = null;
		} else {
			sb.setLength(sb.length() - 1);
			this.fromPatternNegative = Pattern.compile(sb.toString());
		}

		sb.setLength(0);

		for (Map.Entry<String, Boolean> e : t.getOfPattern().entrySet()) {
			if (e.getValue()) {
				ofPatternPositive.add(Pattern.compile(e.getKey()));
			} else {
				sb.append("(");
				sb.append(e.getKey());
				sb.append(")|");
			}
		}

		if (sb.length() == 0) {
			this.ofPatternNegative = null;
		} else {
			sb.setLength(sb.length() - 1);
			this.ofPatternNegative = Pattern.compile(sb.toString());
		}

		sb.setLength(0);

		this.hash = t.hashCode();
	}

	public boolean matchesIn(String s) {
		return (inPatternNegative == null ? true : inPatternNegative.matcher(s).matches())
				&& (inPatternPositive.isEmpty() ? true
						: inPatternPositive.stream().allMatch(p -> p.matcher(s).matches()));
	}

	public boolean matchesFrom(String s) {
		return (fromPatternNegative == null ? true : fromPatternNegative.matcher(s).matches())
				&& (fromPatternPositive.isEmpty() ? true
						: fromPatternPositive.stream().allMatch(p -> p.matcher(s).matches()));
	}

	public boolean matchesOf(String s) {
		return (ofPatternNegative == null ? true : ofPatternNegative.matcher(s).matches())
				&& (ofPatternPositive.isEmpty() ? true
						: ofPatternPositive.stream().allMatch(p -> p.matcher(s).matches()));
	}

	public String getTag() {
		return tag;
	}

	public EventType getType() {
		return type;
	}

	public Set<EventMonitor> getMonitors() {
		return monitors;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (!(o instanceof EventPatternMatcher)) {
			return false;
		}

		EventPatternMatcher c = (EventPatternMatcher) o;

		return Objects.equals(type, c.getType()) && Objects.equals(inPatternPositive, c.inPatternPositive)
				&& Objects.equals(fromPatternPositive, c.fromPatternPositive)
				&& Objects.equals(ofPatternPositive, c.ofPatternPositive)
				&& Objects.equals(inPatternNegative, c.inPatternNegative)
				&& Objects.equals(fromPatternNegative, c.fromPatternNegative)
				&& Objects.equals(ofPatternNegative, c.ofPatternNegative);
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();

		builder.append("Type: '" + this.type + "'");

		builder.append("\n-------------------------\n");

		monitors.forEach(m -> {
			builder.append(m);
			builder.append("\n");
		});

		return builder.toString();
	}
}
