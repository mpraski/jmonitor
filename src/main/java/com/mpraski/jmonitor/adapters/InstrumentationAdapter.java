package com.mpraski.jmonitor.adapters;

import static com.mpraski.jmonitor.util.Constants.CLASS_DOUBLE;
import static com.mpraski.jmonitor.util.Constants.CLASS_FLOAT;
import static com.mpraski.jmonitor.util.Constants.CLASS_INTEGER;
import static com.mpraski.jmonitor.util.Constants.CLASS_LONG;
import static com.mpraski.jmonitor.util.Constants.INSNS_REF;
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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import com.mpraski.jmonitor.EventOrder;
import com.mpraski.jmonitor.instead.FieldReadGenerator;
import com.mpraski.jmonitor.instead.FieldWriteGenerator;
import com.mpraski.jmonitor.instead.InsteadActionGenerator;
import com.mpraski.jmonitor.instead.MethodCallGenerator;
import com.mpraski.jmonitor.instead.NewInstanceGenerator;
import com.mpraski.jmonitor.util.Pair;
import com.mpraski.jmonitor.util.EventUtil;
import com.mpraski.jmonitor.util.TypeUtil;

public class InstrumentationAdapter extends AnalyzerAdapter implements Opcodes {

	/*
	 * Instance of enclosing MonitorClassAdapter, used to get indices of next inner
	 * class / accessor method.
	 */
	private final ClassAdapter adapter;

	/*
	 * Used to add local variables in cases where certain values (e.g. method
	 * arguments, event arguments array) need to be preserved.
	 */
	private final LocalVariablesSorter sorter;

	/*
	 * Characteristics of instrumented method
	 */
	private final String name;
	private final String desc;
	private final String owner;
	private final String source;
	private final Type ownerType;

	/*
	 * Temporaries signaling presence of certain scenarios (e.g. capture local
	 * variable for sending event after field write)
	 */
	private boolean shouldGenerateLocal;
	private boolean shouldGenerateDup;
	private boolean shouldGenerateArgsArray;
	private boolean shouldPreserveOriginal = true;

	private int line;
	private int generatedLocal;
	private int generatedArgsArray;
	private int currentNumArgs;

	public InstrumentationAdapter(
			String owner,
			int access,
			String name,
			String descriptor,
			String source,
			ClassAdapter adapter,
			LocalVariablesSorter sorter) {
		super(ASM5, owner, access, name, descriptor, sorter);

		this.adapter = adapter;
		this.sorter = sorter;
		this.name = name;
		this.desc = descriptor;
		this.owner = owner;
		this.source = source;
		this.ownerType = Type.getObjectType(owner);
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		this.line = line;
		super.visitLineNumber(line, start);
	}

	public boolean shouldGenerateLocal() {
		return shouldGenerateLocal;
	}

	public boolean shouldGenerateDup() {
		return shouldGenerateDup;
	}

	public boolean shouldGenerateArgsArray() {
		return shouldGenerateArgsArray;
	}

	public boolean shouldPreserveOriginal() {
		return shouldPreserveOriginal;
	}

	public void generateLocal() {
		generatedLocal = captureLocal();
	}

	public void generatedArgsArray() {
		LocalVariable[] vars = captureMethodArguments(currentNumArgs);
		restoreMethodArguments(currentNumArgs, vars);
		generatedArgsArray = vars[vars.length - 1].getIndex();
	}

	public void setShouldGenerateLocal(boolean shouldGenerateLocal) {
		this.shouldGenerateLocal = shouldGenerateLocal;
	}

	public void setShouldGenerateDup(boolean shouldGenerateDup) {
		this.shouldGenerateDup = shouldGenerateDup;
	}

	public void setShouldGenerateArgsArray(boolean shouldGenerateArgsArray) {
		this.shouldGenerateArgsArray = shouldGenerateArgsArray;
	}

	public void setShouldPreserveOriginal(boolean shouldPreserveOriginal) {
		this.shouldPreserveOriginal = shouldPreserveOriginal;
	}

	public void setNumArgs(int args) {
		this.currentNumArgs = args;
	}

	public void reset() {
		shouldGenerateLocal = shouldGenerateDup = shouldGenerateArgsArray = false;
		shouldPreserveOriginal = true;
	}

