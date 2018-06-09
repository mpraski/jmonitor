package com.mpraski.jmonitor.adapters;

import static com.mpraski.jmonitor.util.Constants.TYPE_BOOLEAN;
import static com.mpraski.jmonitor.util.Constants.TYPE_BYTE;
import static com.mpraski.jmonitor.util.Constants.TYPE_CHARACTER;
import static com.mpraski.jmonitor.util.Constants.TYPE_DOUBLE;
import static com.mpraski.jmonitor.util.Constants.TYPE_FLOAT;
import static com.mpraski.jmonitor.util.Constants.TYPE_INTEGER;
import static com.mpraski.jmonitor.util.Constants.TYPE_LONG;
import static com.mpraski.jmonitor.util.Constants.TYPE_SHORT;
import static com.mpraski.jmonitor.util.Constants.eventType;
import static com.mpraski.jmonitor.util.Constants.insteadMonitorClass;
import static com.mpraski.jmonitor.util.Constants.insteadMonitorClassFunc;
import static com.mpraski.jmonitor.util.Constants.insteadMonitorClassType;
import static com.mpraski.jmonitor.util.Constants.monitorClass;
import static com.mpraski.jmonitor.util.Constants.monitorClassFunc;
import static com.mpraski.jmonitor.util.Constants.monitorClassType;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import com.mpraski.jmonitor.event.EventType;
import com.mpraski.jmonitor.pattern.EventMonitor;
import com.mpraski.jmonitor.pattern.EventOrder;
import com.mpraski.jmonitor.pattern.EventPatternMatcher;
import com.mpraski.jmonitor.util.Pair;

public class MonitorMethodAdapter extends AnalyzerAdapter implements Opcodes {
	private final LocalVariablesSorter lvs;

	private final String thisName, thisDesc, thisOwner;
	private final Map<EventType, List<EventPatternMatcher>> mapped;
	private final Map<EventPatternMatcher, Boolean> matchesFrom;

	private final List<EventData> beforeMonitors, afterMonitors, insteadMonitors;

	private boolean shouldGenerateLocal = false;
	private boolean shouldGenerateDup = false;
	private int generatedLocal;
	private EventType currentType;

	protected MonitorMethodAdapter(String owner, int access, String name, String desc, LocalVariablesSorter lvs,
			List<EventPatternMatcher> matchers, Map<EventType, List<EventPatternMatcher>> mapped,
			Map<EventPatternMatcher, Boolean> matchesFrom, List<EventData> beforeMonitors,
			List<EventData> afterMonitors, List<EventData> insteadMonitors) {
		super(ASM5, owner, access, name, desc, lvs);

		this.thisOwner = owner.replace('/', '.');
		this.thisName = owner + '.' + name.replace('/', '.');
		this.thisDesc = desc;
		this.lvs = lvs;
		this.mapped = mapped;
		this.matchesFrom = matchesFrom;

		matchesFrom.clear();

		for (EventPatternMatcher m : matchers)
			matchesFrom.put(m, m.matchesFrom(thisName));

		this.beforeMonitors = beforeMonitors;
		this.afterMonitors = afterMonitors;
		this.insteadMonitors = insteadMonitors;

		System.out.println("Inside " + thisName + '|' + thisDesc);
	}

	@Override
	public void visitInsn(int opcode) {
		reset();

		switch (opcode) {
		case IRETURN:
		case LRETURN:
		case FRETURN:
		case DRETURN:
			String name = Type.getInternalName(getTop().getClass());
			tryMatch(EventType.RETURN, name, null, name, thisName);
			break;
		case ARETURN:
			String ret = (String) getTop();
			tryMatch(EventType.RETURN, ret, null, ret, thisName);
			break;
		case RETURN:
			tryMatch(EventType.RETURN);
			break;
		case MONITORENTER:
			String mon1 = (String) getTop();
			tryMatch(EventType.MONITOR_ENTER, mon1, null, mon1, thisName);
			break;
		case MONITOREXIT:
			String mon2 = (String) getTop();
			tryMatch(EventType.MONITOR_EXIT, mon2, null, mon2, thisName);
			break;
		case ATHROW:
			String ex = (String) getTop();
			tryMatch(EventType.THROW, ex, null, ex, thisName);
			break;
		}

		insertBeforeMonitors();
		super.visitInsn(opcode);
		insertAfterMonitors();
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		reset();

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
		reset();

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
		reset();

		// localNames.put(index, new LocalVariable(name, desc, signature));

		super.visitLocalVariable(name, desc, signature, start, end, index);
	}

