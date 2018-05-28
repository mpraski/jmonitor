package com.mpraski.jmonitor.pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.mpraski.jmonitor.event.EventType;

public final class EventPatternMatcher {
	private final EventType type;
	private final List<Pattern> inPatternPositive, fromPatternPositive, ofPatternPositive;
	private final List<Pattern> inPatternNegative, fromPatternNegative, ofPatternNegative;
	private final Set<EventMonitor> monitors;

	public EventPatternMatcher(EventPatternTemporary t) {
		this.type = t.getType();
		this.monitors = t.getMonitors();
		this.inPatternPositive = getPatterns(t.getInPattern(), true);
		this.inPatternNegative = getPatterns(t.getInPattern(), false);
		this.fromPatternPositive = getPatterns(t.getFromPattern(), true);
		this.fromPatternNegative = getPatterns(t.getFromPattern(), false);
		this.ofPatternPositive = getPatterns(t.getOfPattern(), true);
		this.ofPatternNegative = getPatterns(t.getOfPattern(), false);
	}

	public boolean definesIn() {
		return !(inPatternPositive.isEmpty() && inPatternNegative.isEmpty());
	}

	public boolean definesFrom() {
		return !(fromPatternPositive.isEmpty() && fromPatternNegative.isEmpty());
	}

	public boolean definesOf() {
		return !(ofPatternPositive.isEmpty() && ofPatternNegative.isEmpty());
	}

	public boolean matchesIn(String s) {
		if (definesIn())
			return matches(inPatternPositive, inPatternNegative, s);

		return true;
	}

	public boolean matchesFrom(String s) {
		if (definesFrom())
			return matches(fromPatternPositive, fromPatternNegative, s);

		return true;
	}

	public boolean matchesOf(String s) {
		if (definesOf())
			return matches(ofPatternPositive, ofPatternNegative, s);

		return true;
	}

	private boolean matches(List<Pattern> listPos, List<Pattern> listNeg, String s) {
		return listPos.stream().allMatch(p -> p.matcher(s).matches())
				&& listNeg.stream().noneMatch(p -> p.matcher(s).matches());
	}

	private static List<Pattern> getPatterns(Map<String, Boolean> items, boolean sign) throws PatternSyntaxException {
		List<Pattern> l = new ArrayList<>();

		for (Map.Entry<String, Boolean> e : items.entrySet()) {
			if (sign == e.getValue()) {
				l.add(Pattern.compile(e.getKey()));
			}
		}

		return l;
	}

	@SuppressWarnings("unused")
	private static Pattern getNegated(Map<String, Boolean> items) throws PatternSyntaxException {
		StringBuilder sb = new StringBuilder();
		sb.append("(?!");

		for (Map.Entry<String, Boolean> e : items.entrySet()) {
			if (!e.getValue()) {
				sb.append("(");
				sb.append(e.getKey());
				sb.append(")|");
			}
		}

		if (!items.isEmpty()) {
			sb.setLength(sb.length() - 1);
		}

		sb.append(")");

		return Pattern.compile(sb.toString());
	}

	public EventType getType() {
		return type;
	}

	public Set<EventMonitor> getMonitors() {
		return monitors;
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
