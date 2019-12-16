package fr.inria.diverse.maven.resolver.processor.testfinder;

import java.util.ArrayDeque;
import java.util.Date;
import java.util.Queue;

public class TimeCredit {
	Queue<Long> fifo = new ArrayDeque<>();
	long windowLength = 60 * 60 * 1000; // 1h
	int max = 5000;

	public TimeCredit(long windowLength, int max) {
		this.max = max;
		this.windowLength = windowLength;
	}

	public TimeCredit() {

	}

	public synchronized void update() {
		long now = new Date().getTime();
		while(!fifo.isEmpty() && (fifo.peek() < (now - windowLength))) fifo.poll();
	}

	public synchronized void spend() throws InterruptedException {
		update();
		if(fifo.size() >= max) {
			long now = new Date().getTime();
			long until = fifo.peek();
			Thread.sleep( until + windowLength - now);
		}
		long now = new Date().getTime();
		fifo.add(now);
	}
}
