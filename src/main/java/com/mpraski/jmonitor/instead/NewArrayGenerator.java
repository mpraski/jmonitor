package com.mpraski.jmonitor.instead;

import org.objectweb.asm.ClassVisitor;

import com.mpraski.jmonitor.adapters.MonitorClassAdapter;

public final class NewArrayGenerator extends InsteadActionGenerator {

	public NewArrayGenerator(String innerClass, String outerClass, String methodName, String methodDesc) {
		super(innerClass, outerClass, methodName, methodDesc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public byte[] generate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean modifiesOuterClass() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void modifyOuterClass(ClassVisitor cv) {
		throw new UnsupportedOperationException();
	}

}
