package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;

/**
 * Collection log progress straight from the two varps the log's own header uses, so no widget has
 * to be open. They read 0 until the client has them, in which case nothing is reported - a wrong
 * total would skew the site's rank recommendations, a missing one just leaves the old value.
 */
public final class CollectionLogTotals
{
	private CollectionLogTotals()
	{
	}

	public static int completed(Client client)
	{
		return client.getVarpValue(VarPlayerID.COLLECTION_COUNT);
	}

	public static int total(Client client)
	{
		return client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX);
	}

	/** @return {completed,total} in the shape of the site's LOGIN snapshot, or null while unknown */
	public static JsonObject snapshot(Client client)
	{
		final int completed = completed(client);
		final int total = total(client);
		if (completed <= 0 || total <= 0 || completed > total)
		{
			return null;
		}

		final JsonObject progress = new JsonObject();
		progress.addProperty("completed", completed);
		progress.addProperty("total", total);
		return progress;
	}
}
