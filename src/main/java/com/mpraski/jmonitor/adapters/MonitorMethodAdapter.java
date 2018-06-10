package com.mpraski.jmonitor.adapters;

import static com.mpraski.jmonitor.util.Constants.FLOAT_INSNS;
import static com.mpraski.jmonitor.util.Constants.INTEGER_INSNS;
import static com.mpraski.jmonitor.util.Constants.OBJECT_ARRAY_TYPE;
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

	private final String thisName, thisDesc;
	private final Map<EventType, List<EventPatternMatcher>> mapped;
	private final Map<EventPatternMatcher, Boolean> matchesFrom;

	private final List<EventData> beforeMonitors, afterMonitors, insteadMonitors;

	private boolean shouldGenerateLocal = false;
	private boolean shouldGenerateDup = false;
	private int generatedLocal;
	private EventType currentType;
	private Type[] currentTypes;

	protected MonitorMethodAdapter(String owner, int access, String name, String desc, LocalVariablesSorter lvs,
			List<EventPatternMatcher> matchers, Map<EventType, List<EventPatternMatcher>> mapped,
			Map<EventPatternMatcher, Boolean> matchesFrom, List<EventData> beforeMonitors,
			List<EventData> afterMonitors, List<EventData> insteadMonitors) {
		super(ASM5, owner, access, name, desc, lvs);

		this.thisName = owner.replace('/', '.') + '.' + name.replace('/', '.');
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

		System.out.println("Inside " + thisName + " | " + thisDesc);
	}

	@Override
	public void visitInsn(int opcode) {
		reset();

		switch (opcode) {
		case IRETURN:
		case LRETURN:
		case FRETURN:
		case DRETURN:
			tryMatch(EventType.RETURN, Type.getInternalName(getTop().getClass()));
			break;
		case ARETURN:
			tryMatch(EventType.RETURN, (String) getTop());
			break;
		case RETURN:
			tryMatch(EventType.RETURN);
			break;
		case MONITORENTER:
			tryMatch(EventType.MONITOR_ENTER, (String) getTop());
			break;
		case MONITOREXIT:
			tryMatch(EventType.MONITOR_EXIT, (String) getTop());
			break;
		case ATHROW:
			tryMatch(EventType.THROW, (String) getTop());
			break;
		}

		insertBeforeMonitors();
		super.visitInsn(opcode);
		insertAfterMonitors();
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		reset();

		String fieldName = owner.replace('/', '.') + '.' + name;

		switch (opcode) {
		case GETFIELD:
			tryMatch(EventType.FIELD_READ, fieldName, name, desc, owner);
			break;
		case PUTFIELD:
			tryMatch(EventType.FIELD_WRITE, fieldName, name, desc, owner);
			break;
		case GETSTATIC:
			tryMatch(EventType.FIELD_READ_STATIC, fieldName, name, desc, owner);
			break;
		case PUTSTATIC:
			tryMatch(EventType.FIELD_WRITE_STATIC, fieldName, name, desc, owner);
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
	public void visitTypeInsn(int opcode, String desc) {
		reset();

		switch (opcode) {
		case NEW:
			tryMatch(EventType.INSTANCE, EventOrder.BEFORE, desc);
			break;
		case ANEWARRAY:
			tryMatch(EventType.INSTANCE_ARRAY, desc);
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
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
		reset();

		String methodOwner = owner.replace('/', '.');
		String methodName = methodOwner + '.' + name;
		currentTypes = Type.getMethodType(desc).getArgumentTypes();

		switch (opcode) {
		case INVOKESPECIAL:
			if (name.equals("<init>"))
				tryMatch(EventType.INSTANCE, EventOrder.AFTER, methodOwner);
			else
				tryMatch(EventType.METHOD_CALL, methodName, currentTypes.length);
			break;
		case INVOKESTATIC:
		case INVOKEVIRTUAL:
		case INVOKEINTERFACE:
			tryMatch(EventType.METHOD_CALL, methodName, currentTypes.length);
			break;
		}

		insertBeforeMonitors();
		super.visitMethodInsn(opcode, owner, name, desc, isInterface);
		insertAfterMonitors();
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

	private void tryMatch(EventType type, String of) {
		for (EventPatternMatcher m : mapped.get(type))
			if (matchesFrom.get(m))
				addMonitors(m, of);

		currentType = type;
	}

	private void tryMatch(EventType type, String of, String name, String desc, String owner) {
		for (EventPatternMatcher m : mapped.get(type))
			if (matchesFrom.get(m) && m.matchesOf(of))
				addMonitors(m, name, desc, owner);

		currentType = type;
	}

	private void tryMatch(EventType type, EventOrder order, String of) {
		for (EventPatternMatcher m : mapped.get(type))
			if (matchesFrom.get(m) && m.matchesOf(of))
				addMonitors(m, order, of);

		currentType = type;
	}

	private void tryMatch(EventType type, String of, int numArgs) {
		for (EventPatternMatcher m : mapped.get(type))
			if (matchesFrom.get(m) && m.matchesOf(of))
				addMonitors(m, of, numArgs);

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
				afterMonitors.add(d);
				break;
			case INSTEAD:
				insteadMonitors.add(d);
				break;
			}
		}
	}

	private void addMonitors(EventPatternMatcher e, String of) {
		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder(), of);
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
	}

	private void addMonitors(EventPatternMatcher e, String name, String desc, String owner) {
		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder(), name, desc, owner);
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

		if (e.getType() == EventType.FIELD_WRITE || e.getType() == EventType.FIELD_WRITE_STATIC)
			shouldGenerateLocal = true;
	}

	private void addMonitors(EventPatternMatcher e, EventOrder order, String of) {
		for (EventMonitor m : e.getMonitors()) {
			if (m.getOrder() != order)
				continue;

			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder(), of);
			switch (order) {
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

	private void addMonitors(EventPatternMatcher e, String of, int numArgs) {
		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder(), of, numArgs);
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
			for (EventData e : beforeMonitors)
				vititMethodCall(e);
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
		case INSTANCE_ARRAY:
			for (EventData e : afterMonitors)
				visitNewInstanceAfter(e);
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
		visitEventEndWithArg(e, generatedLocal);
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
		visitEventEndWithArg(e, generatedLocal);
	}

	private void visitTopWithAutobox(EventData e) {
		autobox();
		visitEventStartWithSwap(e);
		visitEventEnd(e);
	}

	private void visitTopWithSwap(EventData e) {
		visitEventStartWithSwap(e);
		visitEventEnd(e);
	}

	private void visitNewInstanceAfter(EventData e) {
		super.visitInsn(DUP);
		visitEventStartWithSwap(e);
		visitEventEnd(e);
	}

	private void visitNewInstance(EventData e) {
		visitEventStart(e);
		super.visitInsn(ACONST_NULL);
		visitEventEndWithType(e, e.getName());
	}

	// TO-DO: Add support for two-word values (long and double)
	private void vititMethodCall(EventData e) {
		LocalVariable[] localsIndices = new LocalVariable[e.getNumArgs()];

		pushInt(e.getNumArgs());
		super.visitTypeInsn(ANEWARRAY, "java/lang/Object");

		for (int i = 0; i < e.getNumArgs(); i++) {
			super.visitInsn(DUP_X1);
			super.visitInsn(SWAP);

			Object top = getTop();

			if (top instanceof Integer) {
				super.visitInsn(DUP);

				Pair<Integer, Integer> insns = getPrimitiveInsns((Integer) top);

				int l = lvs.newLocal(getPrimitiveType((Integer) top));
				super.visitVarInsn(insns.getKey(), l);

				localsIndices[i] = new LocalVariable(l, insns.getValue());

				autoboxWithoutDup();
			}

			System.out.println(stack);

			pushInt(i);
			super.visitInsn(SWAP);
			super.visitInsn(AASTORE);

			System.out.println(stack);
		}

		int arrLocal = lvs.newLocal(OBJECT_ARRAY_TYPE);
		super.visitVarInsn(ASTORE, arrLocal);

		visitEventStart(e);
		super.visitLdcInsn(e.getName());
		visitEventEndWithArgs(e, arrLocal);

		// Restore arguments on stack
		for (int i = 0, n = e.getNumArgs() - 1; i < e.getNumArgs(); i++, n--) {
			LocalVariable l = localsIndices[n];
			if (l != null) {
				super.visitVarInsn(l.getLoadInsn(), l.getIndex());
			} else {
				super.visitVarInsn(ALOAD, arrLocal);
				pushInt(n);
				super.visitInsn(AALOAD);
				super.visitTypeInsn(CHECKCAST, currentTypes[i].getInternalName());
			}
		}
	}

	private void visitEventStart(EventData e) {
		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/common/Resolver", e.getMonitor(),
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassType : monitorClassType);
		super.visitTypeInsn(NEW, "com/mpraski/jmonitor/event/Event");
		super.visitInsn(DUP);

		if (e.getTag() == null)
			super.visitInsn(ACONST_NULL);
		else
			super.visitLdcInsn(e.getTag());

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

		if (e.getTag() == null)
			super.visitInsn(ACONST_NULL);
		else
			super.visitLdcInsn(e.getTag());

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

	private void visitEventEndWithArg(EventData e, int localArg) {
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

	private void visitEventEndWithArgs(EventData e, int arrLocal) {
		super.visitVarInsn(ALOAD, arrLocal);

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

	private Type getPrimitiveType(Integer i) {
		if (i.equals(INTEGER))
			return Type.INT_TYPE;
		else if (i.equals(FLOAT))
			return Type.FLOAT_TYPE;

		throw new IllegalStateException("Should not reach this state");
	}

	private Pair<Integer, Integer> getPrimitiveInsns(Integer i) {
		if (i.equals(INTEGER))
			return INTEGER_INSNS;
		else if (i.equals(FLOAT))
			return FLOAT_INSNS;

		throw new IllegalStateException("Should not reach this state");
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
		Object v2 = getTop();

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

		super.visitInsn(DUP);

		return autobox(v2);
	}

	/*
	 * Attempts to produce a boxed value from primitive on top of stack. Accepts a
	 * single stack entry.
	 */
	private String autobox(Object value) {
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

	/*
	 * Attempts to produce a boxed value from primitive on top of stack. Aware of
	 * long/double taking two slots. No duplication
	 */
	private String autoboxWithoutDup() {
		Object v2 = getTop();

		if (stack.size() > 1) {
			Object v1 = stack.get(stack.size() - 2);

			if (v1 instanceof Integer && v2 instanceof Integer && v2.equals(TOP)) {
				Integer desc = (Integer) v1;

				if (desc.equals(LONG)) {
					super.visitMethodInsn(INVOKESTATIC, TYPE_LONG.getKey(), "valueOf", TYPE_LONG.getValue(), false);
					return TYPE_LONG.getKey();
				} else if (desc.equals(DOUBLE)) {
					super.visitMethodInsn(INVOKESTATIC, TYPE_DOUBLE.getKey(), "valueOf", TYPE_DOUBLE.getValue(), false);
					return TYPE_DOUBLE.getKey();
				}

				throw new IllegalStateException("Cannot autobox TOP");
			}
		}

		return autobox(v2);
	}
}
