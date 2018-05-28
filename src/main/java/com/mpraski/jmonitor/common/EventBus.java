package com.mpraski.jmonitor.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import com.mpraski.jmonitor.event.Event;

public final class EventBus {
	private final BlockingQueue<Event> toDispatch = new LinkedBlockingQueue<>();
	private final Map<String, List<Monitor>> subscribers = new ConcurrentHashMap<>();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private volatile boolean running;

	public void startDispatching() {
		running = true;
		executor.submit(new Dispatcher());
	}

	public void stopDispatching() {
		running = false;
		executor.shutdown();
	}

	public void dispatch(Event event) {
		toDispatch.offer(event);
	}

	public void subscribe(String tag, Monitor monitor) {
		List<Monitor> receivers = getReceivers(tag);

		synchronized (receivers) {
			receivers.add(monitor);
		}
	}

	public List<Monitor> getReceivers(String tag) {
		return subscribers.computeIfAbsent(tag, k -> Collections.synchronizedList(new ArrayList<Monitor>()));
	}

	private final class Dispatcher implements Runnable {

		@Override
		public void run() {
			try {
				while (running) {
					Event event = toDispatch.take();
					List<Monitor> receivers = getReceivers(event.getTag());

					synchronized (receivers) {
						for (Monitor m : receivers) {
							if (m != null)
								m.onEvent(event);
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
}
