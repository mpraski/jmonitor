package com.mpraski.jmonitor.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AnalyzerAdapter;

import com.mpraski.jmonitor.event.EventType;
import com.mpraski.jmonitor.pattern.EventMonitor;
import com.mpraski.jmonitor.pattern.EventPatternMatcher;
import com.mpraski.jmonitor.util.LocalVariable;
import com.mpraski.jmonitor.util.ToString;

public class MonitorMethodAdapter extends AnalyzerAdapter implements Opcodes {

	private final String thisName, thisDesc, thisOwner;
	private final Map<EventType, List<EventPatternMatcher>> matchers;
	private final Map<EventPatternMatcher, Boolean> matchesFrom;
	private final Map<Integer, LocalVariable> localNames;

	private final List<EventMonitor> beforeMonitors, afterMonitors, insteadMonitors;

	protected MonitorMethodAdapter(int api, String owner, int access, String name, String desc, MethodVisitor mv,
			List<EventPatternMatcher> matchers) {
		super(api, owner, access, name, desc, mv);

		this.thisName = name;
		this.thisDesc = desc;
		this.thisOwner = owner;
		this.matchers = matchers.stream().collect(Collectors.groupingBy(EventPatternMatcher::getType));
		this.matchesFrom = matchers.stream()
				.collect(Collectors.toMap(Function.identity(), m -> m.matchesFrom(thisName)));
		this.beforeMonitors = new ArrayList<>();
		this.afterMonitors = new ArrayList<>();
		this.insteadMonitors = new ArrayList<>();
		this.localNames = new HashMap<>();
	}

	@Override
	public void visitInsn(int opcode) {
		resetMonitors();

		switch (opcode) {
		case IRETURN:
		case LRETURN:
		case FRETURN:
		case DRETURN:
		case RETURN:
			tryMatch(EventType.RETURN);
			break;
		case ARETURN:
			String ret = getTop();

			tryMatch(EventType.RETURN, ret);
			break;
		case MONITORENTER:
			String obj = getTop();

			tryMatch(EventType.MONITOR_ENTER, obj);
			break;
		case MONITOREXIT:
			String obj2 = getTop();

			tryMatch(EventType.MONITOR_EXIT, obj2);
			break;
		case ATHROW:
			String ex = getTop();

			tryMatch(EventType.THROW, ex);
			break;
		}

		insertMonitors(() -> mv.visitInsn(opcode));
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		resetMonitors();

		switch (opcode) {
		case GETFIELD:
			for (EventPatternMatcher m : matchers.get(EventType.FIELD_READ)) {
				if (matchesFrom.get(m) && (m.matchesOf(name) || m.matchesOf(desc))) {
					addMonitors(m.getMonitors());
				}
			}
			break;
		case PUTFIELD:
			break;
		case GETSTATIC:
			break;
		case PUTSTATIC:
			break;
		}

		mv.visitFieldInsn(opcode, owner, name, desc);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {

		switch (opcode) {
		case ILOAD:
		case LLOAD:
		case FLOAD:
		case DLOAD:
		case ALOAD:
			break;
		case ISTORE:
		case LSTORE:
		case FSTORE:
		case DSTORE:
		case ASTORE:
			break;
		}

		mv.visitVarInsn(opcode, var);
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		localNames.put(index, new LocalVariable(name, desc, signature));

		mv.visitLocalVariable(name, desc, signature, start, end, index);
	}

	@Override
	public void visitTypeInsn(int opcode, String desc) {
		switch (opcode) {
		case NEW:
			break;
		case ANEWARRAY:
			break;
		}

		mv.visitTypeInsn(opcode, desc);
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		if (localNames != null) {

		}

		mv.visitIincInsn(var, increment);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
	}

	@Override
	public void visitMethodInsn(int opc, String owner, String name, String desc, boolean isInterface) {
		mv.visitMethodInsn(opc, owner, name, desc, isInterface);
	}

	@SuppressWarnings("unchecked")
	private <T> T getTop() {
		if (stack.isEmpty())
			throw new IllegalStateException("Stack is empty");

		return (T) stack.get(stack.size() - 1);
	}

	private void tryMatch(EventType type) {
		for (EventPatternMatcher m : matchers.get(type)) {
			if (matchesFrom.get(m)) {
				addMonitors(m.getMonitors());
			}
		}
	}

	private void tryMatch(EventType type, String of) {
		for (EventPatternMatcher m : matchers.get(type)) {
			if (matchesFrom.get(m) && m.matchesOf(of)) {
				addMonitors(m.getMonitors());
			}
		}
	}

	private void resetMonitors() {
		beforeMonitors.clear();
		afterMonitors.clear();
		insteadMonitors.clear();
	}

	private void addMonitors(Set<EventMonitor> ms) {
		System.out.println("Got match!");
		for (EventMonitor m : ms) {
			switch (m.getOrder()) {
			case BEFORE:
				beforeMonitors.add(m);
				break;
			case AFTER:
				afterMonitors.add(m);
				break;
			case INSTEAD:
				insteadMonitors.add(m);
				break;
			}
		}
	}

	private void insertMonitors(Runnable action) {
		for (EventMonitor m : beforeMonitors) {
			// Insert stub
		}

		if (insteadMonitors.isEmpty()) {
			action.run();
		} else {
			for (EventMonitor m : insteadMonitors) {
				// Insert stubs
			}
		}

		for (EventMonitor m : afterMonitors) {
			// Insert stubs
		}
	}

	private static void generateEventDefinition(MethodVisitor mv, String tag, EventType type, String signature,
			Object target, Object[] arguments) {
		mv.visitTypeInsn(NEW, "com/mpraski/jmonitor/event/Event");
		mv.visitInsn(DUP);
		mv.visitLdcInsn(tag);
		mv.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/event/EventType", ToString.eventType(type),
				"Lcom/mpraski/jmonitor/event/EventType;");
		mv.visitLdcInsn(signature);
		mv.visitInsn(ACONST_NULL);
		mv.visitInsn(ACONST_NULL);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;",
				false);
		mv.visitMethodInsn(INVOKESPECIAL, "com/mpraski/jmonitor/event/Event", "<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/event/EventType;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		mv.visitVarInsn(ASTORE, 1);
		mv.visitInsn(RETURN);
		mv.visitMaxs(8, 2);
		mv.visitEnd();
	}

	public static void beforeRead(MethodVisitor mv) {

	}

	private static void autobox(MethodVisitor mv, String desc) {
		if (desc.length() > 1)
			return;

		String type = null;

		switch (desc) {
		case "Z":
			type = "Boolean";
			break;
		case "C":
			type = "Character";
			break;
		case "B":
			type = "Byte";
			break;
		case "S":
			type = "Short";
			break;
		case "I":
			type = "Integer";
			break;
		case "F":
			type = "Float";
			break;
		case "J":
			type = "Long";
			break;
		case "D":
			type = "Double";
			break;
		}

		if (type != null) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/" + type, "valueOf", "(I)Ljava/lang/" + type + ";", false);
		}
	}
}
