package com.mpraski.jmonitor.adapters;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AnalyzerAdapter;

import com.mpraski.jmonitor.event.EventType;
import com.mpraski.jmonitor.pattern.EventMonitor;
import com.mpraski.jmonitor.pattern.EventOrder;
import com.mpraski.jmonitor.pattern.EventPatternMatcher;
import com.mpraski.jmonitor.util.ToString;

public class MonitorMethodAdapter extends AnalyzerAdapter implements Opcodes {

	private static final String monitorClass = "Lcom/mpraski/jmonitor/common/Monitor;";
	private static final String insteadMonitorClass = "Lcom/mpraski/jmonitor/common/InsteadMonitor;";
	private static final String monitorClassFunc = "onEvent";
	private static final String insteadMonitorClassFunc = "doInstead";

	private final String thisName, thisDesc, thisOwner;
	private final Map<EventType, List<EventPatternMatcher>> mapped;
	private final Map<EventPatternMatcher, Boolean> matchesFrom;

	private final List<EventData> beforeMonitors, afterMonitors, insteadMonitors;

	protected MonitorMethodAdapter(String owner, int access, String name, String desc, MethodVisitor mv,
			List<EventPatternMatcher> matchers, Map<EventType, List<EventPatternMatcher>> mapped,
			Map<EventPatternMatcher, Boolean> matchesFrom, List<EventData> beforeMonitors,
			List<EventData> afterMonitors, List<EventData> insteadMonitors) {
		super(ASM5, owner, access, name, desc, mv);

		this.thisName = name;
		this.thisDesc = desc;
		this.thisOwner = owner;
		this.mapped = mapped;
		this.matchesFrom = matchesFrom;

		matchesFrom.clear();
		for (EventPatternMatcher m : matchers) {
			matchesFrom.put(m, m.matchesFrom(thisName));
		}

		this.beforeMonitors = beforeMonitors;
		this.afterMonitors = afterMonitors;
		this.insteadMonitors = insteadMonitors;

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

		insertMonitors(beforeMonitors);
		super.visitInsn(opcode);
		insertMonitors(afterMonitors);
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

		insertMonitors(beforeMonitors);
		super.visitFieldInsn(opcode, owner, name, desc);
		insertMonitors(afterMonitors);
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

		// localNames.put(index, new LocalVariable(name, desc, signature));

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

		// if (localNames.containsKey(var)) {

		// }

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

	// Assuming the caller knows the type of object on top of stack
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
			EventData d = new EventData(e.getType(), e.getTag(), null, m.getFieldName(), m.getOrder());
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
			EventData d = new EventData(e.getType(), e.getTag(), signature, m.getFieldName(), m.getOrder(), name, desc,
					owner);
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

	private void insertMonitors(List<EventData> monitors) {
		for (EventData e : monitors) {
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
		visitEventStart(e);
		super.visitVarInsn(ALOAD, 0);
		super.visitFieldInsn(GETFIELD, e.getOwner(), e.getName(), e.getDesc());
		autobox(e.getDesc());
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);
	}

	private void visitEventStart(EventData e) {
		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/common/Resolver", e.getMonitor(),
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClass : monitorClass);
		super.visitTypeInsn(NEW, "com/mpraski/jmonitor/event/Event");
		super.visitInsn(DUP);
		super.visitLdcInsn(e.getTag() == null ? "" : e.getTag());
		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/event/EventType", ToString.eventType(e.getType()),
				"Lcom/mpraski/jmonitor/event/EventType;");
		super.visitLdcInsn(e.getSignature() == null ? "" : e.getSignature());
	}

	private void visitEventEnd(EventData e) {
		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;",
				false);
		super.visitMethodInsn(INVOKESPECIAL, "com/mpraski/jmonitor/event/Event", "<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/event/EventType;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		super.visitMethodInsn(INVOKEINTERFACE, "com/mpraski/jmonitor/common/Monitor",
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassFunc : monitorClassFunc,
				"(Lcom/mpraski/jmonitor/event/Event;)V", true);
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
