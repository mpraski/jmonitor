package com.mpraski.jmonitor.instead;

import com.mpraski.jmonitor.adapters.MonitorClassAdapter;

public final class FieldWriteGenerator extends InsteadActionGenerator {

	public FieldWriteGenerator(String innerClass, String outerClass) {
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void modifyOuterClass(MonitorClassAdapter adapter) {
		// TODO Auto-generated method stub

	}

}
