package com.mpraski.jmonitor.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import com.mpraski.jmonitor.adapters.MonitorClassAdapter;
import com.mpraski.jmonitor.event.EventType;
import com.mpraski.jmonitor.pattern.EventPattern;
import com.mpraski.jmonitor.pattern.EventPatternCompiler;
import com.mpraski.jmonitor.pattern.EventPatternMatcher;

public class App {
	public static void main(String[] args) {
		// Dummy event patterns
		EventPattern p = EventPattern.onMethodCall().of("sdsd").doBefore("wolololo");

		EventPattern p3 = p.not().doInstead("sdsdsdll");

		EventPattern p1 = EventPattern.onFieldRead().of("sdsd").from("a").doAfter("mrart");

		EventPattern p12 = EventPattern.onFieldRead().of("sdsd").from("b").not();

		EventPattern p2 = p.or(p1).from("testing").doAfter("asd");

		EventPattern p5 = EventPattern.onArrayCreated().of("dsds")
				.or(EventPattern.onFieldRead().of("sds").or(EventPattern.onFieldWrite().of("sdsd"))).from("testing")
				.doAfter("asd");

		EventPattern p6 = p1.and(p12).doAfter("asdas");

		EventPattern p7 = EventPattern.onAnyEvent().of("sdsd").and(EventPattern.onAnyEvent().from("sdsd"))
				.and(EventPattern.onFieldRead().in("sdsdsd")).doAfter("wolo wolo").doBefore("why nut");

		EventPattern p8 = p1.excluding(EventPattern.onAnyEvent().in("c")).doBefore("asdasd");

		EventPattern p9 = EventPattern.onFieldRead().of("lel").from("lol").doBefore("lol");

		final List<EventPattern> patterns = new ArrayList<>();
		// patterns.add(p);
		// patterns.add(p1);
		// patterns.add(p2);
		// patterns.add(p3);
		// patterns.add(p5);
		patterns.add(p9);

		// Compile and print
		List<EventPatternMatcher> matchers = new EventPatternCompiler().compile(patterns);
		Map<EventType, List<EventPatternMatcher>> mapped = matchers.stream()
				.collect(Collectors.groupingBy(EventPatternMatcher::getType));

		matchers.forEach(System.out::println);

		// Test with dummy class file
		Path path = Paths.get("./bytecode_test/Example.class");
		byte[] data = null;

		try {
			data = Files.readAllBytes(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (data != null) {
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			ClassVisitor ca = new MonitorClassAdapter(Opcodes.ASM4, cw, matchers, mapped);
			ClassReader cr = new ClassReader(data);
			cr.accept(ca, ClassReader.EXPAND_FRAMES);

			Path file = Paths.get("./bytecode_test/Example_instrumented.class");
			try {
				Files.write(file, cw.toByteArray());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
