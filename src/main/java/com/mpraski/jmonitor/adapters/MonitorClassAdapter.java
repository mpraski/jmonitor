package com.mpraski.jmonitor.adapters;

import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import com.mpraski.jmonitor.pattern.EventPatternMatcher;

public class MonitorClassAdapter extends ClassVisitor {
	private final Set<EventPatternMatcher> matchers;

	public MonitorClassAdapter(int api, Set<EventPatternMatcher> matchers) {
		super(api);
		this.matchers = matchers;
	}

	public MonitorClassAdapter(int api, ClassVisitor classVisitor, Set<EventPatternMatcher> matchers) {
		super(api, classVisitor);
		this.matchers = matchers;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
		if (mv != null) {
			// mv = new MonitorMethodAdapter(api, mv, matchers);
		}
		return mv;
	}

}
