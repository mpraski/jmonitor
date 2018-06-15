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
		EventPattern p3 = EventPattern.onFieldWrite().of("(.*)lel").from("(.*)setLol")
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
		EventPattern p14 = EventPattern.onMethodCall().of("(.*)someMethod").doAfter("com.mpraski.dummy.DummyMonitor4");
		EventPattern p15 = EventPattern.onMethodCall().of("(.*)nothingToSay")
				.doBefore("com.mpraski.dummy.DummyMonitor4");
		EventPattern p16 = EventPattern.onReturn().from("(.*)writeInt").doInstead("com.mpraski.dummy.DummyMonitor5");
		EventPattern p17 = EventPattern.onMonitorEnter().from("(.*)writeMumbo").of("com.mpraski.dummy.Dummy")
				.doInstead("com.mpraski.dummy.DummyMonitor6");
		EventPattern p18 = EventPattern.onReturn().from("(.*)someMethod2").doInstead("com.mpraski.dummy.DummyMonitor7");
		EventPattern p19 = EventPattern.onFieldRead().from("(.*)writeJumbo")
				.doInstead("com.mpraski.dummy.DummyMonitor8");
		EventPattern p20 = EventPattern.onFieldWrite().from("(.*)setLol2").of("(.*)lel")
				.doInstead("com.mpraski.dummy.DummyMonitor9");

		return Arrays.asList(p3, p6, p8, p10, p12, p13, p14, p15, p16, p17, p18, p19, p20);
	}

}
