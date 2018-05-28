package com.mpraski.jmonitor.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import com.mpraski.jmonitor.adapters.MonitorClassAdapter;
import com.mpraski.jmonitor.pattern.EventPattern;
import com.mpraski.jmonitor.pattern.EventPatternCompiler;
import com.mpraski.jmonitor.pattern.EventPatternMatcher;
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

		EventPattern p9 = EventPatterns.onReturn().from("say(.)*").doBefore("lol");

		final List<EventPattern> patterns = new ArrayList<>();
		// patterns.add(p);
		// patterns.add(p1);
		// patterns.add(p2);
		// patterns.add(p3);
		// patterns.add(p5);
		patterns.add(p9);

		EventPatternCompiler compiler = new EventPatternCompiler();
		// Set<EventPatternTemporary> matchers = compiler.compile(patterns);

		// matchers.forEach(System.out::println);

		Set<EventPatternMatcher> matchers = compiler.compile2(patterns);
		matchers.forEach(System.out::println);

		Path path = Paths.get("./bytecode_test/Example.class");
		byte[] data = null;

		try {
			data = Files.readAllBytes(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (data != null) {
			ClassWriter cw = new ClassWriter(0);
			ClassVisitor ca = new MonitorClassAdapter(Opcodes.ASM4, cw, matchers);
			ClassReader cr = new ClassReader(data);
			cr.accept(ca, 0);
		}
	}
}
