package com.mpraski.jmonitor.instead;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import com.mpraski.jmonitor.adapters.MonitorClassAdapter;

public final class FieldReadGenerator extends InsteadActionGenerator {

	public FieldReadGenerator(String innerClass, String outerClass, String methodName, String methodDesc) {
		super(innerClass, outerClass, methodName, methodDesc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public byte[] generate() {
		ClassWriter classWriter = new ClassWriter(0);
		FieldVisitor fieldVisitor;
		MethodVisitor methodVisitor;

		classWriter.visit(V1_8, ACC_SUPER, name, null, "java/lang/Object", insteadInterface);

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean modifiesOuterClass() {
		return true;
	}

	@Override
	public void modifyOuterClass(MonitorClassAdapter adapter) {
		// TODO Auto-generated method stub
	}

}
