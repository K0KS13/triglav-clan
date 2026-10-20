package com.triglav.clan.star;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StarReporterTest
{
	@Test
	public void mapsCrashedStarObjectsToSize()
	{
		assertEquals(9, StarReporter.starSize(41020));
		assertEquals(8, StarReporter.starSize(41021));
		assertEquals(7, StarReporter.starSize(41223));
		assertEquals(1, StarReporter.starSize(41229));
	}

	@Test
	public void ignoresOtherObjects()
	{
		assertEquals(0, StarReporter.starSize(0));
		assertEquals(0, StarReporter.starSize(41222));
		assertEquals(0, StarReporter.starSize(41230));
	}
}
