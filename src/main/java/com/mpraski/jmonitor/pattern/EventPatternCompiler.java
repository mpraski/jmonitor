package com.mpraski.jmonitor.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.mpraski.jmonitor.event.EventType;

public final class EventPatternCompiler {
	private final Set<EventPatternTemporary> matchers = new HashSet<>();

	public List<EventPatternTemporary> compile(List<EventPattern> patterns) {
		matchers.clear();

		final List<EventPatternTemporary> temp = new ArrayList<>();

		for (EventPattern p : patterns) {
			_compile(temp, p);
			temp.clear();
		}

		return new ArrayList<>(matchers);
	}

	public List<EventPatternMatcher> compileMatchers(List<EventPattern> patterns) {
		matchers.clear();

		final List<EventPatternTemporary> temp = new ArrayList<>();

		for (EventPattern p : patterns) {
			_compile(temp, p);
			temp.clear();
		}

		return matchers.stream().filter(t -> t.getType() != EventType.NOT).map(EventPatternMatcher::new)
				.collect(Collectors.toList());
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

			if (e.hasMonitors()) {
				addMatchers(e);
			}

			temp.add(e);
		}
	}

	private void addMatchers(List<EventPatternTemporary> temp) {
		temp.stream().filter(m -> m != null && m.hasMonitors()).forEach(matchers::add);
	}

	private void addMatchers(EventPatternTemporary... temp) {
		matchers.addAll(Arrays.asList(temp));
	}

	private void addMonitor(List<EventPatternTemporary> temp, EventPattern p) {
		for (EventPatternTemporary m : temp) {
			m.addMonitor(p.getBeforeMonitor());
			m.addMonitor(p.getAfterMonitor());
			m.addMonitor(p.getInsteadMonitor());
		}
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
		temp.add(new EventPatternTemporary(andType, inPattern, fromPattern, ofPattern));

		addMonitor(temp, p);
		addMatchers(temp);
	}

	private void merge(List<EventPatternTemporary> result, final EventPattern p) {
		result.replaceAll(m -> new EventPatternTemporary(m.getType(),
				p.getIn() != null ? mapOfPatterns(p.getIn()) : m.getInPattern(),
				p.getFrom() != null ? mapOfPatterns(p.getFrom()) : m.getFromPattern(),
				p.getOf() != null ? mapOfPatterns(p.getOf()) : m.getOfPattern()));
	}

	private void negate(List<EventPatternTemporary> result) {
		result.replaceAll(t -> t.negate());
	}

	private EventType getAndType(List<EventPatternTemporary> temp) {
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

	private Map<String, Boolean> mapOfPatterns(String p) {
		Map<String, Boolean> m = new HashMap<>();
		m.put(p, true);

		return m;
	}
}
