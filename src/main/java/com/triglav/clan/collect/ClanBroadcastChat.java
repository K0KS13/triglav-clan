package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.client.util.Text;

/**
 * The clan broadcast "Jakvrh1 received a drop: Crystal armour seed (5,617,392 coins)". Every clan member sees it,
 * including drops the player's own client never reported (chests, players without the plugin), so the site can
 * list them. Only the game's own clan announcements are parsed; a player cannot type one.
 */
public final class ClanBroadcastChat
{
	private static final Pattern DROP = Pattern.compile(
		"^(?<rsn>.{1,12}?) received a drop: (?:(?<qty>[\\d,]+) x )?(?<item>.+?) \\((?<value>[\\d,]+) coins\\)\\.?$");

	private ClanBroadcastChat()
	{
	}

	/** @return {rsn, item, quantity, value} (value = total coins), or null if this is not a drop broadcast */
	public static JsonObject parse(String message)
	{
		final Matcher matcher = DROP.matcher(Text.removeTags(message).replace(' ', ' ').trim());
		if (!matcher.matches())
		{
			return null;
		}

		final JsonObject drop = new JsonObject();
		drop.addProperty("rsn", matcher.group("rsn").trim());
		drop.addProperty("item", matcher.group("item").trim());
		drop.addProperty("quantity", matcher.group("qty") == null ? 1 : Integer.parseInt(matcher.group("qty").replace(",", "")));
		drop.addProperty("value", Long.parseLong(matcher.group("value").replace(",", "")));
		return drop;
	}
}
