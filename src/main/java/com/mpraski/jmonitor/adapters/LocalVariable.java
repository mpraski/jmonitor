package com.mpraski.jmonitor.adapters;

public final class LocalVariable {

	private final int index, loadInsn;

	public LocalVariable(int index, int loadInsn) {
		this.index = index;
		this.loadInsn = loadInsn;
	}

	public int getIndex() {
		return index;
	}

	public int getLoadInsn() {
		return loadInsn;
	}

	@Override
	public String toString() {
		return "<" + index + ":" + loadInsn + ">";
	}

}
