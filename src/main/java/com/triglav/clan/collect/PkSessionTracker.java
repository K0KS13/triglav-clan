package com.triglav.clan.collect;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * One PK outing in one report: kills, deaths and Loot Chest value between the first PK action and a quiet spell
 * (no kill, death or chest for {@value #QUIET_MILLIS} ms). The site shows it on the member's profile. The summary is sent
 * from a game tick, so it also goes out the next time the player is online after a logout.
 */
@Singleton
public class PkSessionTracker
{
	private static final long QUIET_MILLIS = 5 * 60_000;

	private boolean active;
	private long startedAt;
	private long lastActivityAt;
	private int kills;
	private int deaths;
	private long victimGear;
	private long chestValue;
	private long deathLoss;

	@Inject
	private PkSessionTracker()
	{
	}

	public synchronized void onKill(JsonObject kill)
	{
		touch();
		kills++;
		if (kill.has("victimEquipment") && kill.get("victimEquipment").isJsonObject())
		{
			for (java.util.Map.Entry<String, JsonElement> slot : kill.getAsJsonObject("victimEquipment").entrySet())
			{
				final JsonObject item = slot.getValue().getAsJsonObject();
				if (item.has("priceEach"))
				{
					victimGear += item.get("priceEach").getAsLong();
				}
			}
		}
	}

	/** @param death the DEATH `extra`; counted only when it is a PK death or has a known loss */
	public synchronized void onDeath(JsonObject death)
	{
		if (!active)
		{
			return; // a death outside a PK outing is not part of one
		}
		touch();
		deaths++;
		if (death.has("valueLost"))
		{
			deathLoss += Math.max(0, death.get("valueLost").getAsLong());
		}
	}

	public synchronized void onChest(long value)
	{
		if (!active)
		{
			return;
		}
		touch();
		chestValue += value;
	}

	/** @return the finished session's `extra`, or null while it is running or nothing is recorded */
	public synchronized JsonObject onGameTick()
	{
		if (!active || System.currentTimeMillis() - lastActivityAt < QUIET_MILLIS)
		{
			return null;
		}

		final JsonObject extra = new JsonObject();
		extra.addProperty("kills", kills);
		extra.addProperty("deaths", deaths);
		extra.addProperty("chestValue", chestValue);
		extra.addProperty("victimGear", victimGear);
		extra.addProperty("deathLoss", deathLoss);
		extra.addProperty("durationSeconds", Math.max(0, (lastActivityAt - startedAt) / 1000));
		extra.addProperty("startedAt", startedAt);
		active = false;
		kills = 0;
		deaths = 0;
		victimGear = 0;
		chestValue = 0;
		deathLoss = 0;
		return extra;
	}

	private void touch()
	{
		final long now = System.currentTimeMillis();
		if (!active)
		{
			active = true;
			startedAt = now;
		}
		lastActivityAt = now;
	}
}
