package com.mpraski.jmonitor;

import java.util.ArrayList;
import java.util.List;

import com.mpraski.jmonitor.pattern.EventPattern;
import com.mpraski.jmonitor.pattern.EventPatternDefinitions;

public class TestDefinitions implements EventPatternDefinitions {

	@Override
	public List<EventPattern> getEventPatterns() {
		EventPattern p = EventPattern.onFieldRead().of("lel").from("(.*)lol").doBefore("com.mpraski.dummy.DummyMonitor")
				.setTag("tagg");
		EventPattern p2 = EventPattern.onFieldRead().of("lilp").from("l(.*)")
				.doAfter("com.mpraski.dummy.DummyMonitor2");
		EventPattern p3 = EventPattern.onFieldWrite().of("lel").from("setLol")
				.doBefore("com.mpraski.dummy.DummyMonitor2").setTag("example");
		EventPattern p4 = EventPattern.onReturn().from("lol").of("(.*)String(.*)")
				.doBefore("com.mpraski.dummy.DummyMonitor3").setTag("example");
		EventPattern p55 = EventPattern.onReturn().from("(.*)readWop").doBefore("com.mpraski.dummy.DummyMonitor3");
		EventPattern p5 = EventPattern.onReturn().from("(.*)writeInt").doBefore("com.mpraski.dummy.DummyMonitor3");
		EventPattern p6 = EventPattern.onReturn().from("(.*)writeMumbo").doBefore("com.mpraski.dummy.DummyMonitor3");
		EventPattern p7 = EventPattern.onReturn().from("(.*)writeJumbo").doBefore("com.mpraski.dummy.DummyMonitor3");
		EventPattern p8 = EventPattern.onFieldRead().from("(.*)writeJumbo").doBefore("com.mpraski.dummy.DummyMonitor3");

		final List<EventPattern> patterns = new ArrayList<>();
		patterns.add(p);
		patterns.add(p8);

		return patterns;
	}

}
