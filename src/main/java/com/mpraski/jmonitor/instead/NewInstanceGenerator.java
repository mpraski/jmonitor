package com.mpraski.jmonitor.instead;

import static com.mpraski.jmonitor.util.TypeUtil.isReference;
import static com.mpraski.jmonitor.util.TypeUtil.takesTwoWords;

import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.mpraski.jmonitor.util.InstrumentUtil;

public final class NewInstanceGenerator extends InsteadActionGenerator {

	private final String instanceClass;
	private final String instanceConstructor;
	private final List<Type> argTypes;

	public NewInstanceGenerator(
			String innerClass,
			String outerClass,
			String methodName,
			String methodDesc,
			String instanceClass,
			String instanceConstructor,
			List<Type> argTypes) {
		super(innerClass, outerClass, methodName, methodDesc);
		this.instanceClass = instanceClass;
		this.instanceConstructor = instanceConstructor;
		this.argTypes = argTypes;
	}

	@Override
	public byte[] generate() {
		final String this0 = "this$0";
		final String innerDesc = Type.getObjectType(name).getDescriptor();
		final String outerDesc = Type.getObjectType(outerClass).getDescriptor();

		ClassWriter classWriter = new ClassWriter(0);
		FieldVisitor fieldVisitor;
		MethodVisitor methodVisitor;

		classWriter.visit(V1_8, ACC_SUPER, name, null, "java/lang/Object", insteadInterface);

		classWriter.visitOuterClass(outerClass, methodName, methodDesc);
		classWriter.visitInnerClass(name, outerClass, simpleName, 0);

		fieldVisitor = classWriter.visitField(ACC_FINAL | ACC_SYNTHETIC, this0, outerDesc, null, null);
		fieldVisitor.visitEnd();

		{
			methodVisitor = classWriter.visitMethod(0, "<init>", "(" + outerDesc + ")V", null, null);
			methodVisitor.visitCode();
			Label l0 = new Label();
			methodVisitor.visitLabel(l0);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitVarInsn(ALOAD, 1);
			methodVisitor.visitFieldInsn(PUTFIELD, name, this0, outerDesc);
			Label l1 = new Label();
			methodVisitor.visitLabel(l1);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			methodVisitor.visitInsn(RETURN);
			Label l2 = new Label();
			methodVisitor.visitLabel(l2);
			methodVisitor.visitLocalVariable("this", innerDesc, null, l0, l2, 0);
			methodVisitor.visitMaxs(2, 2);
			methodVisitor.visitEnd();
		}

		{
			methodVisitor = classWriter
					.visitMethod(ACC_PUBLIC, "doAction", "([Ljava/lang/Object;)Ljava/lang/Object;", null, null);
			methodVisitor.visitCode();

			Label l0 = new Label();
			methodVisitor.visitLabel(l0);
			methodVisitor.visitTypeInsn(NEW, instanceClass);
			methodVisitor.visitInsn(DUP);

			Type type;
			int maxStack = 3;
			for (int i = 0; i < argTypes.size(); i++) {
				type = argTypes.get(i);
				maxStack += takesTwoWords(type) ? 2 : 1;

				methodVisitor.visitVarInsn(ALOAD, 1);
				InstrumentUtil.pushInt(methodVisitor, i);
				methodVisitor.visitInsn(AALOAD);

				if (isReference(type))
					methodVisitor.visitTypeInsn(CHECKCAST, type.getInternalName());
				else
					InstrumentUtil.unbox(methodVisitor, type);
			}

			methodVisitor.visitMethodInsn(INVOKESPECIAL, instanceClass, "<init>", instanceConstructor, false);
			methodVisitor.visitInsn(ARETURN);

			Label l1 = new Label();
			methodVisitor.visitLabel(l1);
			methodVisitor.visitLocalVariable("this", innerDesc, null, l0, l1, 0);
			methodVisitor.visitLocalVariable("arguments", "[Ljava/lang/Object;", null, l0, l1, 1);
			methodVisitor.visitMaxs(maxStack, 2);
			methodVisitor.visitEnd();
		}

		classWriter.visitEnd();

		return classWriter.toByteArray();
	}

	@Override
	public boolean modifiesOuterClass() {
		return false;
	}

	@Override
	public void modifyOuterClass(ClassVisitor cv) {
		throw new UnsupportedOperationException();
	}

}
