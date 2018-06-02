package com.mpraski.jmonitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.objectweb.asm.util.TraceClassVisitor;

import com.mpraski.jmonitor.adapters.MonitorClassAdapter;
import com.mpraski.jmonitor.common.MonitorClassLoader;
import com.mpraski.jmonitor.event.EventType;
import com.mpraski.jmonitor.pattern.EventPattern;
import com.mpraski.jmonitor.pattern.EventPatternCompiler;
import com.mpraski.jmonitor.pattern.EventPatternMatcher;

public class TestDriver {
	public static void main(String[] args) {
		new TestDriver().run();
	}

	public void run() {
		MonitorClassLoader cl = new MonitorClassLoader(getClass().getClassLoader(),
				"com.mpraski.jmonitor.TestDefinitions");

		Thread.currentThread().setContextClassLoader(cl);

		EventPattern p9 = EventPattern.onFieldRead().of("lel").from("lol").doBefore("monitor_1");

		final List<EventPattern> patterns = new ArrayList<>();
		patterns.add(p9);

		EventPatternCompiler c = new EventPatternCompiler();
		c.compile(patterns);

		List<EventPatternMatcher> matchers = c.getMatchers();
		Map<EventType, List<EventPatternMatcher>> mapped = matchers.stream()
				.collect(Collectors.groupingBy(EventPatternMatcher::getType));

		Path path = Paths.get("./bytecode_test/Example.class");
		byte[] data = null;

		try {
			data = Files.readAllBytes(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (data != null) {
			PrintWriter writer = new PrintWriter(System.out);

			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			TraceClassVisitor tcw = new TraceClassVisitor(cw, writer);
			ClassVisitor ca = new MonitorClassAdapter(tcw, matchers, mapped);

			ClassReader cr = new ClassReader(data);
			cr.accept(ca, ClassReader.EXPAND_FRAMES);

			Class testClass = cl.defineClass("bytecode_test.Example", cw.toByteArray());

			Method meth;
			try {
				meth = testClass.getMethod("main", String[].class);
				String[] params = null;
				meth.invoke(null, (Object) params);
			} catch (NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
}