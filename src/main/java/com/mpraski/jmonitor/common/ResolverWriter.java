package com.mpraski.jmonitor.common;

import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.mpraski.jmonitor.pattern.EventMonitor;
import com.mpraski.jmonitor.pattern.EventOrder;

public class ResolverWriter implements Opcodes {
	private static final String monitorClass = "Lcom/mpraski/jmonitor/common/Monitor;";
	private static final String insteadMonitorClass = "Lcom/mpraski/jmonitor/common/InsteadMonitor;";

	public static byte[] write(List<EventMonitor> monitors) {

		ClassWriter classWriter = new ClassWriter(0);
		FieldVisitor fieldVisitor;
		MethodVisitor methodVisitor;

		classWriter.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, "com/mpraski/jmonitor/common/Resolver", null,
				"java/lang/Object", null);

		classWriter.visitSource("Resolver.java", null);

		{
			methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			methodVisitor.visitCode();
			Label label0 = new Label();
			methodVisitor.visitLabel(label0);
			methodVisitor.visitLineNumber(3, label0);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			methodVisitor.visitInsn(RETURN);
			methodVisitor.visitMaxs(1, 1);
			methodVisitor.visitEnd();
		}

		String mClass;
		String fieldName;
		String internalName;

		for (EventMonitor m : monitors) {
			fieldName = m.getFieldName();
			mClass = m.getOrder() == EventOrder.INSTEAD ? insteadMonitorClass : monitorClass;

			fieldVisitor = classWriter.visitField(ACC_PUBLIC | ACC_FINAL | ACC_STATIC, fieldName, mClass, null, null);
			fieldVisitor.visitEnd();

		}

		methodVisitor = classWriter.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		methodVisitor.visitCode();

		for (EventMonitor m : monitors) {
			mClass = m.getOrder() == EventOrder.INSTEAD ? insteadMonitorClass : monitorClass;
			internalName = toInternalType(m.getMonitor());
			fieldName = m.getFieldName();

			methodVisitor.visitTypeInsn(NEW, internalName);
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitMethodInsn(INVOKESPECIAL, internalName, "<init>", "()V", false);
			methodVisitor.visitFieldInsn(PUTSTATIC, "com/mpraski/jmonitor/common/Resolver", fieldName, mClass);
		}

		methodVisitor.visitInsn(RETURN);
		methodVisitor.visitMaxs(2, 0);
		methodVisitor.visitEnd();

		classWriter.visitEnd();

		return classWriter.toByteArray();
	}

	private static String toInternalType(String name) {
		return name.replaceAll("[.]", "/");
	}
}