package com.mpraski.jmonitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/*
 * Used to generate inner classes - instances of InsteadAction which execute 
 * instrumented code during the call to passThrough methods of Event class.
 */
public final class InsteadActionWriter implements Opcodes {
	public InsteadActionWriter() {

	}

	public MethodVisitor getMethodVisitor() {
		return null;
	}

	public static byte[] write() {
		return null;
	}
}
