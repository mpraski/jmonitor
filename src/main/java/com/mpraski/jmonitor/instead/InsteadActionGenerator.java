package com.mpraski.jmonitor.instead;

import java.util.regex.Pattern;

import org.objectweb.asm.Opcodes;

import com.mpraski.jmonitor.adapters.MonitorClassAdapter;

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
	protected final String innerClass, outerClass, methodName, methodDesc, name, simpleName;
	protected final static String[] insteadInterface = new String[] { "com/mpraski/jmonitor/InsteadAction" };

	public InsteadActionGenerator(String innerClass, String outerClass, String methodName, String methodDesc) {
		this.innerClass = innerClass;
		this.outerClass = outerClass;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		this.name = outerClass + '$' + innerClass;
		String[] parts = name.split(Pattern.quote("/"));
		this.simpleName = parts[parts.length - 1];
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

	public String getMethodName() {
		return methodName;
	}

	public String getMethodDesc() {
		return methodDesc;
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
