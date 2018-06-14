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

import com.mpraski.jmonitor.EventOrder;
import com.mpraski.jmonitor.EventType;

public final class Utils {

	public static String constructorOf(Type type) {
		return "(" + type.getDescriptor() + ")V";
	}

	public static boolean isReference(Type type) {
		return type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;
	}

	public static Pair<Integer, Integer> getPrimitiveInsns(Type t) {
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

	public static String eventType(EventType type) {
		String s = null;

		switch (type) {
		case FIELD_READ:
			s = "FIELD_READ";
			break;
		case FIELD_WRITE:
			s = "FIELD_WRITE";
			break;
		case FIELD_READ_STATIC:
			s = "FIELD_READ_STATIC";
			break;
		case FIELD_WRITE_STATIC:
			s = "FIELD_WRITE_STATIC";
			break;
		case METHOD_CALL:
			s = "METHOD_CALL";
			break;
		case RETURN:
			s = "RETURN";
			break;
		case THROW:
			s = "THROW";
			break;
		case INSTANCE:
			s = "INSTANCE";
			break;
		case INSTANCE_ARRAY:
			s = "INSTANCE_ARRAY";
			break;
		case MONITOR_ENTER:
			s = "MONITOR_ENTER";
			break;
		case MONITOR_EXIT:
			s = "MONITOR_EXIT";
			break;
		case ANY:
			s = "ANY";
			break;
		case AND:
			s = "AND";
			break;
		case OR:
			s = "OR";
			break;
		case NOT:
			s = "NOT";
			break;
		}

		return s;
	}

	public static String eventOrder(EventOrder order) {
		String s = null;

		switch (order) {
		case BEFORE:
			s = "BEFORE";
			break;
		case AFTER:
			s = "AFTER";
			break;
		case INSTEAD:
			s = "INSTEAD";
			break;
		}

		return s;
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
