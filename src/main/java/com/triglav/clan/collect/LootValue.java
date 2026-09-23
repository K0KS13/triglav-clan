package com.triglav.clan.collect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Total GE value of a LOOT `extra.items` array, used to decide whether a drop clears the screenshot threshold. */
public final class LootValue
{
	private LootValue()
	{
	}

	public static long total(JsonArray items)
	{
		long total = 0;
		for (JsonElement element : items)
		{
			final JsonObject item = element.getAsJsonObject();
			total += item.get("priceEach").getAsLong() * item.get("quantity").getAsLong();
		}
		return total;
	}
}
