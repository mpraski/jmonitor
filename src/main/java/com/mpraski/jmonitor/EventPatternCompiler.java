package com.mpraski.jmonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class EventPatternCompiler {
	private final Set<EventPatternTemporary> matchers = new HashSet<>();
	private final Set<EventMonitor> monitors = new HashSet<>();

	public void compile(List<EventPattern> patterns) {
		final List<EventPatternTemporary> temp = new ArrayList<>();

		for (EventPattern p : patterns) {
			_compile(temp, p);
			temp.clear();
		}
	}

	public List<EventPatternMatcher> getMatchers() {
		return matchers.stream().map(EventPatternMatcher::new).collect(Collectors.toList());
	}

	public Set<EventMonitor> getMonitors() {
		return monitors;
	}

	public void clear() {
		matchers.clear();
		monitors.clear();
	}

	private void _compile(List<EventPatternTemporary> temp, EventPattern p) {
		switch (p.getType()) {
		case AND:
			compileAndPattern(temp, p);
			break;
		case OR:
			compileOrPattern(temp, p);
			break;
		case NOT:
			compileNotPattern(temp, p);
			break;
		default:
			EventPatternTemporary e = new EventPatternTemporary(p);

			if (e.hasMonitors())
				addMatchers(e);

			temp.add(e);
		}
	}

	private void addMatchers(List<EventPatternTemporary> temp) {
		temp.stream().filter(EventPatternTemporary::hasMonitors).forEach(matchers::add);
	}

	private void addMatchers(EventPatternTemporary temp) {
		matchers.add(temp);
		monitors.addAll(temp.getMonitors());
	}

	private void addMonitor(List<EventPatternTemporary> temp, EventPattern p) {
		for (EventPatternTemporary m : temp) {
			m.addMonitor(p.getBeforeMonitor());
			m.addMonitor(p.getAfterMonitor());
			m.addMonitor(p.getInsteadMonitor());
		}

		monitors.add(p.getBeforeMonitor());
		monitors.add(p.getAfterMonitor());
		monitors.add(p.getInsteadMonitor());
	}

	private void compileOrPattern(List<EventPatternTemporary> temp, EventPattern p) {
		_compile(temp, p.getOp2());
		_compile(temp, p.getOp1());

		merge(temp, p);

		addMonitor(temp, p);
		addMatchers(temp);
	}

	private void compileNotPattern(List<EventPatternTemporary> temp, EventPattern p) {
		_compile(temp, p.getOp1());

		merge(temp, p);
		negate(temp);
	}

	private void compileAndPattern(List<EventPatternTemporary> temp, EventPattern p) {
		_compile(temp, p.getOp2());
		_compile(temp, p.getOp1());

		merge(temp, p);

		EventType andType = getAndType(temp);

		if (andType == null) {
			temp.clear();
			return;
		}

		final Map<String, Boolean> ofPattern = new HashMap<>();
		final Map<String, Boolean> inPattern = new HashMap<>();
		final Map<String, Boolean> fromPattern = new HashMap<>();

		for (EventPatternTemporary t : temp) {
			for (Map.Entry<String, Boolean> pair : t.getOfPattern().entrySet()) {
				if (ofPattern.containsKey(pair.getKey()) && ofPattern.get(pair.getKey()) ^ pair.getValue()) {
					temp.clear();
					return;
				}

				ofPattern.put(pair.getKey(), pair.getValue());
			}

			for (Map.Entry<String, Boolean> pair : t.getInPattern().entrySet()) {
				if (inPattern.containsKey(pair.getKey()) && inPattern.get(pair.getKey()) ^ pair.getValue()) {
					temp.clear();
					return;
				}

				inPattern.put(pair.getKey(), pair.getValue());
			}

			for (Map.Entry<String, Boolean> pair : t.getFromPattern().entrySet()) {
				if (fromPattern.containsKey(pair.getKey()) && fromPattern.get(pair.getKey()) ^ pair.getValue()) {
					temp.clear();
					return;
				}

				fromPattern.put(pair.getKey(), pair.getValue());
			}
		}

		temp.clear();
		temp.add(new EventPatternTemporary(p.getTag(), andType, inPattern, fromPattern, ofPattern));

		addMonitor(temp, p);
		addMatchers(temp);
	}

	private static void merge(List<EventPatternTemporary> result, final EventPattern p) {
		result.replaceAll(m -> new EventPatternTemporary(m.getTag(), m.getType(),
				p.getIn() != null ? mapOf(p.getIn()) : m.getInPattern(),
				p.getFrom() != null ? mapOf(p.getFrom()) : m.getFromPattern(),
				p.getOf() != null ? mapOf(p.getOf()) : m.getOfPattern()));
	}

	private static void negate(List<EventPatternTemporary> result) {
		result.replaceAll(EventPatternTemporary::negate);
	}

	private static EventType getAndType(List<EventPatternTemporary> temp) {
		if (temp.isEmpty())
			return null;

		EventPatternTemporary first = temp.get(0);
		EventType type = first.getType();

		for (EventPatternTemporary m : temp) {
			if (type == EventType.ANY && m.getType() != EventType.ANY) {
				type = m.getType();
			} else if (m.getType() != EventType.ANY && type != m.getType()) {
				return null;
			}
		}

		return type;
	}

	private static Map<String, Boolean> mapOf(String p) {
		Map<String, Boolean> m = new HashMap<>();
		m.put(p, true);

		return m;
	}
}
