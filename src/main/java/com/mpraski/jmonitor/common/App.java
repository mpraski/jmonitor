package com.mpraski.jmonitor.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mpraski.jmonitor.pattern.EventPattern;
import com.mpraski.jmonitor.pattern.EventPatternCompiler;
import com.mpraski.jmonitor.pattern.EventPatternTemporary;
import com.mpraski.jmonitor.pattern.EventPatterns;

public class App {
	public static void main(String[] args) {
		EventPattern p = EventPatterns.onMethodCall().of("sdsd").doBefore("wolololo");

		EventPattern p3 = p.not().doInstead("sdsdsdll");

		EventPattern p1 = EventPatterns.onFieldRead().of("sdsd").from("a").doAfter("mrart");

		EventPattern p12 = EventPatterns.onFieldRead().of("sdsd").from("b").not();

		EventPattern p2 = p.or(p1).from("testing").doAfter("asd");

		EventPattern p5 = EventPatterns.onArrayCreated().of("dsds")
				.or(EventPatterns.onFieldRead().of("sds").or(EventPatterns.onFieldWrite().of("sdsd"))).from("testing")
				.doAfter("asd");

		EventPattern p6 = p1.and(p12).doAfter("asdas");

		EventPattern p7 = EventPatterns.onAnyEvent().of("sdsd").and(EventPatterns.onAnyEvent().from("sdsd"))
				.and(EventPatterns.onFieldRead().in("sdsdsd")).doAfter("wolo wolo").doBefore("why nut");

		EventPattern p8 = p1.excluding(EventPatterns.onAnyEvent().in("c")).doBefore("asdasd");

		final List<EventPattern> patterns = new ArrayList<>();
		// patterns.add(p);
		// patterns.add(p1);
		// patterns.add(p2);
		// patterns.add(p3);
		// patterns.add(p5);
		patterns.add(p8);

		EventPatternCompiler compiler = new EventPatternCompiler();
		Set<EventPatternTemporary> matchers = compiler.compile(patterns);

		matchers.forEach(System.out::println);
	}
}
