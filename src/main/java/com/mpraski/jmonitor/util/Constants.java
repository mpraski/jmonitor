package com.mpraski.jmonitor.util;

import com.mpraski.jmonitor.event.EventType;

public final class Constants {
	public static final Pair<String, String> BOOLEAN = new Pair<>("java/lang/Boolean", "(Z)Ljava/lang/Boolean;");
	public static final Pair<String, String> CHARACTER = new Pair<>("java/lang/Character", "(C)Ljava/lang/Character;");
	public static final Pair<String, String> BYTE = new Pair<>("java/lang/Byte", "(B)Ljava/lang/Byte;");
	public static final Pair<String, String> SHORT = new Pair<>("java/lang/Short", "(S)Ljava/lang/Short;");
	public static final Pair<String, String> INTEGER = new Pair<>("java/lang/Integer", "(I)Ljava/lang/Integer;");
	public static final Pair<String, String> FLOAT = new Pair<>("java/lang/Float", "(I)Ljava/lang/Float;");
	public static final Pair<String, String> LONG = new Pair<>("java/lang/Long", "(J)Ljava/lang/Long;");
	public static final Pair<String, String> DOUBLE = new Pair<>("java/lang/Double", "(D)Ljava/lang/Double;");

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
}
