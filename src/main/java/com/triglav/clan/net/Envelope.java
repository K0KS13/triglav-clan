package com.triglav.clan.net;

import com.google.gson.JsonObject;
import net.runelite.api.Client;

/**
 * The envelope every ingest event shares (site: src/lib/dink/schema.ts). Kept identical to
 * what Dink sends so the site's existing parser needs no changes.
 */
public final class Envelope
{
	private Envelope()
	{
	}

	public static JsonObject create(Client client, String type, JsonObject extra)
	{
		final JsonObject envelope = new JsonObject();
		envelope.addProperty("type", type);
		envelope.addProperty("playerName", client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : "");
		envelope.addProperty("accountType", client.getAccountType().name());
		envelope.addProperty("dinkAccountHash", Long.toString(client.getAccountHash()));
		envelope.addProperty("world", client.getWorld());
		envelope.add("extra", extra == null ? new JsonObject() : extra);
		return envelope;
	}
}
