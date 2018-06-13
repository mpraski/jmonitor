package com.mpraski.jmonitor;

import static com.mpraski.jmonitor.util.Constants.insteadMonitorClassType;
import static com.mpraski.jmonitor.util.Constants.monitorClassType;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ResolverGenerator implements Opcodes {

	public static byte[] generate(Set<EventMonitor> monitors) {
		Set<String> processed = new HashSet<>(monitors.size());

		ClassWriter classWriter = new ClassWriter(0);
		FieldVisitor fieldVisitor;
		MethodVisitor methodVisitor;

		classWriter.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, "com/mpraski/jmonitor/Resolver", null,
				"java/lang/Object", null);

		classWriter.visitSource("Resolver.java", null);

		methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		methodVisitor.visitCode();
		methodVisitor.visitVarInsn(ALOAD, 0);
		methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		methodVisitor.visitInsn(RETURN);
		methodVisitor.visitMaxs(1, 1);
		methodVisitor.visitEnd();

		String mClass;
		String fieldName;
		String internalName;

		for (EventMonitor m : monitors) {
			if (processed.contains(m.getMonitor()))
				continue;
			processed.add(m.getMonitor());

			fieldName = m.getFieldName();
			mClass = m.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassType : monitorClassType;

			fieldVisitor = classWriter.visitField(ACC_PUBLIC | ACC_FINAL | ACC_STATIC, fieldName, mClass, null, null);
			fieldVisitor.visitEnd();
		}

		methodVisitor = classWriter.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		methodVisitor.visitCode();

		processed.clear();

		for (EventMonitor m : monitors) {
			if (processed.contains(m.getMonitor()))
				continue;
			processed.add(m.getMonitor());

			mClass = m.getOrder() == EventOrder.INSTEAD ? insteadMonitorClassType : monitorClassType;
			internalName = toInternalType(m.getMonitor());
			fieldName = m.getFieldName();

			methodVisitor.visitTypeInsn(NEW, internalName);
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitMethodInsn(INVOKESPECIAL, internalName, "<init>", "()V", false);
			methodVisitor.visitFieldInsn(PUTSTATIC, "com/mpraski/jmonitor/Resolver", fieldName, mClass);
		}

		methodVisitor.visitInsn(RETURN);
		methodVisitor.visitMaxs(2, 0);
		methodVisitor.visitEnd();

		classWriter.visitEnd();

		return classWriter.toByteArray();
	}

	private static String toInternalType(String name) {
		return name.replace('.', '/');
	}
}