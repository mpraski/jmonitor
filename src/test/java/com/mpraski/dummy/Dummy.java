package com.mpraski.dummy;

public class Dummy {
	private String lel = "lel";
	private int lilp = 1;
	private int gosh = 2;

	public static void main(String[] args) {
		Dummy d = new Dummy();
		System.out.println(d.lol());
		System.out.println("Now printing...");
		System.out.println(d.lil());

		d.setLol();
	}

	public void sayHello() {
		System.out.println("hello");
	}

	public String lol() {
		return lel;
	}

	public Integer lil() {
		Object[] luls = new Object[32768];
		return lilp;
	}

	public void setLol() {
		this.lel = "lulu";
	}

	public void setLis(int f) {
		this.lilp = Integer.parseInt("3");
	}
}
