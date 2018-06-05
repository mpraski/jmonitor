package com.mpraski.dummy;

public class Dummy {
	private String lel = "lel";
	private int lilp = 1;
	private int gosh = 2;

	private static String wop = "wop";

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

	public String readWop() {
		return wop;
	}

	public void writeWop(String s) {
		wop = s;
	}
}
