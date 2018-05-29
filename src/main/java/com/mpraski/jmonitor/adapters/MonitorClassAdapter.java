package com.mpraski.jmonitor.adapters;

import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import com.mpraski.jmonitor.pattern.EventPatternMatcher;

public class MonitorClassAdapter extends ClassVisitor {
	// matchers
	private final List<EventPatternMatcher> matchers;
	private String owner;

	public MonitorClassAdapter(int api, List<EventPatternMatcher> matchers) {
		super(api);
		this.matchers = matchers;
	}

	public MonitorClassAdapter(int api, ClassVisitor classVisitor, List<EventPatternMatcher> matchers) {
		super(api, classVisitor);
		this.matchers = matchers;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.owner = name;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
		if (mv != null) {
			mv = new MonitorMethodAdapter(api, owner, access, name, desc, mv, matchers);
		}
		return mv;
	}

}
