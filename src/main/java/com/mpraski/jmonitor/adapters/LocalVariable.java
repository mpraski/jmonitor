package com.mpraski.jmonitor.adapters;

public final class LocalVariable {
	public LocalVariable(int index, int loadInsn) {
		super();
		this.index = index;
		this.loadInsn = loadInsn;
	}

	private final int index, loadInsn;

	public int getIndex() {
		return index;
	}

	public int getLoadInsn() {
		return loadInsn;
	}
}