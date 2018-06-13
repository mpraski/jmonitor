package com.mpraski.jmonitor.instead;

import com.mpraski.jmonitor.adapters.MonitorClassAdapter;

public final class MethodCallGenerator extends InsteadActionGenerator {

	public MethodCallGenerator(String innerClass, String outerClass, String methodName, String methodDesc) {
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
		return false;
	}

	@Override
	public void modifyOuterClass(MonitorClassAdapter adapter) {
		throw new UnsupportedOperationException();
	}

}
