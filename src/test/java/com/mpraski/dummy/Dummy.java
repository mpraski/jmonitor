package com.mpraski.dummy;

public class Dummy {
	private String lel = "lel";
	private int lilp = 1;

	public static void main(String[] args) {
		Dummy d = new Dummy();
		System.out.println(d.lol());
		System.out.println("Now printing...");
		System.out.println(d.lil());
	}

	public void sayHello() {
		System.out.println("hello");
	}

	public String lol() {
		return lel;
	}

	public Integer lil() {
		return lilp;
	}
}
