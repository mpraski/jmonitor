package com.mpraski.jmonitor.adapters;

import static com.mpraski.jmonitor.util.Constants.DOUBLE_INSNS;
import static com.mpraski.jmonitor.util.Constants.DOUBLE_TYPE;
import static com.mpraski.jmonitor.util.Constants.FLOAT_INSNS;
import static com.mpraski.jmonitor.util.Constants.FLOAT_TYPE;
import static com.mpraski.jmonitor.util.Constants.INTEGER_INSNS;
import static com.mpraski.jmonitor.util.Constants.INTEGER_TYPE;
import static com.mpraski.jmonitor.util.Constants.LONG_INSNS;
import static com.mpraski.jmonitor.util.Constants.LONG_TYPE;
import static com.mpraski.jmonitor.util.Constants.OBJECT_ARRAY_TYPE;
import static com.mpraski.jmonitor.util.Constants.REF_INSNS;
import static com.mpraski.jmonitor.util.Constants.TYPE_DOUBLE;
import static com.mpraski.jmonitor.util.Constants.TYPE_FLOAT;
import static com.mpraski.jmonitor.util.Constants.TYPE_INTEGER;
import static com.mpraski.jmonitor.util.Constants.TYPE_LONG;
import static com.mpraski.jmonitor.util.Constants.eventType;
import static com.mpraski.jmonitor.util.Constants.getPrimitiveClass;
import static com.mpraski.jmonitor.util.Constants.insteadMonitorClass;
import static com.mpraski.jmonitor.util.Constants.insteadMonitorClassFunc;
import static com.mpraski.jmonitor.util.Constants.insteadMonitorClassType;
import static com.mpraski.jmonitor.util.Constants.monitorClass;
import static com.mpraski.jmonitor.util.Constants.monitorClassFunc;
import static com.mpraski.jmonitor.util.Constants.monitorClassType;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import com.mpraski.jmonitor.EventMonitor;
import com.mpraski.jmonitor.EventOrder;
import com.mpraski.jmonitor.EventPatternMatcher;
import com.mpraski.jmonitor.EventType;
import com.mpraski.jmonitor.util.Pair;

/*
 * This MethodVisitor is responsible for injecting appropriate bytecode instructions to perform instrumentation.
 */
public class MonitorMethodAdapter extends AnalyzerAdapter implements Opcodes {

	/*
	 * Used to add local variables in cases where certain values (e.g. method
	 * arguments, event args array) need to be preserved.
	 */
	private final LocalVariablesSorter sorter;

	private final String thisName, thisDesc;
	private final Map<EventType, List<EventPatternMatcher>> mapped;
	private final Map<EventPatternMatcher, Boolean> matchesFrom;

	private final List<EventData> beforeMonitors, afterMonitors, insteadMonitors;

	/*
	 * Temporaries used for capturing some values prior to the target instruction so
	 * that an event can be generated after this instruction is executed.
	 */
	private boolean shouldGenerateLocal;
	private boolean shouldGenerateDup;
	private boolean shouldGenerateArgsArray;
	private int generatedLocal;
	private int generatedArgsArray;
	private int currentNumArgs;
	private EventType currentType;

	protected MonitorMethodAdapter(String owner, int access, String name, String desc, LocalVariablesSorter lvs,
			List<EventPatternMatcher> matchers, Map<EventType, List<EventPatternMatcher>> mapped,
			Map<EventPatternMatcher, Boolean> matchesFrom, List<EventData> beforeMonitors,
			List<EventData> afterMonitors, List<EventData> insteadMonitors) {
		super(ASM5, owner, access, name, desc, lvs);

		this.thisName = toDots(owner) + '.' + name;
		this.thisDesc = desc;
		this.sorter = lvs;
		this.mapped = mapped;
		this.matchesFrom = matchesFrom;

		matchesFrom.clear();

		/*
		 * Compute all the 'from' matches only once, they won't change throughout the
		 * method execution.
		 */
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
			tryMatch(EventType.RETURN, getTopType().getInternalName());
			break;
		case ARETURN:
			tryMatch(EventType.RETURN, toDots((String) getTop()));
			break;
		case RETURN:
			tryMatch(EventType.RETURN);
			break;
		case MONITORENTER:
			tryMatch(EventType.MONITOR_ENTER, toDots((String) getTop()));
			break;
		case MONITOREXIT:
			tryMatch(EventType.MONITOR_EXIT, toDots((String) getTop()));
			break;
		case ATHROW:
			tryMatch(EventType.THROW, toDots((String) getTop()));
			break;
		}

