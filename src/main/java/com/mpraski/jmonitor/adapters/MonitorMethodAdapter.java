package com.mpraski.jmonitor.adapters;

import static com.mpraski.jmonitor.util.Constants.CLASS_DOUBLE;
import static com.mpraski.jmonitor.util.Constants.CLASS_FLOAT;
import static com.mpraski.jmonitor.util.Constants.CLASS_INTEGER;
import static com.mpraski.jmonitor.util.Constants.CLASS_LONG;
import static com.mpraski.jmonitor.util.Constants.INSNS_DOUBLE;
import static com.mpraski.jmonitor.util.Constants.INSNS_FLOAT;
import static com.mpraski.jmonitor.util.Constants.INSNS_INT;
import static com.mpraski.jmonitor.util.Constants.INSNS_LONG;
import static com.mpraski.jmonitor.util.Constants.INSNS_REF;
import static com.mpraski.jmonitor.util.Constants.eventOrder;
import static com.mpraski.jmonitor.util.Constants.eventType;
import static com.mpraski.jmonitor.util.Constants.getPrimitiveClass;
import static com.mpraski.jmonitor.util.Constants.insteadMonitorClass;
import static com.mpraski.jmonitor.util.Constants.insteadMonitorClassFunc;
import static com.mpraski.jmonitor.util.Constants.insteadMonitorClassFuncType;
import static com.mpraski.jmonitor.util.Constants.insteadMonitorClassType;
import static com.mpraski.jmonitor.util.Constants.monitorClass;
import static com.mpraski.jmonitor.util.Constants.monitorClassFunc;
import static com.mpraski.jmonitor.util.Constants.monitorClassFuncType;
import static com.mpraski.jmonitor.util.Constants.monitorClassType;
import static com.mpraski.jmonitor.util.Constants.typeOfArray;
import static com.mpraski.jmonitor.util.Constants.typeOfDouble;
import static com.mpraski.jmonitor.util.Constants.typeOfFloat;
import static com.mpraski.jmonitor.util.Constants.typeOfInteger;
import static com.mpraski.jmonitor.util.Constants.typeOfLong;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import com.mpraski.jmonitor.EventMonitor;
import com.mpraski.jmonitor.EventOrder;
import com.mpraski.jmonitor.EventPatternMatcher;
import com.mpraski.jmonitor.EventType;
import com.mpraski.jmonitor.instead.FieldReadGenerator;
import com.mpraski.jmonitor.util.Pair;

/*
 * This MethodVisitor is responsible for injecting appropriate bytecode instructions to perform instrumentation.
 */
public class MonitorMethodAdapter extends AnalyzerAdapter implements Opcodes {

	/*
	 * Instance of enclosing MonitorClassAdapter, used to get indices of next inner
	 * class / accessor method.
	 */
	private final MonitorClassAdapter adapter;

	/*
	 * Used to add local variables in cases where certain values (e.g. method
	 * arguments, event arguments array) need to be preserved.
	 */
	private final LocalVariablesSorter sorter;

	private final String thisName, thisDesc, thisOwner, thisRet, thisSource, originalName;
	private final Map<EventType, List<EventPatternMatcher>> mapped;
	private final Map<EventPatternMatcher, Boolean> matchesFrom;

	private final List<EventData> eventsBefore, eventsAfter, eventsInstead;

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
	private int currentLineNumer;
	private EventType currentType;

	protected MonitorMethodAdapter(String owner, int access, String name, String desc, String source,
			MonitorClassAdapter adapter, LocalVariablesSorter sorter, List<EventPatternMatcher> matchers,
			Map<EventType, List<EventPatternMatcher>> mapped, Map<EventPatternMatcher, Boolean> matchesFrom,
			List<EventData> eventsBefore, List<EventData> eventsAfter, List<EventData> eventsInstead) {
		super(ASM5, owner, access, name, desc, sorter);

		this.thisOwner = owner;
		this.originalName = name;
		this.thisName = toDots(owner) + '.' + name;
		this.thisDesc = desc;
		this.thisRet = toDots(Type.getMethodType(desc).getReturnType().getInternalName());
		this.thisSource = source;
		this.adapter = adapter;
		this.sorter = sorter;
		this.mapped = mapped;
		this.matchesFrom = matchesFrom;

		matchesFrom.clear();

		/*
		 * Compute all the 'from' matches only once, they won't change throughout the
		 * method execution.
		 */
		for (EventPatternMatcher m : matchers)
			matchesFrom.put(m, m.matchesFrom(thisName));

		this.eventsBefore = eventsBefore;
		this.eventsAfter = eventsAfter;
		this.eventsInstead = eventsInstead;

		System.out.println("Visiting " + thisName + " | " + thisDesc);
	}