	@Override
	public void visitTypeInsn(int opcode, String desc) {
		reset();

		switch (opcode) {
		case NEW:
			tryMatch(EventType.INSTANCE, desc, null, desc, thisName);
			break;
		case ANEWARRAY:
			tryMatch(EventType.INSTANCE_ARRAY, desc, null, desc, thisName);
			break;
		}

		insertBeforeMonitors();
		super.visitTypeInsn(opcode, desc);
		insertAfterMonitors();
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		reset();

		super.visitIincInsn(var, increment);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		reset();

		super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
	}

	@Override
	public void visitMethodInsn(int opc, String owner, String name, String desc, boolean isInterface) {
		reset();

		super.visitMethodInsn(opc, owner, name, desc, isInterface);
	}

	private Object getTop() {
		if (stack.isEmpty())
			throw new IllegalStateException("Stack is empty");

		return stack.get(stack.size() - 1);
	}

	private void tryMatch(EventType type) {
		for (EventPatternMatcher m : mapped.get(type))
			if (matchesFrom.get(m))
				addMonitors(m);

		currentType = type;
	}

	private void tryMatch(EventType type, String of, String name, String desc, String owner) {
		for (EventPatternMatcher m : mapped.get(type))
			if (matchesFrom.get(m) && m.matchesOf(of))
				addMonitors(m, name, desc, owner);

		currentType = type;
	}

	private void reset() {
		beforeMonitors.clear();
		afterMonitors.clear();
		insteadMonitors.clear();
		shouldGenerateLocal = shouldGenerateDup = false;
	}

