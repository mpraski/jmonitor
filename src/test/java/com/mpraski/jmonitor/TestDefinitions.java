package com.mpraski.jmonitor;

import java.util.Arrays;
import java.util.List;

public class TestDefinitions implements EventPatternDefinitions {

	@Override
	public List<EventPattern> getEventPatterns() {
		EventPattern p = EventPattern.onFieldRead().of("lel").from("(.*)lol").doAfter("com.mpraski.dummy.DummyMonitor")
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
		EventPattern p9 = EventPattern.onInstanceCreated().from("(.*)writeObject")
				.doAfter("com.mpraski.dummy.DummyMonitor3");
		EventPattern p10 = EventPattern.onMonitorExit().from("(.*)writeMumbo").of("com.mpraski.dummy.Dummy")
				.doAfter("com.mpraski.dummy.DummyMonitor3");
		EventPattern p11 = EventPattern.onArrayCreated().from("(.*)lil").doAfter("com.mpraski.dummy.DummyMonitor4");
		EventPattern p12 = EventPattern.onInstanceCreated().from("(.*)lil").doBefore("com.mpraski.dummy.DummyMonitor4");
		EventPattern p13 = EventPattern.onInstanceCreated().from("(.*)lil").doAfter("com.mpraski.dummy.DummyMonitor4");
		EventPattern p14 = EventPattern.onMethodCall().from("(.*)doCall").doAfter("com.mpraski.dummy.DummyMonitor4");

		return Arrays.asList(p6, p8, p10, p12, p13, p14);
	}

}
