package com.mpraski.jmonitor.instead;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class FieldReadGenerator extends InsteadActionGenerator {

	public FieldReadGenerator(String innerClass, String outerClass) {
		super(innerClass, outerClass);
		// TODO Auto-generated constructor stub
	}

	@Override
	public byte[] generate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean modifiesOuterClass() {
		return true;
	}

	@Override
	public void modifyOuterClass(ClassVisitor cv) {
		// TODO Auto-generated method stub
	}

}