	private void addMonitors(EventPatternMatcher e) {
		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder());
			switch (m.getOrder()) {
			case BEFORE:
				beforeMonitors.add(d);
				break;
			case AFTER:
				if (e.getType() == EventType.MONITOR_ENTER || e.getType() == EventType.MONITOR_EXIT)
					shouldGenerateDup = true;

				afterMonitors.add(d);
				break;
			case INSTEAD:
				insteadMonitors.add(d);
				break;
			}
		}

		if (e.getType() == EventType.FIELD_WRITE || e.getType() == EventType.FIELD_WRITE_STATIC)
			shouldGenerateLocal = true;
	}

	private void addMonitors(EventPatternMatcher e, String name, String desc, String owner) {
		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder(), name, desc, owner);
			switch (m.getOrder()) {
			case BEFORE:
				beforeMonitors.add(d);
				break;
			case AFTER:
				if (e.getType() == EventType.MONITOR_ENTER || e.getType() == EventType.MONITOR_EXIT)
					shouldGenerateDup = true;

				afterMonitors.add(d);
				break;
			case INSTEAD:
				insteadMonitors.add(d);
				break;
			}
		}

		if (e.getType() == EventType.FIELD_WRITE || e.getType() == EventType.FIELD_WRITE_STATIC)
			shouldGenerateLocal = true;
	}

	private void insertBeforeMonitors() {
		if (shouldGenerateLocal)
			generatedLocal = captureLocal();

		if (shouldGenerateDup)
			super.visitInsn(DUP);

		switch (currentType) {
		case FIELD_READ:
			for (EventData e : beforeMonitors)
				visitFieldRead(e);
			break;
		case FIELD_WRITE:
			for (EventData e : beforeMonitors)
				visitFieldWrite(e);
			break;
		case FIELD_READ_STATIC:
			for (EventData e : beforeMonitors)
				visitStaticFieldRead(e);
			break;
		case FIELD_WRITE_STATIC:
			for (EventData e : beforeMonitors)
				visitStaticFieldWrite(e);
			break;
		case METHOD_CALL:

			break;
		case RETURN:
		case THROW:
			for (EventData e : beforeMonitors)
				visitTopWithAutobox(e);
			break;
		case INSTANCE:
		case INSTANCE_ARRAY:
			for (EventData e : beforeMonitors)
				visitNewInstance(e);
			break;
		case MONITOR_ENTER:
		case MONITOR_EXIT:
			for (EventData e : beforeMonitors)
				visitTopWithAutobox(e);
			break;
		}

	}

	private void insertAfterMonitors() {
		switch (currentType) {
		case FIELD_READ:
		case FIELD_READ_STATIC:
			for (EventData e : afterMonitors)
				visitTopWithAutobox(e);
			break;
		case FIELD_WRITE:
			for (EventData e : afterMonitors)
				visitFieldWrite(e);
			break;
		case FIELD_WRITE_STATIC:
			for (EventData e : afterMonitors)
				visitStaticFieldWrite(e);
			break;
		case METHOD_CALL:

			break;
		case INSTANCE:

			break;
		case INSTANCE_ARRAY:

			break;
		case MONITOR_ENTER:
			for (EventData e : afterMonitors)
				visitTopWithAutobox(e);
			break;
		case MONITOR_EXIT:
			for (EventData e : afterMonitors)
				visitTopWithSwap(e);
			break;
		}
	}

	private int captureLocal() {
		super.visitInsn(DUP);

		String newTop = autobox();

		if (newTop == null)
			throw new IllegalStateException("Cannot create a local variable");

		int l = lvs.newLocal(Type.getObjectType(newTop));
		super.visitVarInsn(ASTORE, l);

		return l;
	}

	private void visitFieldRead(EventData e) {
		visitEventStart(e);
		super.visitVarInsn(ALOAD, 0);
		super.visitFieldInsn(GETFIELD, e.getOwner(), e.getName(), e.getDesc());
		autobox(e.getDesc());
		visitEventEnd(e);
	}

	private void visitFieldWrite(EventData e) {
		visitEventStart(e);
		super.visitVarInsn(ALOAD, 0);
		super.visitFieldInsn(GETFIELD, e.getOwner(), e.getName(), e.getDesc());
		autobox(e.getDesc());
		visitEventEndWithArgs(e, generatedLocal);
	}

	private void visitStaticFieldRead(EventData e) {
		visitEventStart(e);
		super.visitFieldInsn(GETSTATIC, e.getOwner(), e.getName(), e.getDesc());
		autobox(e.getDesc());
		visitEventEnd(e);
	}

	private void visitStaticFieldWrite(EventData e) {
		visitEventStart(e);
		super.visitFieldInsn(GETSTATIC, e.getOwner(), e.getName(), e.getDesc());
		autobox(e.getDesc());
		visitEventEndWithArgs(e, generatedLocal);
	}

	private void visitTopWithAutobox(EventData e) {
		visitEventStartWithDup(e);
		visitEventEnd(e);
	}

	private void visitTopWithSwap(EventData e) {
		visitEventStartWithSwap(e);
		visitEventEnd(e);
	}

	private void visitNewInstance(EventData e) {
		visitEventStart(e);
		super.visitInsn(ACONST_NULL);
		visitEventEndWithType(e, e.getDesc());
	}

	private void visitEventStart(EventData e) {
		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/common/Resolver", e.getMonitor(),
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassType : monitorClassType);
		super.visitTypeInsn(NEW, "com/mpraski/jmonitor/event/Event");
		super.visitInsn(DUP);

		if (e.getTag() == null) {
			super.visitInsn(ACONST_NULL);
		} else {
			super.visitLdcInsn(e.getTag());
		}

		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/event/EventType", eventType(e.getType()),
				"Lcom/mpraski/jmonitor/event/EventType;");
	}

	private void visitEventStartWithSwap(EventData e) {
		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/common/Resolver", e.getMonitor(),
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassType : monitorClassType);
		super.visitInsn(SWAP);
		super.visitTypeInsn(NEW, "com/mpraski/jmonitor/event/Event");
		super.visitInsn(DUP_X1);
		super.visitInsn(SWAP);

		if (e.getTag() == null) {
			super.visitInsn(ACONST_NULL);
		} else {
			super.visitLdcInsn(e.getTag());
		}

		super.visitInsn(SWAP);

		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/event/EventType", eventType(e.getType()),
				"Lcom/mpraski/jmonitor/event/EventType;");

		super.visitInsn(SWAP);
	}

	private void visitEventStartWithDup(EventData e) {
		autobox();

		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/common/Resolver", e.getMonitor(),
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassType : monitorClassType);
		super.visitInsn(SWAP);
		super.visitTypeInsn(NEW, "com/mpraski/jmonitor/event/Event");
		super.visitInsn(DUP_X1);
		super.visitInsn(SWAP);

		if (e.getTag() == null) {
			super.visitInsn(ACONST_NULL);
		} else {
			super.visitLdcInsn(e.getTag());
		}

		super.visitInsn(SWAP);

		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/event/EventType", eventType(e.getType()),
				"Lcom/mpraski/jmonitor/event/EventType;");

		super.visitInsn(SWAP);
	}

	private void visitEventEnd(EventData e) {
		super.visitInsn(ACONST_NULL);
		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;",
				false);
		super.visitMethodInsn(INVOKESPECIAL, "com/mpraski/jmonitor/event/Event", "<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/event/EventType;Ljava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		super.visitMethodInsn(INVOKEINTERFACE, e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClass : monitorClass,
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassFunc : monitorClassFunc,
				"(Lcom/mpraski/jmonitor/event/Event;)V", true);
	}

	private void visitEventEndWithArgs(EventData e, int localArg) {
		super.visitInsn(ICONST_1);
		super.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		super.visitInsn(DUP);
		super.visitInsn(ICONST_0);
		super.visitVarInsn(ALOAD, localArg);
		super.visitInsn(AASTORE);

		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;",
				false);
		super.visitMethodInsn(INVOKESPECIAL, "com/mpraski/jmonitor/event/Event", "<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/event/EventType;Ljava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		super.visitMethodInsn(INVOKEINTERFACE, e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClass : monitorClass,
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassFunc : monitorClassFunc,
				"(Lcom/mpraski/jmonitor/event/Event;)V", true);
	}

	private void visitEventEndWithArgs(EventData e, int[] localArgs) {
		pushInt(localArgs.length);
		super.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		super.visitInsn(DUP);
		for (int i = 0; i < localArgs.length; i++) {
			super.visitInsn(DUP);
			pushInt(i);
			super.visitVarInsn(ALOAD, localArgs[i]);
			super.visitInsn(AASTORE);
		}

		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;",
				false);
		super.visitMethodInsn(INVOKESPECIAL, "com/mpraski/jmonitor/event/Event", "<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/event/EventType;Ljava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		super.visitMethodInsn(INVOKEINTERFACE, e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClass : monitorClass,
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassFunc : monitorClassFunc,
				"(Lcom/mpraski/jmonitor/event/Event;)V", true);
	}

	private void visitEventEndWithType(EventData e, String desc) {
		super.visitInsn(ICONST_1);
		super.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		super.visitInsn(DUP);
		super.visitInsn(ICONST_0);
		super.visitLdcInsn(desc);
		super.visitInsn(AASTORE);

		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;",
				false);
		super.visitMethodInsn(INVOKESPECIAL, "com/mpraski/jmonitor/event/Event", "<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/event/EventType;Ljava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		super.visitMethodInsn(INVOKEINTERFACE, e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClass : monitorClass,
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassFunc : monitorClassFunc,
				"(Lcom/mpraski/jmonitor/event/Event;)V", true);
	}

	private void pushInt(int i) {
		if (i < 6) {
			switch (i) {
			case 0:
				super.visitInsn(ICONST_0);
				break;
			case 1:
				super.visitInsn(ICONST_1);
				break;
			case 2:
				super.visitInsn(ICONST_2);
				break;
			case 3:
				super.visitInsn(ICONST_3);
				break;
			case 4:
				super.visitInsn(ICONST_4);
				break;
			case 5:
				super.visitInsn(ICONST_5);
				break;
			}
		} else if (i > 5 && i < 128) {
			super.visitVarInsn(BIPUSH, i);
		} else if (i > 127 && i < 32768) {
			super.visitVarInsn(SIPUSH, i);
		} else {
			super.visitVarInsn(LDC, i);
		}
	}

	/*
	 * Attempts to produce a boxed value from descriptor of type on top of stack.
	 */
	private String autobox(String desc) {
		if (desc.length() > 1)
			return desc;

		Pair<String, String> type = null;

		switch (desc) {
		case "Z":
			type = TYPE_BOOLEAN;
			break;
		case "C":
			type = TYPE_CHARACTER;
			break;
		case "B":
			type = TYPE_BYTE;
			break;
		case "S":
			type = TYPE_SHORT;
			break;
		case "I":
			type = TYPE_INTEGER;
			break;
		case "F":
			type = TYPE_FLOAT;
			break;
		case "J":
			type = TYPE_LONG;
			break;
		case "D":
			type = TYPE_DOUBLE;
			break;
		}

		super.visitMethodInsn(INVOKESTATIC, type.getKey(), "valueOf", type.getValue(), false);

		return type.getKey();
	}

	/*
	 * Attempts to produce a boxed value from primitive on top of stack. Aware of
	 * long/double taking two slots.
	 */
	private String autobox() {
		if (stack.isEmpty())
			throw new IllegalStateException("Stack is empty");

		Object v2 = stack.get(stack.size() - 1);

		if (stack.size() > 1) {
			Object v1 = stack.get(stack.size() - 2);

			if (v1 instanceof Integer && v2 instanceof Integer && v2.equals(TOP)) {
				Integer desc = (Integer) v1;

				if (desc.equals(LONG)) {
					super.visitInsn(DUP2);
					super.visitMethodInsn(INVOKESTATIC, TYPE_LONG.getKey(), "valueOf", TYPE_LONG.getValue(), false);
					return TYPE_LONG.getKey();
				} else if (desc.equals(DOUBLE)) {
					super.visitInsn(DUP2);
					super.visitMethodInsn(INVOKESTATIC, TYPE_DOUBLE.getKey(), "valueOf", TYPE_DOUBLE.getValue(), false);
					return TYPE_DOUBLE.getKey();
				}

				throw new IllegalStateException("Cannot autobox TOP");
			}
		}

		return autobox(v2);
	}

	/*
	 * Attempts to produce a boxed value from primitive on top of stack. Only
	 * accepts single stack entry.
	 */
	private String autobox(Object value) {
		super.visitInsn(DUP);

		if (!(value instanceof Integer))
			if (value instanceof String)
				return (String) value;
			else
				throw new IllegalStateException("Cannot autobox a null value / label");

		Integer desc = (Integer) value;

		if (desc.equals(INTEGER)) {
			super.visitMethodInsn(INVOKESTATIC, TYPE_INTEGER.getKey(), "valueOf", TYPE_INTEGER.getValue(), false);
			return TYPE_INTEGER.getKey();
		} else if (desc.equals(FLOAT)) {
			super.visitMethodInsn(INVOKESTATIC, TYPE_FLOAT.getKey(), "valueOf", TYPE_FLOAT.getValue(), false);
			return TYPE_FLOAT.getKey();
		}

		throw new IllegalStateException("Should not reach this state");
	}
}
