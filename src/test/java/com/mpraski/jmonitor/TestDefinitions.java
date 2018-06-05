package com.mpraski.jmonitor;

import java.util.ArrayList;
import java.util.List;

import com.mpraski.jmonitor.pattern.EventPattern;
import com.mpraski.jmonitor.pattern.EventPatternDefinitions;

public class TestDefinitions implements EventPatternDefinitions {

	@Override
	public List<EventPattern> getEventPatterns() {
		EventPattern p = EventPattern.onFieldRead().of("lel").from("lol").doBefore("com.mpraski.dummy.DummyMonitor");
		// EventPattern p2 = EventPattern.onFieldRead().of("lilp").from("l(.*)")
		// .doAfter("com.mpraski.dummy.DummyMonitor2");
		EventPattern p3 = EventPattern.onFieldWrite().of("lel").from("setLol")
				.doBefore("com.mpraski.dummy.DummyMonitor2");

		final List<EventPattern> patterns = new ArrayList<>();
		patterns.add(p);
		patterns.add(p3);

		return patterns;
	}

}
