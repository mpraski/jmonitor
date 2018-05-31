package com.mpraski.jmonitor.util;

import com.mpraski.jmonitor.event.EventType;

public final class ToString {
	public static String eventType(EventType type) {
		String s = null;

		switch (type) {
		case FIELD_READ:
			s = "FIELD_READ";
			break;
		case FIELD_WRITE:
			s = "FIELD_WRITE";
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
