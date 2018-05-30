package com.mpraski.jmonitor.common;

import com.mpraski.jmonitor.event.Event;
import com.mpraski.jmonitor.event.EventType;

public class Example {
	private String lel = "lel";
	private int lilp = 1;

	public void sayHello() {
		System.out.println("hello");
	}

	public void composeEvent() {
		Event e = new Event("tag", EventType.FIELD_READ, "I", new Integer(1), null,
				Thread.currentThread().getStackTrace());
	}

	public String lol() {
		return lel;
	}

	public Integer lil() {
		return lilp;
	}
}
