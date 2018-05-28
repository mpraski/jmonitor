package com.mpraski.jmonitor.util;

public final class LocalVariable {
	private final String name, description, signature;

	public LocalVariable(String name, String description, String signature) {
		this.name = name;
		this.description = description;
		this.signature = signature;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getSignature() {
		return signature;
	}
}
