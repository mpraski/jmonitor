package com.mpraski.jmonitor.instead;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

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
	protected final String innerClass, outerClass;

	public InsteadActionGenerator(String innerClass, String outerClass) {
		this.innerClass = innerClass;
		this.outerClass = outerClass;
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
