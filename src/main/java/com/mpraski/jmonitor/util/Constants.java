package com.mpraski.jmonitor.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.mpraski.jmonitor.Event;

public final class Constants implements Opcodes {
	public static final String monitorClassType = "Lcom/mpraski/jmonitor/Monitor;";
	public static final String insteadMonitorClassType = "Lcom/mpraski/jmonitor/InsteadMonitor;";
	public static final String monitorClass = "com/mpraski/jmonitor/Monitor";
	public static final String insteadMonitorClass = "com/mpraski/jmonitor/InsteadMonitor";
	public static final String monitorClassFunc = "onEvent";
	public static final String insteadMonitorClassFunc = "doInstead";
	public static final String monitorClassFuncType = "(Lcom/mpraski/jmonitor/Event;)V";
	public static final String insteadMonitorClassFuncType = "(Lcom/mpraski/jmonitor/Event;)Ljava/lang/Object;";

	public static final Type typeOfEvent = Type.getType(Event.class);
	public static final Type typeOfArray = Type.getType(Object[].class);
	public static final Type typeOfInteger = Type.getType(Integer.class);
	public static final Type typeOfFloat = Type.getType(Float.class);
	public static final Type typeOfLong = Type.getType(Long.class);
	public static final Type typeOfDouble = Type.getType(Double.class);

	public static final Pair<String, String> CLASS_BOOLEAN = new Pair<>("java/lang/Boolean", "(Z)Ljava/lang/Boolean;");
	public static final Pair<String, String> CLASS_CHARACTER = new Pair<>("java/lang/Character",
			"(C)Ljava/lang/Character;");
	public static final Pair<String, String> CLASS_BYTE = new Pair<>("java/lang/Byte", "(B)Ljava/lang/Byte;");
	public static final Pair<String, String> ClASS_SHORT = new Pair<>("java/lang/Short", "(S)Ljava/lang/Short;");
	public static final Pair<String, String> CLASS_INTEGER = new Pair<>("java/lang/Integer", "(I)Ljava/lang/Integer;");
	public static final Pair<String, String> CLASS_FLOAT = new Pair<>("java/lang/Float", "(F)Ljava/lang/Float;");
	public static final Pair<String, String> CLASS_LONG = new Pair<>("java/lang/Long", "(J)Ljava/lang/Long;");
	public static final Pair<String, String> CLASS_DOUBLE = new Pair<>("java/lang/Double", "(D)Ljava/lang/Double;");

	public static final Pair<Integer, Integer> INSNS_REF = new Pair<>(ASTORE, ALOAD);
	public static final Pair<Integer, Integer> INSNS_INT = new Pair<>(ISTORE, ILOAD);
	public static final Pair<Integer, Integer> INSNS_FLOAT = new Pair<>(FSTORE, FLOAD);
	public static final Pair<Integer, Integer> INSNS_DOUBLE = new Pair<>(DSTORE, DLOAD);
	public static final Pair<Integer, Integer> INSNS_LONG = new Pair<>(LSTORE, LLOAD);
}
