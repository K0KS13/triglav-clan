package com.triglav.clan.net;

import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.Player;

/**
 * The envelope every ingest event shares (site: src/lib/dink/schema.ts). Kept identical to
 * what Dink sends so the site's existing parser needs no changes. The site rejects an empty
 * `playerName`, so callers that may run without a loaded local player (logout) pass the name in.
 */
public final class Envelope
{
	private Envelope()
	{
	}

	/** @return the local player's name, or null while the player isn't loaded (login screen, first login ticks) */
	public static String playerName(Client client)
	{
		final Player local = client.getLocalPlayer();
		final String name = local == null ? null : local.getName();
		return name == null || name.isEmpty() ? null : name;
	}

	public static JsonObject create(Client client, String type, JsonObject extra)
	{
		return create(client, type, extra, playerName(client));
	}

	public static JsonObject create(Client client, String type, JsonObject extra, String playerName)
	{
		final JsonObject envelope = new JsonObject();
		envelope.addProperty("type", type);
		envelope.addProperty("playerName", playerName == null ? "" : playerName);
		envelope.addProperty("accountType", client.getAccountType().name());
		envelope.addProperty("dinkAccountHash", Long.toString(client.getAccountHash()));
		envelope.addProperty("world", client.getWorld());
		envelope.add("extra", extra == null ? new JsonObject() : extra);
		return envelope;
	}
}
