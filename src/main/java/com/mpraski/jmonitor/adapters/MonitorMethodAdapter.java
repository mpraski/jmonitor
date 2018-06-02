package com.mpraski.jmonitor.adapters;

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
	private final Map<EventType, List<EventPatternMatcher>> mapped;
	private final Map<EventPatternMatcher, Boolean> matchesFrom;
	private final Map<Integer, LocalVariable> localNames;

	private final List<EventData> beforeMonitors, afterMonitors, insteadMonitors;

	protected MonitorMethodAdapter(String owner, int access, String name, String desc, MethodVisitor mv,
			List<EventPatternMatcher> matchers, Map<EventType, List<EventPatternMatcher>> mapped,
			List<EventData> beforeMonitors, List<EventData> afterMonitors, List<EventData> insteadMonitors) {
		super(ASM4, owner, access, name, desc, mv);

		this.thisName = name;
		this.thisDesc = desc;
		this.thisOwner = owner;
		this.mapped = mapped;
		this.matchesFrom = new HashMap<>(matchers.size());

		for (EventPatternMatcher m : matchers) {
			this.matchesFrom.put(m, m.matchesFrom(thisName));
		}

		this.beforeMonitors = beforeMonitors;
		this.afterMonitors = afterMonitors;
		this.insteadMonitors = insteadMonitors;
		this.localNames = new HashMap<>();

		System.out.println("Inside " + thisName + " : " + thisDesc + " : " + thisOwner);
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

			tryMatch(EventType.RETURN, ret, null, null, null);
			break;
		case MONITORENTER:
			String obj = getTop();

			tryMatch(EventType.MONITOR_ENTER, obj, null, null, null);
			break;
		case MONITOREXIT:
			String obj2 = getTop();

			tryMatch(EventType.MONITOR_EXIT, obj2, null, null, null);
			break;
		case ATHROW:
			String ex = getTop();

			tryMatch(EventType.THROW, ex, null, null, null);
			break;
		}

		insertBeforeMonitors();
		super.visitInsn(opcode);
		insertAfterMonitors();
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		resetMonitors();

		switch (opcode) {
		case GETFIELD:
			tryMatch(EventType.FIELD_READ, name, name, desc, owner);
			break;
		case PUTFIELD:
			tryMatch(EventType.FIELD_WRITE, name, name, desc, owner);
			break;
		case GETSTATIC:
			tryMatch(EventType.FIELD_READ_STATIC, name, name, desc, owner);
			break;
		case PUTSTATIC:
			tryMatch(EventType.FIELD_WRITE_STATIC, name, name, desc, owner);
			break;
		}

		insertBeforeMonitors();
		super.visitFieldInsn(opcode, owner, name, desc);
		insertAfterMonitors();
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		resetMonitors();

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

		super.visitVarInsn(opcode, var);
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		resetMonitors();

		localNames.put(index, new LocalVariable(name, desc, signature));

		super.visitLocalVariable(name, desc, signature, start, end, index);
	}

	@Override
	public void visitTypeInsn(int opcode, String desc) {
		resetMonitors();

		switch (opcode) {
		case NEW:
			tryMatch(EventType.INSTANCE, desc, null, null, null);
			break;
		case ANEWARRAY:
			tryMatch(EventType.INSTANCE_ARRAY, desc, null, null, null);
			break;
		}

		super.visitTypeInsn(opcode, desc);
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		resetMonitors();

		if (localNames.containsKey(var)) {

		}

		super.visitIincInsn(var, increment);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		resetMonitors();

		super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
	}

	@Override
	public void visitMethodInsn(int opc, String owner, String name, String desc, boolean isInterface) {
		resetMonitors();

		super.visitMethodInsn(opc, owner, name, desc, isInterface);
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

	private void tryMatch(EventType type, String of, String name, String desc, String owner) {
		for (EventPatternMatcher m : getMatchers(type)) {
			if (matchesFrom.get(m) && m.matchesOf(of))
				addMonitors(m, of, name, desc, owner);
		}
	}

	private void resetMonitors() {
		beforeMonitors.clear();
		afterMonitors.clear();
		insteadMonitors.clear();
	}

	private List<EventPatternMatcher> getMatchers(EventType type) {
		return mapped.getOrDefault(type, Collections.<EventPatternMatcher>emptyList());
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

	private void insertBeforeMonitors() {
		for (EventData e : beforeMonitors) {
			switch (e.getType()) {
			case FIELD_READ:
				generateFieldRead(e);
				break;
			case FIELD_WRITE:
				break;
			case FIELD_READ_STATIC:
				break;
			case FIELD_WRITE_STATIC:
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

	private void insertAfterMonitors() {
		for (EventData e : afterMonitors) {
			switch (e.getType()) {
			case FIELD_READ:
				generateFieldRead(e);
				break;
			case FIELD_WRITE:
				break;
			case FIELD_READ_STATIC:
				break;
			case FIELD_WRITE_STATIC:
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

	private void generateFieldRead(EventData e) {
		System.out.println("Called for eventdata: " + e);
		visitEventStart(e.getTag(), e.getType(), e.getSignature());
		super.visitVarInsn(ALOAD, 0);
		super.visitFieldInsn(GETFIELD, e.getOwner(), e.getName(), e.getDesc());
		autobox(e.getDesc());
		super.visitInsn(ACONST_NULL);
		visitEventEnd();
	}

	private void visitEventStart(String tag, EventType type, String signature) {
		super.visitTypeInsn(NEW, "com/mpraski/jmonitor/event/Event");
		super.visitInsn(DUP);
		super.visitLdcInsn(tag == null ? "" : tag);
		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/event/EventType", ToString.eventType(type),
				"Lcom/mpraski/jmonitor/event/EventType;");
		super.visitLdcInsn(signature == null ? "" : signature);
	}

	private void visitEventEnd() {
		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;",
				false);
		super.visitMethodInsn(INVOKESPECIAL, "com/mpraski/jmonitor/event/Event", "<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/event/EventType;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
	}

	private void visitNewArray() {
		super.visitTypeInsn(ANEWARRAY, "java/lang/Object");
	}

	private void visitAddToArray() {
		super.visitInsn(DUP);
		// mv.visitInsn(ICONST_0);
		super.visitVarInsn(ALOAD, 1);
		super.visitInsn(AASTORE);
	}

	private void autobox(String desc) {
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

		if (type != null)
			super.visitMethodInsn(INVOKESTATIC, "java/lang/" + type, "valueOf", "(I)Ljava/lang/" + type + ";", false);
	}
}
