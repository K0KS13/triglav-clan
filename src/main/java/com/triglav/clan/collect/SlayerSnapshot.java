package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.Varbits;

/**
 * `slayer` block of the LOGIN snapshot (site: src/lib/dink/snapshot.ts, `{points, streak}`).
 * Both are plain varbit reads (Varbits.SLAYER_POINTS / SLAYER_TASK_STREAK) — long-stable,
 * unlike collection log or combat achievement totals which need widget access.
 */
public final class SlayerSnapshot
{
	private SlayerSnapshot()
	{
	}

	public static JsonObject build(Client client)
	{
		final JsonObject slayer = new JsonObject();
		slayer.addProperty("points", client.getVarbitValue(Varbits.SLAYER_POINTS));
		slayer.addProperty("streak", client.getVarbitValue(Varbits.SLAYER_TASK_STREAK));
		return slayer;
	}
}
