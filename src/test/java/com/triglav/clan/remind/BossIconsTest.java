package com.triglav.clan.remind;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BossIconsTest
{
	@Test
	public void findsTheBossInATitle()
	{
		assertEquals(12921, BossIcons.itemIdFor("TEST ZULRAH"));
		assertEquals(12921, BossIcons.itemIdFor("Zul-Andra kill"));
		assertEquals(21992, BossIcons.itemIdFor("ostalo", "vorkath učenje"));
		assertEquals(25348, BossIcons.itemIdFor("ToA 300 invo"));
		assertEquals(22473, BossIcons.itemIdFor("tob 3 man"));
	}

	@Test
	public void shortAliasesNeedWholeWords()
	{
		assertEquals(-1, BossIcons.itemIdFor("Konkurenca"));       // contains "nex"? no — but "kq" style traps
		assertEquals(-1, BossIcons.itemIdFor("Moxie fisherman"));
		assertEquals(26348, BossIcons.itemIdFor("Nex mass"));
		assertEquals(-1, BossIcons.itemIdFor("Next week social"));  // "nex" inside "next" must not match
		assertEquals(13262, BossIcons.itemIdFor("sire hunt"));
		assertEquals(-1, BossIcons.itemIdFor("desire to fish"));
	}

	@Test
	public void primeAndRexAreNotMixedUp()
	{
		assertEquals(12644, BossIcons.itemIdFor("Dagannoth Prime"));
		assertEquals(12645, BossIcons.itemIdFor("Dagannoth Rex"));
	}

	@Test
	public void unknownActivitiesFallBackToThePluginIcon()
	{
		assertEquals(-1, BossIcons.itemIdFor("Klepet in druženje"));
		assertEquals(-1, BossIcons.itemIdFor((String) null));
	}
}
