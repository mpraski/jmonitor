package com.mpraski.jmonitor;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/*
 * Used to generate inner classes - instances of InsteadAction which execute 
 * instrumented code during the call to passThrough methods of Event class.
 */
public final class InsteadActionWriter implements Opcodes {
	public static byte[] write(ClassWriter cw) {
		return null;
	}
}
