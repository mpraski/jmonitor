package com.mpraski.jmonitor.util;

import static com.mpraski.jmonitor.util.Constants.CLASS_BOOLEAN;
import static com.mpraski.jmonitor.util.Constants.CLASS_BYTE;
import static com.mpraski.jmonitor.util.Constants.CLASS_CHARACTER;
import static com.mpraski.jmonitor.util.Constants.CLASS_DOUBLE;
import static com.mpraski.jmonitor.util.Constants.CLASS_FLOAT;
import static com.mpraski.jmonitor.util.Constants.CLASS_INTEGER;
import static com.mpraski.jmonitor.util.Constants.CLASS_LONG;
import static com.mpraski.jmonitor.util.Constants.ClASS_SHORT;
import static com.mpraski.jmonitor.util.Constants.INSNS_DOUBLE;
import static com.mpraski.jmonitor.util.Constants.INSNS_FLOAT;
import static com.mpraski.jmonitor.util.Constants.INSNS_INT;
import static com.mpraski.jmonitor.util.Constants.INSNS_LONG;
import static com.mpraski.jmonitor.util.Constants.INSNS_REF;

import org.objectweb.asm.Type;

public final class TypeUtil {

	public static String constructorOf(Type type) {
		return "(" + type.getDescriptor() + ")V";
	}

	public static boolean isReference(Type type) {
		return type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;
	}

	public static Pair<Integer, Integer> getLoadStoreInsns(Type t) {
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

	public static String toDots(String s) {
		return s.replace('/', '.');
	}

	public static boolean takesTwoWords(Type type) {
		return type.equals(Type.LONG_TYPE) || type.equals(Type.DOUBLE_TYPE);
	}

	public static Pair<String, String> getPrimitiveClass(String desc) {
		Pair<String, String> type = null;

		switch (desc) {
		case "Z":
			type = CLASS_BOOLEAN;
			break;
		case "C":
			type = CLASS_CHARACTER;
			break;
		case "B":
			type = CLASS_BYTE;
			break;
		case "S":
			type = ClASS_SHORT;
			break;
		case "I":
			type = CLASS_INTEGER;
			break;
		case "F":
			type = CLASS_FLOAT;
			break;
		case "J":
			type = CLASS_LONG;
			break;
		case "D":
			type = CLASS_DOUBLE;
			break;
		}

		return type;
	}
}
