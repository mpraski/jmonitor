package com.mpraski.jmonitor.util;

import static com.mpraski.jmonitor.util.Constants.CLASS_DOUBLE;
import static com.mpraski.jmonitor.util.Constants.CLASS_FLOAT;
import static com.mpraski.jmonitor.util.Constants.CLASS_INTEGER;
import static com.mpraski.jmonitor.util.Constants.CLASS_LONG;
import static com.mpraski.jmonitor.util.TypeUtil.getPrimitiveClass;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class InstrumentUtil implements Opcodes {

	/*
	 * Attempts to produce a boxed value from descriptor of type of the value on top
	 * of the stack.
	 */
	public static void box(MethodVisitor mv, String desc) {
		if (desc.length() > 1)
			return;

		Pair<String, String> type = getPrimitiveClass(desc);

		mv.visitMethodInsn(INVOKESTATIC, type.getKey(), "valueOf", type.getValue(), false);
	}

	public static void unbox(MethodVisitor mv, Type type) {
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

	public static int getReturnInsn(String desc) {
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

	public static void pushInt(MethodVisitor mv, int i) {
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
}
