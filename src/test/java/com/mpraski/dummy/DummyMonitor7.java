package com.mpraski.dummy;

import java.math.BigInteger;

import com.mpraski.jmonitor.Event;
import com.mpraski.jmonitor.InsteadMonitor;

public class DummyMonitor7 implements InsteadMonitor {

	@Override
	public Object doInstead(Event event) {
		System.out.println("DummyMonitor7 :: " + event);

		return new BigInteger("666");
	}

}
