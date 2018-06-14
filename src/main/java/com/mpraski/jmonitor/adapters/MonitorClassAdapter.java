package com.mpraski.jmonitor.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

import com.mpraski.jmonitor.EventPatternMatcher;
import com.mpraski.jmonitor.EventType;
import com.mpraski.jmonitor.instead.InsteadActionGenerator;

public class MonitorClassAdapter extends ClassVisitor implements Opcodes {

	private final List<EventPatternMatcher> matchers;
	private final Map<EventType, List<EventPatternMatcher>> mapped;
	private final Map<EventPatternMatcher, Boolean> matchesFrom;
	private final List<EventData> eventsBefore, eventsAfter, eventsInstead;
	private final List<InsteadActionGenerator> actionGenerators;

	private String owner;
	private String source;
	private int accessorIndex;
	private int innerClassIndex = 1;

	public MonitorClassAdapter(ClassVisitor classVisitor, List<EventPatternMatcher> matchers,
			Map<EventType, List<EventPatternMatcher>> mapped) {
		super(ASM5, classVisitor);

		this.matchers = matchers;
		this.mapped = mapped;
		this.matchesFrom = new HashMap<>(matchers.size());
		this.eventsBefore = new ArrayList<>();
		this.eventsAfter = new ArrayList<>();
		this.eventsInstead = new ArrayList<>();
		this.actionGenerators = new ArrayList<>();
	}

	public String getNextInnerClass() {
		return String.valueOf(innerClassIndex++);
	}

	public String getNextAccessor() {
		return "access$" + (accessorIndex++);
	}

	public ClassVisitor getClassVisitor() {
		return cv;
	}

	public List<InsteadActionGenerator> getActionGenerators() {
		return actionGenerators;
	}

	public void addActionGenerator(InsteadActionGenerator a) {
		actionGenerators.add(a);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.owner = name;
		cv.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public void visitSource(java.lang.String source, java.lang.String debug) {
		this.source = source;
		cv.visitSource(source, debug);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

		if (mv != null) {
			LocalVariablesSorter sorter = new LocalVariablesSorter(access, desc, mv);
			mv = new MonitorMethodAdapter(owner, access, name, desc, source, this, sorter, matchers, mapped,
					matchesFrom, eventsBefore, eventsAfter, eventsInstead);
		}

		return mv;
	}

	@Override
	public void visitEnd() {
		for (InsteadActionGenerator g : actionGenerators) {
			cv.visitInnerClass(g.getName(), g.getOuterName(), g.getSimpleName(), 0);

			if (g.modifiesOuterClass())
				g.modifyOuterClass(this);
		}

		cv.visitEnd();
	}

}