		insertBeforeMonitors();
		super.visitInsn(opcode);
		insertAfterMonitors();
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		reset();

		String fieldName = toDots(owner) + '.' + name;

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
			tryMatch(EventType.INSTANCE, toDots(desc), EventOrder.BEFORE);
			break;
		case ANEWARRAY:
			tryMatch(EventType.INSTANCE_ARRAY, toDots(desc));
			break;
		}

		insertBeforeMonitors();
		super.visitTypeInsn(opcode, desc);
		insertAfterMonitors();
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
		reset();

		String methodOwner = toDots(owner);
		String methodName = methodOwner + '.' + name;
		int numArgs = Type.getMethodType(desc).getArgumentTypes().length;

		switch (opcode) {
		case INVOKESPECIAL:
			if (name.equals("<init>"))
				tryMatch(EventType.INSTANCE, methodOwner, EventOrder.AFTER);
			else
				tryMatch(EventType.METHOD_CALL, methodName, numArgs);
			break;
		case INVOKESTATIC:
		case INVOKEVIRTUAL:
		case INVOKEINTERFACE:
			tryMatch(EventType.METHOD_CALL, methodName, numArgs);
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
			if (matchesFrom.get(m) && m.matchesOf(of))
				addMonitors(m, of);

		currentType = type;
	}

	private void tryMatch(EventType type, String of, int numArgs) {
		for (EventPatternMatcher m : mapped.get(type))
			if (matchesFrom.get(m) && m.matchesOf(of))
				addMonitors(m, of, numArgs);

		currentType = type;
	}

	private void tryMatch(EventType type, String of, EventOrder order) {
		for (EventPatternMatcher m : mapped.get(type))
			if (matchesFrom.get(m) && m.matchesOf(of))
				addMonitors(m, order, of);

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
		shouldGenerateLocal = shouldGenerateDup = shouldGenerateArgsArray = false;
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
				if (e.getType() == EventType.METHOD_CALL) {
					shouldGenerateArgsArray = true;
					currentNumArgs = numArgs;
				}

				afterMonitors.add(d);
				break;
			case INSTEAD:
				insteadMonitors.add(d);
				break;
			}
		}
	}

	private void insertBeforeMonitors() {
		if (currentType == null)
			return;

		if (shouldGenerateLocal)
			generatedLocal = captureLocal();

		if (shouldGenerateArgsArray) {
			LocalVariable[] vars = captureMethodArguments(currentNumArgs);
			restoreMethodArguments(currentNumArgs, vars);
			generatedArgsArray = vars[vars.length - 1].getIndex();
		}

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
				visitMethodCallBefore(e);
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
		if (currentType == null)
			return;

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
			for (EventData e : afterMonitors)
				visitMethodCallAfter(e);
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

	/*
	 * Creates a local variable holding boxed value on the top of the stack.
	 */
	private int captureLocal() {
		Type topType = getTopType();

		if (isTwoWords(topType))
			super.visitInsn(DUP2);
		else
			super.visitInsn(DUP);

		int l = sorter.newLocal(autobox(topType));
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
		autobox(getTopType());
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

	private void visitMethodCallBefore(EventData e) {
		LocalVariable[] vars = captureMethodArguments(e.getNumArgs());
		visitEventStart(e);
		super.visitLdcInsn(e.getName());
		visitEventEndWithArgs(e, vars[e.getNumArgs()].getIndex());
		restoreMethodArguments(e.getNumArgs(), vars);
	}

	private void visitMethodCallAfter(EventData e) {
		visitEventStart(e);
		super.visitLdcInsn(e.getName());
		visitEventEndWithArgs(e, generatedArgsArray);
	}

	/*
	 * Builds an array of indices of local variables added to preserve the arguments
	 * of instrumented method in between the event generation. Also, the last entry
	 * contains the index of an array of boxed arguments ready to be passed to an
	 * event.
	 */
	private LocalVariable[] captureMethodArguments(int numArgs) {
		LocalVariable[] localsVars = new LocalVariable[numArgs + 1];

		pushInt(numArgs);
		super.visitTypeInsn(ANEWARRAY, "java/lang/Object");

		int methodArgs = sorter.newLocal(OBJECT_ARRAY_TYPE);
		super.visitVarInsn(ASTORE, methodArgs);

		Type topType;
		boolean twoWords;

		for (int i = numArgs - 1; i >= 0; i--) {
			topType = getTopType();
			twoWords = isTwoWords(topType);

			if (twoWords)
				super.visitInsn(DUP2);
			else
				super.visitInsn(DUP);

			Pair<Integer, Integer> insns = getPrimitiveInsns(topType);

			int l = sorter.newLocal(topType);
			super.visitVarInsn(insns.getKey(), l);

			localsVars[i] = new LocalVariable(l, insns.getValue());

			super.visitVarInsn(ALOAD, methodArgs);

			if (twoWords) {
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
			} else {
				super.visitInsn(SWAP);
			}

			if (!insns.equals(REF_INSNS))
				autoboxWithoutDup(topType);

			pushInt(i);

			super.visitInsn(SWAP);
			super.visitInsn(AASTORE);
		}

		localsVars[numArgs] = new LocalVariable(methodArgs, 0);

		return localsVars;
	}

	/*
	 * Pushes the values of captured method arguments back onto the stack.
	 */
	private void restoreMethodArguments(int numArgs, LocalVariable[] localVars) {
		for (int i = 0; i < numArgs; i++)
			super.visitVarInsn(localVars[i].getLoadInsn(), localVars[i].getIndex());
	}

	private void visitEventStart(EventData e) {
		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/Resolver", e.getMonitor(),
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassType : monitorClassType);
		super.visitTypeInsn(NEW, "com/mpraski/jmonitor/Event");
		super.visitInsn(DUP);

		if (e.getTag() == null)
			super.visitInsn(ACONST_NULL);
		else
			super.visitLdcInsn(e.getTag());

		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/EventType", eventType(e.getType()),
				"Lcom/mpraski/jmonitor/EventType;");
	}

	private void visitEventStartWithSwap(EventData e) {
		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/Resolver", e.getMonitor(),
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassType : monitorClassType);
		super.visitInsn(SWAP);
		super.visitTypeInsn(NEW, "com/mpraski/jmonitor/Event");
		super.visitInsn(DUP_X1);
		super.visitInsn(SWAP);

		if (e.getTag() == null)
			super.visitInsn(ACONST_NULL);
		else
			super.visitLdcInsn(e.getTag());

		super.visitInsn(SWAP);

		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/EventType", eventType(e.getType()),
				"Lcom/mpraski/jmonitor/EventType;");

		super.visitInsn(SWAP);
	}

	private void visitEventEnd(EventData e) {
		super.visitInsn(ACONST_NULL);

		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;",
				false);
		super.visitMethodInsn(INVOKESPECIAL, "com/mpraski/jmonitor/Event", "<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/EventType;Ljava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		super.visitMethodInsn(INVOKEINTERFACE, e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClass : monitorClass,
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassFunc : monitorClassFunc,
				"(Lcom/mpraski/jmonitor/Event;)V", true);
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
		super.visitMethodInsn(INVOKESPECIAL, "com/mpraski/jmonitor/Event", "<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/EventType;Ljava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		super.visitMethodInsn(INVOKEINTERFACE, e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClass : monitorClass,
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassFunc : monitorClassFunc,
				"(Lcom/mpraski/jmonitor/Event;)V", true);
	}

	private void visitEventEndWithArgs(EventData e, int arrLocal) {
		super.visitVarInsn(ALOAD, arrLocal);

		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;",
				false);
		super.visitMethodInsn(INVOKESPECIAL, "com/mpraski/jmonitor/Event", "<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/EventType;Ljava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		super.visitMethodInsn(INVOKEINTERFACE, e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClass : monitorClass,
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassFunc : monitorClassFunc,
				"(Lcom/mpraski/jmonitor/Event;)V", true);
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
		super.visitMethodInsn(INVOKESPECIAL, "com/mpraski/jmonitor/Event", "<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/EventType;Ljava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		super.visitMethodInsn(INVOKEINTERFACE, e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClass : monitorClass,
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassFunc : monitorClassFunc,
				"(Lcom/mpraski/jmonitor/Event;)V", true);
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
	 * Returns the type of value on the top of the stack. Aware of long/double types
	 * taking two words.
	 */
	private Type getTopType() {
		Object v2 = getTop();

		if (v2 instanceof String)
			return Type.getObjectType((String) v2);

		if (stack.size() > 1) {
			Object v1 = stack.get(stack.size() - 2);

			if (v1 instanceof Integer && v2 instanceof Integer && v2.equals(TOP)) {
				Integer desc = (Integer) v1;

				if (desc.equals(LONG))
					return Type.LONG_TYPE;
				else if (desc.equals(DOUBLE))
					return Type.DOUBLE_TYPE;

				throw new IllegalStateException("Cannot autobox TOP");
			}
		}

		Integer i = (Integer) v2;

		if (i.equals(INTEGER))
			return Type.INT_TYPE;
		else if (i.equals(FLOAT))
			return Type.FLOAT_TYPE;

		throw new IllegalStateException("Should not reach this state");
	}

	/*
	 * Attempts to produce a boxed value from descriptor of type of the value on top
	 * of the stack.
	 */
	private String autobox(String desc) {
		if (desc.length() > 1)
			return desc;

		Pair<String, String> type = getPrimitiveClass(desc);

		super.visitMethodInsn(INVOKESTATIC, type.getKey(), "valueOf", type.getValue(), false);

		return type.getKey();
	}

	/*
	 * Pushes a boxed value of the top of the stack.
	 */
	private Type autobox(Type type) {
		if (type.equals(Type.LONG_TYPE)) {
			super.visitInsn(DUP2);
			super.visitMethodInsn(INVOKESTATIC, TYPE_LONG.getKey(), "valueOf", TYPE_LONG.getValue(), false);
			return LONG_TYPE;
		} else if (type.equals(Type.DOUBLE_TYPE)) {
			super.visitInsn(DUP2);
			super.visitMethodInsn(INVOKESTATIC, TYPE_DOUBLE.getKey(), "valueOf", TYPE_DOUBLE.getValue(), false);
			return DOUBLE_TYPE;
		} else if (type.equals(Type.INT_TYPE)) {
			super.visitInsn(DUP);
			super.visitMethodInsn(INVOKESTATIC, TYPE_INTEGER.getKey(), "valueOf", TYPE_INTEGER.getValue(), false);
			return INTEGER_TYPE;
		} else if (type.equals(Type.FLOAT_TYPE)) {
			super.visitInsn(DUP);
			super.visitMethodInsn(INVOKESTATIC, TYPE_FLOAT.getKey(), "valueOf", TYPE_FLOAT.getValue(), false);
			return FLOAT_TYPE;
		}

		return type;
	}

	/*
	 * Pushes a boxed value of the top of the stack. Does not duplicate the raw
	 * value.
	 */
	private String autoboxWithoutDup(Type type) {
		if (type.equals(Type.LONG_TYPE)) {
			super.visitMethodInsn(INVOKESTATIC, TYPE_LONG.getKey(), "valueOf", TYPE_LONG.getValue(), false);
			return TYPE_LONG.getKey();
		} else if (type.equals(Type.DOUBLE_TYPE)) {
			super.visitMethodInsn(INVOKESTATIC, TYPE_DOUBLE.getKey(), "valueOf", TYPE_DOUBLE.getValue(), false);
			return TYPE_DOUBLE.getKey();
		} else if (type.equals(Type.INT_TYPE)) {
			super.visitMethodInsn(INVOKESTATIC, TYPE_INTEGER.getKey(), "valueOf", TYPE_INTEGER.getValue(), false);
			return TYPE_INTEGER.getKey();
		} else if (type.equals(Type.FLOAT_TYPE)) {
			super.visitMethodInsn(INVOKESTATIC, TYPE_FLOAT.getKey(), "valueOf", TYPE_FLOAT.getValue(), false);
			return TYPE_FLOAT.getKey();
		}

		return type.getInternalName();
	}

	private static Pair<Integer, Integer> getPrimitiveInsns(Type t) {
		if (t.equals(Type.INT_TYPE))
			return INTEGER_INSNS;
		else if (t.equals(Type.FLOAT_TYPE))
			return FLOAT_INSNS;
		else if (t.equals(Type.LONG_TYPE))
			return LONG_INSNS;
		else if (t.equals(Type.DOUBLE_TYPE))
			return DOUBLE_INSNS;

		return REF_INSNS;
	}

	private static String toDots(String s) {
		return s.replace('/', '.');
	}

	private static boolean isTwoWords(Type type) {
		return type.equals(Type.LONG_TYPE) || type.equals(Type.DOUBLE_TYPE);
	}
}
