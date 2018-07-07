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

public class ClassAdapter extends ClassVisitor implements Opcodes {

	private final List<EventPatternMatcher> matchers;
	private final List<EventData> eventsBefore, eventsAfter, eventsInstead;
	private final List<InsteadActionGenerator> actionGenerators;

	private final Map<EventType, List<EventPatternMatcher>> mapped;
	private final Map<EventPatternMatcher, Boolean> matchesFrom;

	private String owner;
	private String source;
	private int accessorIndex;
	private int innerClassIndex;

	public ClassAdapter(
			ClassVisitor classVisitor,
			List<EventPatternMatcher> matchers,
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

	public List<InsteadActionGenerator> getActionGenerators() {
		return actionGenerators;
	}

	protected String getNextInnerClass() {
		return String.valueOf(innerClassIndex++);
	}

	protected String getNextAccessor() {
		return "access$" + (accessorIndex++);
	}

	protected void addActionGenerator(InsteadActionGenerator a) {
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

			InstrumentationAdapter instrument = new InstrumentationAdapter(
					owner,
					access,
					name,
					desc,
					source,
					this,
					sorter);

			mv = new MethodAdapter(
					name,
					desc,
					source,
					owner,
					instrument,
					matchers,
					mapped,
					matchesFrom,
					eventsBefore,
					eventsAfter,
					eventsInstead);
		}

		return mv;
	}

	@Override
	public void visitEnd() {
		for (InsteadActionGenerator action : actionGenerators) {
			cv.visitInnerClass(action.getName(), action.getOuterName(), action.getSimpleName(), ACC_SUPER);

			if (action.modifiesOuterClass())
				action.modifyOuterClass(cv);
		}

		cv.visitEnd();
	}

}