	public void visitFieldRead(EventData e) {
		visitEventStart(e);
		super.visitVarInsn(ALOAD, 0);
		super.visitFieldInsn(GETFIELD, e.getOwner(), e.getName(), e.getDesc());
		box(e.getDesc());
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);
	}

	public void visitFieldWrite(EventData e) {
		visitEventStart(e);
		super.visitVarInsn(ALOAD, 0);
		super.visitFieldInsn(GETFIELD, e.getOwner(), e.getName(), e.getDesc());
		box(e.getDesc());
		visitEventEndWithArg(e, generatedLocal);
	}

	public void visitStaticFieldRead(EventData e) {
		visitEventStart(e);
		super.visitFieldInsn(GETSTATIC, e.getOwner(), e.getName(), e.getDesc());
		box(e.getDesc());
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);
	}

	public void visitStaticFieldWrite(EventData e) {
		visitEventStart(e);
		super.visitFieldInsn(GETSTATIC, e.getOwner(), e.getName(), e.getDesc());
		box(e.getDesc());
		visitEventEndWithArg(e, generatedLocal);
	}

	public void visitTopWithAutobox(EventData e) {
		box(getTopType());
		visitEventStartWithSwap(e);
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);
	}

	public void visitTopWithSwap(EventData e) {
		visitEventStartWithSwap(e);
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);
	}

	public void visitNewInstanceAfter(EventData e) {
		super.visitInsn(DUP);
		visitEventStartWithSwap(e);
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);
	}

	public void visitNewInstance(EventData e) {
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

	public void visitMethodCallBefore(EventData e) {
		LocalVariable[] vars = captureMethodArguments(e.getNumArgs());
		visitEventStart(e);
		super.visitLdcInsn(e.getName());
		super.visitVarInsn(ALOAD, vars[e.getNumArgs()].getIndex());
		visitEventEnd(e);
		restoreMethodArguments(e.getNumArgs(), vars);
	}

	public void visitReturnInstead(EventData e) {
		Type oldType = getTopType();

		boxWithoutDup(oldType);

		visitEventStartWithSwap(e);
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);

		if (TypeUtil.isReference(oldType))
			super.visitTypeInsn(CHECKCAST, oldType.getInternalName());
		else
			unbox(oldType);
	}

	public void visitThrowInstead(EventData e) {
		Type oldType = getTopType();

		visitEventStartWithSwap(e);
		super.visitInsn(ACONST_NULL);
		visitEventEnd(e);

		super.visitTypeInsn(CHECKCAST, oldType.getInternalName());
	}

	public void visitMethodCallAfter(EventData e) {
		visitEventStart(e);
		super.visitLdcInsn(e.getName());
		super.visitVarInsn(ALOAD, generatedArgsArray);
		visitEventEnd(e);
	}

	public void visitReadInstead(EventData e) {
		Type oldType = Type.getType(e.getDesc());
		Type ownerType = Type.getObjectType(owner);
		String nextInnerClass = adapter.getNextInnerClass();
		String nextAccessor = adapter.getNextAccessor();

		InsteadActionGenerator action = new FieldReadGenerator(
				nextInnerClass,
				owner,
				name,
				desc,
				nextAccessor,
				"(" + ownerType.getDescriptor() + ")" + e.getDesc(),
				e.getName(),
				e.getDesc());

		adapter.addActionGenerator(action);

		newInsteadAction(action, ownerType);
		visitEventStartWithSwap(e);
		super.visitInsn(ACONST_NULL);
		visitEventEndWithAction(e);

		if (TypeUtil.isReference(oldType))
			super.visitTypeInsn(CHECKCAST, oldType.getInternalName());
		else
			unbox(oldType);

		shouldPreserveOriginal = false;
	}

	public void visitWriteInstead(EventData e) {
		Type oldType = Type.getType(e.getDesc());
		String nextInnerClass = adapter.getNextInnerClass();
		String nextAccessor = adapter.getNextAccessor();

		InsteadActionGenerator action = new FieldWriteGenerator(
				nextInnerClass,
				owner,
				name,
				desc,
				nextAccessor,
				TypeUtil.methodOf(Type.VOID_TYPE, ownerType, Type.getType(e.getDesc())),
				e.getName(),
				e.getDesc());

		adapter.addActionGenerator(action);

		boxWithoutDup(oldType);

		super.visitInsn(ICONST_1);
		super.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		super.visitInsn(DUP_X1);
		super.visitInsn(SWAP);
		super.visitInsn(ICONST_0);
		super.visitInsn(SWAP);
		super.visitInsn(AASTORE);

		int argsArray = captureLocal();

		newInsteadAction(action, ownerType);
		visitEventStartWithSwap(e);
		super.visitVarInsn(ALOAD, argsArray);
		visitEventEndWithAction(e);

		super.visitInsn(POP);

		shouldPreserveOriginal = false;
	}

	public void visitMethodCallInstead(EventData e) {
		Type retType = e.getRetType();
		String nextInnerClass = adapter.getNextInnerClass();

		String[] parts = e.getName().split(Pattern.quote("."));
		String methodName = parts[parts.length - 1];

		Pair<Integer, List<Type>> arrayAndTypes = captureArgumentsArray(e.getNumArgs());

		super.visitInsn(POP);

		InsteadActionGenerator action = new MethodCallGenerator(
				nextInnerClass,
				owner,
				name,
				desc,
				methodName,
				e.getDesc(),
				arrayAndTypes.getValue());

		adapter.addActionGenerator(action);

		newInsteadAction(action, ownerType);
		visitEventStartWithSwap(e);
		super.visitVarInsn(ALOAD, arrayAndTypes.getKey());
		visitEventEndWithAction(e);

		if (TypeUtil.isReference(retType))
			super.visitTypeInsn(CHECKCAST, retType.getInternalName());
		else
			unbox(retType);

		shouldPreserveOriginal = false;
	}

	public void visitInstanceInstead(EventData e) {
		Type retType = e.getRetType();
		String nextInnerClass = adapter.getNextInnerClass();

		Pair<Integer, List<Type>> arrayAndTypes = captureArgumentsArray(e.getNumArgs());

		super.visitInsn(POP2);

		InsteadActionGenerator action = new NewInstanceGenerator(
				nextInnerClass,
				owner,
				name,
				desc,
				retType.getInternalName(),
				e.getDesc(),
				arrayAndTypes.getValue());

		adapter.addActionGenerator(action);

		newInsteadAction(action, ownerType);
		visitEventStartWithSwap(e);
		super.visitVarInsn(ALOAD, arrayAndTypes.getKey());
		visitEventEndWithAction(e);

		super.visitTypeInsn(CHECKCAST, retType.getInternalName());

		shouldPreserveOriginal = false;
	}

	public void visitEventStart(EventData e) {
		super.visitFieldInsn(
				GETSTATIC,
				"com/mpraski/jmonitor/Resolver",
				e.getMonitor(),
				e.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassType : monitorClassType);
		super.visitTypeInsn(NEW, "com/mpraski/jmonitor/Event");
		super.visitInsn(DUP);

		if (e.getTag() == null)
			super.visitInsn(ACONST_NULL);
		else
			super.visitLdcInsn(e.getTag());

		super.visitFieldInsn(
				GETSTATIC,
				"com/mpraski/jmonitor/EventType",
				EventUtil.eventType(e.getType()),
				"Lcom/mpraski/jmonitor/EventType;");

		super.visitFieldInsn(
				GETSTATIC,
				"com/mpraski/jmonitor/EventOrder",
				EventUtil.eventOrder(e.getOrder()),
				"Lcom/mpraski/jmonitor/EventOrder;");

		if (source == null)
			super.visitInsn(ACONST_NULL);
		else
			super.visitLdcInsn(source);

		pushInt(line);
	}

	public void visitEventStartWithSwap(EventData e) {
		super.visitFieldInsn(
				GETSTATIC,
				"com/mpraski/jmonitor/Resolver",
				e.getMonitor(),
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

		super.visitFieldInsn(
				GETSTATIC,
				"com/mpraski/jmonitor/EventType",
				EventUtil.eventType(e.getType()),
				"Lcom/mpraski/jmonitor/EventType;");

		super.visitInsn(SWAP);

		super.visitFieldInsn(
				GETSTATIC,
				"com/mpraski/jmonitor/EventOrder",
				EventUtil.eventOrder(e.getOrder()),
				"Lcom/mpraski/jmonitor/EventOrder;");

		super.visitInsn(SWAP);

		if (source == null)
			super.visitInsn(ACONST_NULL);
		else
			super.visitLdcInsn(source);

		super.visitInsn(SWAP);

		pushInt(line);

		super.visitInsn(SWAP);
	}

	public void visitEventEnd(EventData e) {
		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitMethodInsn(
				INVOKEVIRTUAL,
				"java/lang/Thread",
				"getStackTrace",
				"()[Ljava/lang/StackTraceElement;",
				false);
		super.visitMethodInsn(
				INVOKESPECIAL,
				"com/mpraski/jmonitor/Event",
				"<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/EventType;Lcom/mpraski/jmonitor/EventOrder;Ljava/lang/String;ILjava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		if (e.getOrder() == EventOrder.INSTEAD)
			super.visitMethodInsn(
					INVOKEINTERFACE,
					insteadMonitorClass,
					insteadMonitorClassFunc,
					insteadMonitorClassFuncType,
					true);
		else
			super.visitMethodInsn(INVOKEINTERFACE, monitorClass, monitorClassFunc, monitorClassFuncType, true);
	}

	public void visitEventEndWithArg(EventData e, int localArg) {
		super.visitInsn(ICONST_1);
		super.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		super.visitInsn(DUP);
		super.visitInsn(ICONST_0);
		super.visitVarInsn(ALOAD, localArg);
		super.visitInsn(AASTORE);

		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitMethodInsn(
				INVOKEVIRTUAL,
				"java/lang/Thread",
				"getStackTrace",
				"()[Ljava/lang/StackTraceElement;",
				false);
		super.visitMethodInsn(
				INVOKESPECIAL,
				"com/mpraski/jmonitor/Event",
				"<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/EventType;Lcom/mpraski/jmonitor/EventOrder;Ljava/lang/String;ILjava/lang/Object;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		if (e.getOrder() == EventOrder.INSTEAD)
			super.visitMethodInsn(
					INVOKEINTERFACE,
					insteadMonitorClass,
					insteadMonitorClassFunc,
					insteadMonitorClassFuncType,
					true);
		else
			super.visitMethodInsn(INVOKEINTERFACE, monitorClass, monitorClassFunc, monitorClassFuncType, true);
	}

	public void visitEventEndWithAction(EventData e) {
		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitMethodInsn(
				INVOKEVIRTUAL,
				"java/lang/Thread",
				"getStackTrace",
				"()[Ljava/lang/StackTraceElement;",
				false);
		super.visitMethodInsn(
				INVOKESPECIAL,
				"com/mpraski/jmonitor/Event",
				"<init>",
				"(Ljava/lang/String;Lcom/mpraski/jmonitor/EventType;Lcom/mpraski/jmonitor/EventOrder;Ljava/lang/String;ILcom/mpraski/jmonitor/InsteadAction;[Ljava/lang/Object;[Ljava/lang/StackTraceElement;)V",
				false);
		super.visitMethodInsn(
				INVOKEINTERFACE,
				insteadMonitorClass,
				insteadMonitorClassFunc,
				insteadMonitorClassFuncType,
				true);
	}

	public void newInsteadAction(InsteadActionGenerator action, Type ownerType) {
		super.visitTypeInsn(NEW, action.getName());
		super.visitInsn(DUP);
		super.visitIntInsn(ALOAD, 0);
		super.visitMethodInsn(INVOKESPECIAL, action.getName(), "<init>", TypeUtil.constructorOf(ownerType), false);
	}

	public Object getTop() {
		if (stack.isEmpty())
			throw new IllegalStateException("Stack is empty");

		return stack.get(stack.size() - 1);
	}

	/*
	 * Creates a local variable holding boxed value on the top of the stack.
	 */
	private int captureLocal() {
		Type topType = getTopType();

		if (TypeUtil.takesTwoWords(topType))
			super.visitInsn(DUP2);
		else
			super.visitInsn(DUP);

		int l = sorter.newLocal(box(topType));
		super.visitVarInsn(ASTORE, l);

		return l;
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
			twoWords = TypeUtil.takesTwoWords(topType);

			if (twoWords)
				super.visitInsn(DUP2);
			else
				super.visitInsn(DUP);

			Pair<Integer, Integer> insns = TypeUtil.getLoadStoreInsns(topType);

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

	private Pair<Integer, List<Type>> captureArgumentsArray(int numArgs) {
		Type[] types = new Type[numArgs];

		pushInt(numArgs);
		super.visitTypeInsn(ANEWARRAY, "java/lang/Object");

		int methodArgs = sorter.newLocal(typeOfArray);
		super.visitVarInsn(ASTORE, methodArgs);

		Type topType;

		for (int i = numArgs - 1; i >= 0; i--) {
			topType = getTopType();

			types[i] = topType;

			super.visitVarInsn(ALOAD, methodArgs);

			if (TypeUtil.takesTwoWords(topType)) {
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
			} else {
				super.visitInsn(SWAP);
			}

			if (!TypeUtil.isReference(topType))
				boxWithoutDup(topType);

			pushInt(i);

			super.visitInsn(SWAP);
			super.visitInsn(AASTORE);
		}

		return new Pair<>(methodArgs, Arrays.asList(types));
	}

	/*
	 * Pushes the values of captured method arguments back onto the stack.
	 */
	private void restoreMethodArguments(int numArgs, LocalVariable[] localVars) {
		for (int i = 0; i < numArgs; i++)
			super.visitVarInsn(localVars[i].getLoadInsn(), localVars[i].getIndex());
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

		Pair<String, String> type = TypeUtil.getPrimitiveClass(desc);

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
}
