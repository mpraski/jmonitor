package com.mpraski.jmonitor.instead;

import static com.mpraski.jmonitor.util.Constants.getPrimitiveClass;

import java.util.regex.Pattern;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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
	public abstract void modifyOuterClass(MonitorClassAdapter adapter);
}
