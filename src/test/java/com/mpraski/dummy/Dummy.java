package com.mpraski.dummy;

public class Dummy {
	private String lel = "oh my my";
	private int lilp = 1;
	private int gosh = 2;
	private double mumbo = 123.545d;
	private long jumbo = 1243647345l;

	private static String wop = "wop";

	public static void main(String[] args) {
		Dummy d = new Dummy();
		d.lol();
		d.lil();
		d.setLol();
		d.writeInt();
		d.writeMumbo();
		d.writeJumbo();
		d.readWop();
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

	public int writeInt() {
		return gosh;
	}

	public double writeMumbo() {
		return mumbo;
	}

	public long writeJumbo() {
		return jumbo;
	}
}
