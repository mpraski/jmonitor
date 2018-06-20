package com.mpraski.jmonitor.adapters;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AnalyzerAdapter;

public class InstrumentationMethodAdapter extends AnalyzerAdapter implements Opcodes {

	public InstrumentationMethodAdapter(String owner, int access, String name, String descriptor,
			MethodVisitor methodVisitor) {
		super(owner, access, name, descriptor, methodVisitor);
		// TODO Auto-generated constructor stub
	}

}
