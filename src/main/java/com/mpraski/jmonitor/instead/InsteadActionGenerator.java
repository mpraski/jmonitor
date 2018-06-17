package com.mpraski.jmonitor.instead;

import static com.mpraski.jmonitor.util.Constants.CLASS_DOUBLE;
import static com.mpraski.jmonitor.util.Constants.CLASS_FLOAT;
import static com.mpraski.jmonitor.util.Constants.CLASS_INTEGER;
import static com.mpraski.jmonitor.util.Constants.CLASS_LONG;
import static com.mpraski.jmonitor.util.Utils.getPrimitiveClass;

import java.util.regex.Pattern;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.mpraski.jmonitor.adapters.MonitorClassAdapter;
import com.mpraski.jmonitor.util.Pair;

/*
 * Used to generate inner classes - instances of InsteadAction which execute 
 * instrumented code during the call to passThrough methods of Event class.
 * Four scenarios are to be expected:
 * 1. Field/static read - no args, returns read value
 * 2. Field/static write - args[0] gets written, return null
 * 3. Method call - passes arguments, return result (if any)
 * 4. New instance/array - constructor called with args, or args[0] - array size, rest - contents. Returns created instance.
 */
public abstract class InsteadActionGenerator implements Opcodes {
	protected final String outerClass, methodName, methodDesc, name, simpleName, internalName;
	protected final static String[] insteadInterface = new String[] { "com/mpraski/jmonitor/InsteadAction" };

	public InsteadActionGenerator(String innerClass, String outerClass, String methodName, String methodDesc) {
		this.outerClass = outerClass;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		this.name = outerClass + '$' + innerClass;
		String[] parts = name.split(Pattern.quote("/"));
		this.simpleName = parts[parts.length - 1];
		this.internalName = name.replace('/', '.');
	}

	public String getInternalName() {
		return internalName;
	}

	public String getName() {
		return name;
	}

	public String getSimpleName() {
		return simpleName;
	}

	public String getOuterName() {
		return outerClass;
	}

	protected static void box(MethodVisitor mv, String desc) {
		if (desc.length() > 1)
			return;

		Pair<String, String> type = getPrimitiveClass(desc);

		mv.visitMethodInsn(INVOKESTATIC, type.getKey(), "valueOf", type.getValue(), false);
	}

	protected static void unbox(MethodVisitor mv, Type type) {
		if (type.equals(Type.LONG_TYPE)) {
			mv.visitTypeInsn(CHECKCAST, CLASS_LONG.getKey());
			mv.visitMethodInsn(INVOKEVIRTUAL, CLASS_LONG.getKey(), "longValue", "()J", false);
		} else if (type.equals(Type.DOUBLE_TYPE)) {
			mv.visitTypeInsn(CHECKCAST, CLASS_DOUBLE.getKey());
			mv.visitMethodInsn(INVOKEVIRTUAL, CLASS_DOUBLE.getKey(), "doubleValue", "()D", false);
		} else if (type.equals(Type.INT_TYPE)) {
			mv.visitTypeInsn(CHECKCAST, CLASS_INTEGER.getKey());
			mv.visitMethodInsn(INVOKEVIRTUAL, CLASS_INTEGER.getKey(), "intValue", "()I", false);
		} else if (type.equals(Type.FLOAT_TYPE)) {
			mv.visitTypeInsn(CHECKCAST, CLASS_FLOAT.getKey());
			mv.visitMethodInsn(INVOKEVIRTUAL, CLASS_FLOAT.getKey(), "floatValue", "()F", false);
		}
	}

	protected static int getReturnInsn(String desc) {
		if (desc.length() > 1)
			return ARETURN;

		switch (desc) {
		case "I":
		case "Z":
		case "C":
		case "B":
		case "S":
			return IRETURN;
		case "F":
			return FRETURN;
		case "D":
			return DRETURN;
		case "J":
			return LRETURN;
		}

		throw new IllegalStateException("Unidentified primitive: " + desc);
	}

	protected static void pushInt(MethodVisitor mv, int i) {
		if (i < 6) {
			switch (i) {
			case 0:
				mv.visitInsn(ICONST_0);
				break;
			case 1:
				mv.visitInsn(ICONST_1);
				break;
			case 2:
				mv.visitInsn(ICONST_2);
				break;
			case 3:
				mv.visitInsn(ICONST_3);
				break;
			case 4:
				mv.visitInsn(ICONST_4);
				break;
			case 5:
				mv.visitInsn(ICONST_5);
				break;
			}
		} else if (i > 5 && i < 128) {
			mv.visitIntInsn(BIPUSH, i);
		} else if (i > 127 && i < 32768) {
			mv.visitIntInsn(SIPUSH, i);
		} else {
			mv.visitLdcInsn(i);
		}
	}

	/*
	 * Generate the inner class representing the InsteadAction instance.
	 */
	public abstract byte[] generate();

	/*
	 * Field read and write scenarios require auxiliary 'accessor' static method to
	 * be added to outer class.
	 */
	public abstract boolean modifiesOuterClass();

	/*
	 * While the class is still being instrumented, add appropriate code.
	 */
	public abstract void modifyOuterClass(ClassVisitor cv);
}
