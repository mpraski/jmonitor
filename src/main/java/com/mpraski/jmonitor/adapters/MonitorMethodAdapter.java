package com.mpraski.jmonitor.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private final List<EventData> beforeMonitors, afterMonitors, insteadMonitors;

	protected MonitorMethodAdapter(int api, String owner, int access, String name, String desc, MethodVisitor mv,
			List<EventPatternMatcher> matchers) {
		super(api, owner, access, name, desc, mv);

		this.thisName = name;
		this.thisDesc = desc;
		this.thisOwner = owner;
		this.matchers = new HashMap<>();
		this.matchesFrom = new HashMap<>();

		for (EventPatternMatcher m : matchers) {
			this.matchers.putIfAbsent(m.getType(), new ArrayList<>());
			this.matchers.get(m.getType()).add(m);
			this.matchesFrom.put(m, m.matchesFrom(thisName));
		}

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
			tryMatch(EventType.FIELD_READ, name);
			break;
		case PUTFIELD:
			tryMatch(EventType.FIELD_WRITE, name);
			break;
		case GETSTATIC:
			tryMatch(EventType.FIELD_READ_STATIC, name);
			break;
		case PUTSTATIC:
			tryMatch(EventType.FIELD_WRITE_STATIC, name);
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
		for (EventPatternMatcher m : getMatchers(type)) {
			if (matchesFrom.get(m))
				addMonitors(m);
		}
	}

	private void tryMatch(EventType type, String of) {
		for (EventPatternMatcher m : getMatchers(type)) {
			if (matchesFrom.get(m) && m.matchesOf(of))
				addMonitors(m, of, null, null, null);
		}
	}

	private void resetMonitors() {
		beforeMonitors.clear();
		afterMonitors.clear();
		insteadMonitors.clear();
	}

	private List<EventPatternMatcher> getMatchers(EventType type) {
		return matchers.getOrDefault(type, Collections.<EventPatternMatcher>emptyList());
	}

	private void addMonitors(EventPatternMatcher e) {
		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), null, m.getMonitor());
			switch (m.getOrder()) {
			case BEFORE:
				beforeMonitors.add(d);
				break;
			case AFTER:
				afterMonitors.add(d);
				break;
			case INSTEAD:
				insteadMonitors.add(d);
				break;
			}
		}
	}

	private void addMonitors(EventPatternMatcher e, String signature, String name, String desc, String owner) {
		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), signature, m.getMonitor(), name, desc, owner);
			switch (m.getOrder()) {
			case BEFORE:
				beforeMonitors.add(d);
				break;
			case AFTER:
				afterMonitors.add(d);
				break;
			case INSTEAD:
				insteadMonitors.add(d);
				break;
			}
		}
	}

	private void insertMonitors(Runnable action) {
		for (EventData e : beforeMonitors) {
			switch (e.getType()) {
			case FIELD_READ:
				generateFieldRead(mv, e);
				break;
			case FIELD_WRITE:
				break;
			case METHOD_CALL:
				break;
			case RETURN:
				break;
			case THROW:
				break;
			case INSTANCE:
				break;
			case INSTANCE_ARRAY:
				break;
			case MONITOR_ENTER:
				break;
			case MONITOR_EXIT:
				break;
			}
		}

		if (insteadMonitors.isEmpty()) {
			action.run();
		} else {
			for (EventData e : insteadMonitors) {
				// Insert stubs
			}
		}

		for (EventData e : afterMonitors) {
			switch (e.getType()) {
			case FIELD_READ:
				generateFieldRead(mv, e);
				break;
			case FIELD_WRITE:
				break;
			case METHOD_CALL:
				break;
			case RETURN:
				break;
			case THROW:
				break;
			case INSTANCE:
				break;
			case INSTANCE_ARRAY:
				break;
			case MONITOR_ENTER:
				break;
			case MONITOR_EXIT:
				break;
			}
		}
	}

	private static void generateFieldRead(MethodVisitor mv, EventData e) {
		visitEventStart(mv, e.getTag(), e.getType(), e.getSignature());
		mv.visitFieldInsn(GETFIELD, e.getOwner(), e.getName(), e.getDesc());
		autobox(mv, e.getDesc());
		mv.visitInsn(ACONST_NULL);
		visitEventEnd(mv);
	}

	private static void visitEventStart(MethodVisitor mv, String tag, EventType type, String signature) {
		mv.visitTypeInsn(NEW, "com/mpraski/jmonitor/event/Event");
		mv.visitInsn(DUP);
		mv.visitLdcInsn(tag);
		mv.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/event/EventType", ToString.eventType(type),
				"Lcom/mpraski/jmonitor/event/EventType;");
		mv.visitLdcInsn(signature);
	}

	private static void visitEventEnd(MethodVisitor mv) {
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;",
				false);
		mv.visitMethodInsn(INVOKESPECIAL, "com/mpraski/jmonitor/event/Event", "<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/event/EventType;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
	}

	private static void visitNewArray(MethodVisitor mv) {
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
	}

	private static void visitAddToArray(MethodVisitor mv) {
		mv.visitInsn(DUP);
		// mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitInsn(AASTORE);
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
