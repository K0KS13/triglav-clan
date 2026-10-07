package com.triglav.clan.remind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.triglav.clan.overview.Overview;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class GearCheckTest
{
	private static Overview.MyLfg lfg(String gearJson)
	{
		final JsonObject body = new JsonParser().parse("{\"myLfg\":[{\"id\":\"l1\",\"title\":\"Zulrah\",\"activity\":\"Zulrah\","
			+ "\"startsAt\":\"2026-10-07T20:00:00.000Z\",\"gear\":" + gearJson + "}]}").getAsJsonObject();
		return Overview.parse(body).myLfg.get(0);
	}

	private static final String SETUP = "{\"title\":\"Zulrah mage\",\"equipment\":[{\"id\":12899,\"name\":\"Trident of the swamp\"},{\"id\":4151,\"name\":\"Abyssal whip\"}],"
		+ "\"inventory\":[{\"id\":385,\"name\":\"Shark\",\"qty\":3},{\"id\":3024,\"name\":\"Super restore(4)\",\"qty\":2}]}";

	@Test
	public void everythingBroughtMeansNothingMissing()
	{
		final Map<Integer, Integer> have = new HashMap<>();
		have.put(12899, 1);
		have.put(4151, 1);
		have.put(385, 5);
		have.put(3024, 2);
		assertTrue(GearCheck.missing(lfg(SETUP), have, id -> id).isEmpty());
	}

	@Test
	public void listsMissingItemsAndShortfalls()
	{
		final Map<Integer, Integer> have = new HashMap<>();
		have.put(12899, 1);
		have.put(385, 1); // 3 sharks wanted, 1 carried
		final List<String> missing = GearCheck.missing(lfg(SETUP), have, id -> id);
		assertEquals(Arrays.asList("Abyssal whip", "Shark x2", "Super restore(4) x2"), missing);
	}

	@Test
	public void aWornItemCountsEvenIfTheSetupHasItInTheInventory()
	{
		final Map<Integer, Integer> have = new HashMap<>();
		have.put(4151, 1);
		final Overview.MyLfg mine = lfg("{\"title\":\"x\",\"equipment\":[],\"inventory\":[{\"id\":4151,\"name\":\"Abyssal whip\"}]}");
		assertTrue(GearCheck.missing(mine, have, id -> id).isEmpty());
	}

	@Test
	public void canonicalIdsMatchVariants()
	{
		final Map<Integer, Integer> have = new HashMap<>();
		have.put(4151, 1); // the canonical id is what the carried side is keyed by
		final Overview.MyLfg mine = lfg("{\"title\":\"x\",\"equipment\":[{\"id\":4152,\"name\":\"Abyssal whip\"}],\"inventory\":[]}");
		assertTrue(GearCheck.missing(mine, have, id -> id == 4152 ? 4151 : id).isEmpty());
	}

	@Test
	public void lfgWithoutASetupHasNothingToCheck()
	{
		final JsonObject body = new JsonParser().parse("{\"myLfg\":[{\"id\":\"l\",\"title\":\"t\",\"activity\":\"a\",\"startsAt\":\"2026-10-07T20:00:00.000Z\",\"gear\":null}]}").getAsJsonObject();
		final Overview.MyLfg mine = Overview.parse(body).myLfg.get(0);
		assertEquals(false, mine.hasGear());
		assertTrue(GearCheck.missing(mine, Collections.emptyMap(), id -> id).isEmpty());
	}
}
