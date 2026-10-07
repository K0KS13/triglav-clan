package com.triglav.clan.overview;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parsed GET /api/plugin/overview/:key (site: src/app/api/plugin/overview/[key]/route.ts). */
public final class Overview
{
	public static final Overview EMPTY = new Overview(null, 0, 0, Collections.emptyList(), Collections.emptyList(),
		Collections.emptyList(), Collections.emptyList(), 0L, Collections.emptyList());

	public static final class Event
	{
		public final String id;
		public final String title;
		public final Instant startsAt;
		public final int going;
		public final boolean mine;

		Event(String id, String title, Instant startsAt, int going, boolean mine)
		{
			this.id = id;
			this.title = title;
			this.startsAt = startsAt;
			this.going = going;
			this.mine = mine;
		}
	}

	public static final class Lfg
	{
		public final String id;
		public final String title;
		public final String activity;
		public final Instant startsAt;
		public final int capacity;
		public final int taken;
		public final boolean mine;

		Lfg(String id, String title, String activity, Instant startsAt, int capacity, int taken, boolean mine)
		{
			this.id = id;
			this.title = title;
			this.activity = activity;
			this.startsAt = startsAt;
			this.capacity = capacity;
			this.taken = taken;
			this.mine = mine;
		}
	}

	public static final class GearItem
	{
		public final int id;
		public final String name;
		public final int quantity;

		GearItem(int id, String name, int quantity)
		{
			this.id = id;
			this.name = name;
			this.quantity = quantity;
		}
	}

	/** An LFG the member is part of, with the setup the post recommends (null if it has none). */
	public static final class MyLfg
	{
		public final String id;
		public final String title;
		public final Instant startsAt;
		public final String gearTitle;
		public final List<GearItem> equipment;
		public final List<GearItem> inventory;

		MyLfg(String id, String title, Instant startsAt, String gearTitle, List<GearItem> equipment, List<GearItem> inventory)
		{
			this.id = id;
			this.title = title;
			this.startsAt = startsAt;
			this.gearTitle = gearTitle;
			this.equipment = equipment;
			this.inventory = inventory;
		}

		public boolean hasGear()
		{
			return gearTitle != null && !(equipment.isEmpty() && inventory.isEmpty());
		}
	}

	public static final class Goal
	{
		public final String title;
		public final int current;
		public final int target;

		Goal(String title, int current, int target)
		{
			this.title = title;
			this.current = current;
			this.target = target;
		}
	}

	public static final class ShopItem
	{
		public final String id;
		public final String name;
		public final int cost;
		public final boolean affordable;

		ShopItem(String id, String name, int cost, boolean affordable)
		{
			this.id = id;
			this.name = name;
			this.cost = cost;
			this.affordable = affordable;
		}
	}

	public final String rsn;
	public final int points;
	public final int deathsThisMonth;
	public final long valueLostThisMonth;
	public final List<Event> events;
	public final List<Lfg> lfg;
	public final List<MyLfg> myLfg;
	public final List<Goal> goals;
	public final List<ShopItem> shop;

	private Overview(String rsn, int points, int deathsThisMonth, List<Event> events, List<Lfg> lfg, List<MyLfg> myLfg,
		List<Goal> goals, long valueLostThisMonth, List<ShopItem> shop)
	{
		this.rsn = rsn;
		this.points = points;
		this.deathsThisMonth = deathsThisMonth;
		this.events = events;
		this.lfg = lfg;
		this.myLfg = myLfg;
		this.goals = goals;
		this.valueLostThisMonth = valueLostThisMonth;
		this.shop = shop;
	}

	public static Overview parse(JsonObject body)
	{
		final JsonObject me = obj(body, "me");
		final JsonObject deaths = obj(body, "deaths");

		final List<Event> events = new ArrayList<>();
		for (JsonElement e : array(body, "events"))
		{
			final JsonObject o = e.getAsJsonObject();
			events.add(new Event(str(o, "id"), str(o, "title"), time(o, "startsAt"), integer(o, "going"),
				o.has("mine") && !o.get("mine").isJsonNull() && "YES".equals(o.get("mine").getAsString())));
		}

		final List<Lfg> lfg = new ArrayList<>();
		for (JsonElement e : array(body, "lfg"))
		{
			final JsonObject o = e.getAsJsonObject();
			lfg.add(new Lfg(str(o, "id"), str(o, "title"), str(o, "activity"), time(o, "startsAt"),
				integer(o, "capacity"), integer(o, "taken"), o.has("mine") && o.get("mine").getAsBoolean()));
		}

		final List<MyLfg> mine = new ArrayList<>();
		for (JsonElement e : array(body, "myLfg"))
		{
			final JsonObject o = e.getAsJsonObject();
			final JsonObject gear = o.has("gear") && o.get("gear").isJsonObject() ? o.getAsJsonObject("gear") : null;
			mine.add(new MyLfg(str(o, "id"), str(o, "title"), time(o, "startsAt"),
				gear == null ? null : str(gear, "title"),
				gear == null ? Collections.emptyList() : gearItems(array(gear, "equipment")),
				gear == null ? Collections.emptyList() : gearItems(array(gear, "inventory"))));
		}

		final List<Goal> goals = new ArrayList<>();
		for (JsonElement e : array(body, "goals"))
		{
			final JsonObject o = e.getAsJsonObject();
			goals.add(new Goal(str(o, "title"), integer(o, "current"), integer(o, "target")));
		}

		final List<ShopItem> shop = new ArrayList<>();
		for (JsonElement e : array(body, "shop"))
		{
			final JsonObject o = e.getAsJsonObject();
			shop.add(new ShopItem(str(o, "id"), str(o, "name"), integer(o, "cost"), o.has("affordable") && o.get("affordable").getAsBoolean()));
		}

		return new Overview(me.has("rsn") && !me.get("rsn").isJsonNull() ? me.get("rsn").getAsString() : null,
			integer(me, "points"), integer(deaths, "month"), events, lfg, mine, goals, longValue(deaths, "valueLost"), shop);
	}

	private static List<GearItem> gearItems(JsonArray items)
	{
		final List<GearItem> out = new ArrayList<>();
		for (JsonElement e : items)
		{
			final JsonObject o = e.getAsJsonObject();
			out.add(new GearItem(integer(o, "id"), str(o, "name"), o.has("qty") ? Math.max(1, integer(o, "qty")) : 1));
		}
		return out;
	}

	private static JsonObject obj(JsonObject parent, String key)
	{
		return parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : new JsonObject();
	}

	private static JsonArray array(JsonObject parent, String key)
	{
		return parent.has(key) && parent.get(key).isJsonArray() ? parent.getAsJsonArray(key) : new JsonArray();
	}

	private static String str(JsonObject o, String key)
	{
		return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
	}

	private static int integer(JsonObject o, String key)
	{
		return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : 0;
	}

	private static long longValue(JsonObject o, String key)
	{
		return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsLong() : 0L;
	}

	private static Instant time(JsonObject o, String key)
	{
		try
		{
			return Instant.parse(str(o, key));
		}
		catch (RuntimeException e)
		{
			return Instant.EPOCH;
		}
	}
}
