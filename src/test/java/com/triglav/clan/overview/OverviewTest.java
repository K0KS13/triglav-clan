package com.triglav.clan.overview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonParser;
import org.junit.Test;

public class OverviewTest
{
	@Test
	public void parsesTheSiteResponse()
	{
		final Overview o = Overview.parse(new JsonParser().parse("{"
			+ "\"me\":{\"rsn\":\"K0KS\",\"name\":\"Koks\",\"points\":246},"
			+ "\"events\":[{\"id\":\"e1\",\"title\":\"Raid\",\"type\":\"RAID\",\"startsAt\":\"2026-10-07T18:00:00.000Z\",\"going\":4,\"mine\":\"YES\",\"icon\":\"https://clan.kokalj.dev/api/icon/boss/zulrah\"}],"
			+ "\"lfg\":[{\"id\":\"l1\",\"title\":\"ToA\",\"activity\":\"ToA\",\"startsAt\":\"2026-10-07T19:00:00.000Z\",\"capacity\":4,\"taken\":2,\"mine\":false}],"
			+ "\"myLfg\":[],"
			+ "\"goals\":[{\"id\":\"g\",\"title\":\"10k Vorkath\",\"boss\":\"Vorkath\",\"target\":10000,\"current\":2500,\"done\":false}],"
			+ "\"deaths\":{\"month\":3,\"valueLost\":4500000},"
			+ "\"shop\":[{\"id\":\"s1\",\"name\":\"Srecka\",\"cost\":50,\"affordable\":true}]}").getAsJsonObject());

		assertEquals("K0KS", o.rsn);
		assertEquals(246, o.points);
		assertEquals(1, o.events.size());
		assertTrue(o.events.get(0).mine);
		assertEquals(4, o.events.get(0).going);
		assertEquals("https://clan.kokalj.dev/api/icon/boss/zulrah", o.events.get(0).icon);
		assertEquals(null, o.lfg.get(0).icon);
		assertEquals(2, o.lfg.get(0).taken);
		assertEquals(2500, o.goals.get(0).current);
		assertEquals(3, o.deathsThisMonth);
		assertEquals(4_500_000L, o.valueLostThisMonth);
		assertTrue(o.shop.get(0).affordable);
	}

	@Test
	public void anEmptyResponseIsHarmless()
	{
		final Overview o = Overview.parse(new JsonParser().parse("{}").getAsJsonObject());
		assertEquals(null, o.rsn);
		assertTrue(o.events.isEmpty() && o.lfg.isEmpty() && o.myLfg.isEmpty() && o.goals.isEmpty() && o.shop.isEmpty());
	}
}
