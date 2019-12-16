package fr.inria.diverse.maven.resolver.processor.testfinder;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class TimeCreditTest {

	@Test
	public void testTimeSpending() throws InterruptedException {
		long windows = 2000;
		long start = new Date().getTime();
		TimeCredit t = new TimeCredit(windows, 3);
		t.spend();
		t.spend();
		t.spend();
		t.spend();
		//t.spend();
		long end = new Date().getTime();
		assertTrue(end >= (start + windows));
	}

}