	@Override
	public void visitInsn(int opcode) {
		reset();

		switch (opcode) {
		case IRETURN:
		case LRETURN:
		case FRETURN:
		case DRETURN:
			tryMatch(EventType.RETURN, thisRet);
			break;
		case ARETURN:
			tryMatch(EventType.RETURN, thisRet);
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

		instrumentBefore();
		instrumentInstead();
		super.visitInsn(opcode);
		instrumentAfter();
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

		instrumentBefore();
		instrumentInstead();
		super.visitFieldInsn(opcode, owner, name, desc);
		instrumentAfter();
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		currentLineNumer = line;
		super.visitLineNumber(line, start);
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

		instrumentBefore();
		instrumentInstead();
		super.visitTypeInsn(opcode, desc);
		instrumentAfter();
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

		instrumentBefore();
		instrumentInstead();
		super.visitMethodInsn(opcode, owner, name, desc, isInterface);
		instrumentAfter();
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
		currentType = null;
		eventsBefore.clear();
		eventsAfter.clear();
		eventsInstead.clear();
		shouldGenerateLocal = shouldGenerateDup = shouldGenerateArgsArray = false;
	}

	private void addMonitors(EventPatternMatcher e) {
		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder());
			switch (m.getOrder()) {
			case BEFORE:
				eventsBefore.add(d);
				break;
			case AFTER:
				eventsAfter.add(d);
				break;
			case INSTEAD:
				eventsInstead.add(d);
				break;
			}
		}
	}

	private void addMonitors(EventPatternMatcher e, String of) {
		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder(), of);
			switch (m.getOrder()) {
			case BEFORE:
				eventsBefore.add(d);
				break;
			case AFTER:
				if (e.getType() == EventType.MONITOR_ENTER || e.getType() == EventType.MONITOR_EXIT)
					shouldGenerateDup = true;

				eventsAfter.add(d);
				break;
			case INSTEAD:
				eventsInstead.add(d);
				break;
			}
		}
	}

	private void addMonitors(EventPatternMatcher e, String name, String desc, String owner) {
		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder(), name, desc, owner);
			switch (m.getOrder()) {
			case BEFORE:
				eventsBefore.add(d);
				break;
			case AFTER:
				eventsAfter.add(d);
				break;
			case INSTEAD:
				eventsInstead.add(d);
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
				eventsBefore.add(d);
				break;
			case AFTER:
				eventsAfter.add(d);
				break;
			case INSTEAD:
				eventsInstead.add(d);
				break;
			}
		}
	}

	private void addMonitors(EventPatternMatcher e, String of, int numArgs) {
		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder(), of, numArgs);
			switch (m.getOrder()) {
			case BEFORE:
				eventsBefore.add(d);
				break;
			case AFTER:
				if (e.getType() == EventType.METHOD_CALL) {
					shouldGenerateArgsArray = true;
					currentNumArgs = numArgs;
				}

				eventsAfter.add(d);
				break;
			case INSTEAD:
				eventsInstead.add(d);
				break;
			}
		}
	}

	private void instrumentBefore() {
		if (currentType == null)
			return;

		if (shouldGenerateLocal) {
			generatedLocal = captureLocal();
		} else if (shouldGenerateArgsArray) {
			LocalVariable[] vars = captureMethodArguments(currentNumArgs);
			restoreMethodArguments(currentNumArgs, vars);
			generatedArgsArray = vars[vars.length - 1].getIndex();
		} else if (shouldGenerateDup) {
			super.visitInsn(DUP);
		}

		switch (currentType) {
		case FIELD_READ:
			eventsBefore.forEach(this::visitFieldRead);
			break;
		case FIELD_WRITE:
			eventsBefore.forEach(this::visitFieldWrite);
			break;
		case FIELD_READ_STATIC:
			eventsBefore.forEach(this::visitStaticFieldRead);
			break;
		case FIELD_WRITE_STATIC:
			eventsBefore.forEach(this::visitStaticFieldWrite);
			break;
		case METHOD_CALL:
			eventsBefore.forEach(this::visitMethodCallBefore);
			break;
		case RETURN:
		case THROW:
			eventsBefore.forEach(this::visitTopWithAutobox);
			break;
		case INSTANCE:
		case INSTANCE_ARRAY:
			eventsBefore.forEach(this::visitNewInstance);
			break;
		case MONITOR_ENTER:
		case MONITOR_EXIT:
			eventsBefore.forEach(this::visitTopWithAutobox);
			break;
		}
	}

	private void instrumentInstead() {
		if (currentType == null)
			return;

		switch (currentType) {
		case RETURN:
			eventsInstead.forEach(this::visitReturnInstead);
			break;
		case THROW:
			eventsInstead.forEach(this::visitThrowInstead);
			break;
		case MONITOR_ENTER:
		case MONITOR_EXIT:
			eventsInstead.forEach(this::visitTopWithSwap);
			break;
		case FIELD_READ:
			eventsInstead.forEach(this::visitReadInstead);
			break;
		}
	}

	private void instrumentAfter() {
		if (currentType == null)
			return;

		switch (currentType) {
		case FIELD_READ:
		case FIELD_READ_STATIC:
			eventsAfter.forEach(this::visitTopWithAutobox);
			break;
		case FIELD_WRITE:
			eventsAfter.forEach(this::visitFieldWrite);
			break;
		case FIELD_WRITE_STATIC:
			eventsAfter.forEach(this::visitStaticFieldWrite);
			break;
		case METHOD_CALL:
			eventsAfter.forEach(this::visitMethodCallAfter);
			break;
		case INSTANCE:
		case INSTANCE_ARRAY:
			eventsAfter.forEach(this::visitNewInstanceAfter);
			break;
		case MONITOR_ENTER:
			eventsAfter.forEach(this::visitTopWithAutobox);
			break;
		case MONITOR_EXIT:
			eventsAfter.forEach(this::visitTopWithSwap);
			break;
		}
	}

	/*
	 * Creates a local variable holding boxed value on the top of the stack.
	 */
	private int captureLocal() {
		Type topType = getTopType();

		if (takesTwoWords(topType))
			super.visitInsn(DUP2);
		else
			super.visitInsn(DUP);

		int l = sorter.newLocal(box(topType));
		super.visitVarInsn(ASTORE, l);

		return l;
	}

	private void visitFieldRead(EventData e) {
		visitEventStart(e);
		super.visitVarInsn(ALOAD, 0);
		super.visitFieldInsn(GETFIELD, e.getOwner(), e.getName(), e.getDesc());
		box(e.getDesc());
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);
	}

	private void visitFieldWrite(EventData e) {
		visitEventStart(e);
		super.visitVarInsn(ALOAD, 0);
		super.visitFieldInsn(GETFIELD, e.getOwner(), e.getName(), e.getDesc());
		box(e.getDesc());
		visitEventEndWithArg(e, generatedLocal);
	}

	private void visitStaticFieldRead(EventData e) {
		visitEventStart(e);
		super.visitFieldInsn(GETSTATIC, e.getOwner(), e.getName(), e.getDesc());
		box(e.getDesc());
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);
	}

	private void visitStaticFieldWrite(EventData e) {
		visitEventStart(e);
		super.visitFieldInsn(GETSTATIC, e.getOwner(), e.getName(), e.getDesc());
		box(e.getDesc());
		visitEventEndWithArg(e, generatedLocal);
	}

	private void visitTopWithAutobox(EventData e) {
		box(getTopType());
		visitEventStartWithSwap(e);
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);
	}

	private void visitTopWithSwap(EventData e) {
		visitEventStartWithSwap(e);
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);
	}

	private void visitNewInstanceAfter(EventData e) {
		super.visitInsn(DUP);
		visitEventStartWithSwap(e);
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);
	}

	private void visitNewInstance(EventData e) {
		visitEventStart(e);
		super.visitInsn(ACONST_NULL);
		super.visitInsn(ICONST_1);
		super.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		super.visitInsn(DUP);
		super.visitInsn(ICONST_0);
		super.visitLdcInsn(e.getName());
		super.visitInsn(AASTORE);
		visitEventEnd(e);
	}

	private void visitMethodCallBefore(EventData e) {
		LocalVariable[] vars = captureMethodArguments(e.getNumArgs());
		visitEventStart(e);
		super.visitLdcInsn(e.getName());
		super.visitVarInsn(ALOAD, vars[e.getNumArgs()].getIndex());
		visitEventEnd(e);
		restoreMethodArguments(e.getNumArgs(), vars);
	}

	private void visitReturnInstead(EventData e) {
		Type oldType = getTopType();

		boxWithoutDup(oldType);

		visitEventStartWithSwap(e);
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);

		if (isReference(oldType))
			super.visitTypeInsn(CHECKCAST, oldType.getInternalName());
		else
			unbox(oldType);
	}

	private void visitThrowInstead(EventData e) {
		Type oldType = getTopType();

		visitEventStartWithSwap(e);
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);

		super.visitTypeInsn(CHECKCAST, oldType.getInternalName());
	}

	private void visitMethodCallAfter(EventData e) {
		visitEventStart(e);
		super.visitLdcInsn(e.getName());
		super.visitVarInsn(ALOAD, generatedArgsArray);
		visitEventEnd(e);
	}

	private void visitReadInstead(EventData e) {
		Type type = Type.getObjectType(thisOwner);
		adapter.addActionGenerator(new FieldReadGenerator(adapter.getNextInnerClass(), thisOwner, originalName,
				thisDesc, "(" + type.getDescriptor() + ")" + e.getDesc(), e.getName(), e.getDesc()));
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

		int methodArgs = sorter.newLocal(typeOfArray);
		super.visitVarInsn(ASTORE, methodArgs);

		Type topType;
		boolean twoWords;

		for (int i = numArgs - 1; i >= 0; i--) {
			topType = getTopType();
			twoWords = takesTwoWords(topType);

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

			if (!insns.equals(INSNS_REF))
				boxWithoutDup(topType);

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

		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/EventOrder", eventOrder(e.getOrder()),
				"Lcom/mpraski/jmonitor/EventOrder;");

		if (thisSource == null)
			super.visitInsn(ACONST_NULL);
		else
			super.visitLdcInsn(thisSource);

		pushInt(currentLineNumer);
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

		super.visitFieldInsn(GETSTATIC, "com/mpraski/jmonitor/EventOrder", eventOrder(e.getOrder()),
				"Lcom/mpraski/jmonitor/EventOrder;");

		super.visitInsn(SWAP);

		if (thisSource == null)
			super.visitInsn(ACONST_NULL);
		else
			super.visitLdcInsn(thisSource);

		super.visitInsn(SWAP);

		pushInt(currentLineNumer);

		super.visitInsn(SWAP);
	}

	private void visitEventEnd(EventData e) {
		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;",
				false);
		super.visitMethodInsn(INVOKESPECIAL, "com/mpraski/jmonitor/Event", "<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/EventType;Lcom/mpraski/jmonitor/EventOrder;Ljava/lang/String;ILjava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		if (e.getOrder() == EventOrder.INSTEAD)
			super.visitMethodInsn(INVOKEINTERFACE, insteadMonitorClass, insteadMonitorClassFunc,
					insteadMonitorClassFuncType, true);
		else
			super.visitMethodInsn(INVOKEINTERFACE, monitorClass, monitorClassFunc, monitorClassFuncType, true);
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
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/EventType;Lcom/mpraski/jmonitor/EventOrder;Ljava/lang/String;ILjava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		if (e.getOrder() == EventOrder.INSTEAD)
			super.visitMethodInsn(INVOKEINTERFACE, insteadMonitorClass, insteadMonitorClassFunc,
					insteadMonitorClassFuncType, true);
		else
			super.visitMethodInsn(INVOKEINTERFACE, monitorClass, monitorClassFunc, monitorClassFuncType, true);
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
			super.visitIntInsn(BIPUSH, i);
		} else if (i > 127 && i < 32768) {
			super.visitIntInsn(SIPUSH, i);
		} else {
			super.visitLdcInsn(i);
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

			if (v1 instanceof Integer && v2.equals(TOP)) {
				Integer desc = (Integer) v1;

				if (desc.equals(LONG))
					return Type.LONG_TYPE;
				else if (desc.equals(DOUBLE))
					return Type.DOUBLE_TYPE;
			}
		}

		Integer i = (Integer) v2;

		if (i.equals(INTEGER))
			return Type.INT_TYPE;
		else if (i.equals(FLOAT))
			return Type.FLOAT_TYPE;

		throw new IllegalStateException("Neither a reference nor a primitive");
	}

	/*
	 * Attempts to produce a boxed value from descriptor of type of the value on top
	 * of the stack.
	 */
	private void box(String desc) {
		if (desc.length() > 1)
			return;

		Pair<String, String> type = getPrimitiveClass(desc);

		super.visitMethodInsn(INVOKESTATIC, type.getKey(), "valueOf", type.getValue(), false);
	}

	/*
	 * Pushes a boxed value of the top of the stack.
	 */
	private Type box(Type type) {
		if (type.equals(Type.LONG_TYPE)) {
			super.visitInsn(DUP2);
			super.visitMethodInsn(INVOKESTATIC, CLASS_LONG.getKey(), "valueOf", CLASS_LONG.getValue(), false);
			return typeOfLong;
		} else if (type.equals(Type.DOUBLE_TYPE)) {
			super.visitInsn(DUP2);
			super.visitMethodInsn(INVOKESTATIC, CLASS_DOUBLE.getKey(), "valueOf", CLASS_DOUBLE.getValue(), false);
			return typeOfDouble;
		} else if (type.equals(Type.INT_TYPE)) {
			super.visitInsn(DUP);
			super.visitMethodInsn(INVOKESTATIC, CLASS_INTEGER.getKey(), "valueOf", CLASS_INTEGER.getValue(), false);
			return typeOfInteger;
		} else if (type.equals(Type.FLOAT_TYPE)) {
			super.visitInsn(DUP);
			super.visitMethodInsn(INVOKESTATIC, CLASS_FLOAT.getKey(), "valueOf", CLASS_FLOAT.getValue(), false);
			return typeOfFloat;
		}

		return type;
	}

	/*
	 * Pushes a boxed value of the top of the stack. Does not duplicate the raw
	 * value.
	 */
	private Type boxWithoutDup(Type type) {
		if (type.equals(Type.LONG_TYPE)) {
			super.visitMethodInsn(INVOKESTATIC, CLASS_LONG.getKey(), "valueOf", CLASS_LONG.getValue(), false);
			return typeOfLong;
		} else if (type.equals(Type.DOUBLE_TYPE)) {
			super.visitMethodInsn(INVOKESTATIC, CLASS_DOUBLE.getKey(), "valueOf", CLASS_DOUBLE.getValue(), false);
			return typeOfDouble;
		} else if (type.equals(Type.INT_TYPE)) {
			super.visitMethodInsn(INVOKESTATIC, CLASS_INTEGER.getKey(), "valueOf", CLASS_INTEGER.getValue(), false);
			return typeOfInteger;
		} else if (type.equals(Type.FLOAT_TYPE)) {
			super.visitMethodInsn(INVOKESTATIC, CLASS_FLOAT.getKey(), "valueOf", CLASS_FLOAT.getValue(), false);
			return typeOfFloat;
		}

		return type;
	}

	private void unbox(Type type) {
		if (type.equals(Type.LONG_TYPE)) {
			super.visitTypeInsn(CHECKCAST, CLASS_LONG.getKey());
			super.visitMethodInsn(INVOKEVIRTUAL, CLASS_LONG.getKey(), "longValue", "()J", false);
		} else if (type.equals(Type.DOUBLE_TYPE)) {
			super.visitTypeInsn(CHECKCAST, CLASS_DOUBLE.getKey());
			super.visitMethodInsn(INVOKEVIRTUAL, CLASS_DOUBLE.getKey(), "doubleValue", "()D", false);
		} else if (type.equals(Type.INT_TYPE)) {
			super.visitTypeInsn(CHECKCAST, CLASS_INTEGER.getKey());
			super.visitMethodInsn(INVOKEVIRTUAL, CLASS_INTEGER.getKey(), "intValue", "()I", false);
		} else if (type.equals(Type.FLOAT_TYPE)) {
			super.visitTypeInsn(CHECKCAST, CLASS_FLOAT.getKey());
			super.visitMethodInsn(INVOKEVIRTUAL, CLASS_FLOAT.getKey(), "floatValue", "()F", false);
		}
	}

	private static boolean isReference(Type type) {
		return type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;
	}

	private static Pair<Integer, Integer> getPrimitiveInsns(Type t) {
		if (t.equals(Type.INT_TYPE))
			return INSNS_INT;
		else if (t.equals(Type.FLOAT_TYPE))
			return INSNS_FLOAT;
		else if (t.equals(Type.LONG_TYPE))
			return INSNS_LONG;
		else if (t.equals(Type.DOUBLE_TYPE))
			return INSNS_DOUBLE;

		return INSNS_REF;
	}

	private static String toDots(String s) {
		return s.replace('/', '.');
	}

	private static boolean takesTwoWords(Type type) {
		return type.equals(Type.LONG_TYPE) || type.equals(Type.DOUBLE_TYPE);
	}
}
