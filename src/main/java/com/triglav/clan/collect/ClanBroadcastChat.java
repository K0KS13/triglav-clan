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

	/** "Oderuh has been defeated by Lesstellyak in The Wilderness and lost (1,154,234 coins) worth of loot." (the ending varies) */
	private static final Pattern DEATH = Pattern.compile(
		"^(?<rsn>.{1,12}?) has been defeated by (?<killer>.{1,12}?) in (?<where>.+?) and lost \\((?<value>[\\d,]+) coins\\) worth of loot.*$");

	/** "Jakvrh1 has reached level 99 in Hitpoints." / "Jakvrh1 has reached 99 Hitpoints." */
	private static final Pattern LEVEL99 = Pattern.compile("^(?<rsn>.{1,12}?) has reached (?:level )?99 (?:in )?(?<skill>[A-Za-z]+)\\.?$");
	/** "Jakvrh1 received a new collection log item: Abyssal whip (432/1477)" */
	private static final Pattern CLOG = Pattern.compile("^(?<rsn>.{1,12}?) received a new collection log item: (?<item>.+?) \\(\\d[\\d,]*/\\d[\\d,]*\\)\\.?$");

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

	/** @return {rsn, killer, location, value} (value = coins of loot lost), or null if this is not a death broadcast */
	public static JsonObject parseDeath(String message)
	{
		final Matcher matcher = DEATH.matcher(Text.removeTags(message).replace(' ', ' ').trim());
		if (!matcher.matches())
		{
			return null;
		}

		final JsonObject death = new JsonObject();
		death.addProperty("rsn", matcher.group("rsn").trim());
		death.addProperty("killer", matcher.group("killer").trim());
		death.addProperty("location", matcher.group("where").trim());
		death.addProperty("value", Long.parseLong(matcher.group("value").replace(",", "")));
		return death;
	}

	/** @return {kind: "level99", rsn, skill} or {kind: "clog", rsn, item}, or null if this is neither */
	public static JsonObject parseEvent(String message)
	{
		final String text = Text.removeTags(message).replace(' ', ' ').trim();
		final Matcher level = LEVEL99.matcher(text);
		if (level.matches())
		{
			final JsonObject json = new JsonObject();
			json.addProperty("kind", "level99");
			json.addProperty("rsn", level.group("rsn").trim());
			json.addProperty("skill", level.group("skill"));
			return json;
		}

		final Matcher clog = CLOG.matcher(text);
		if (clog.matches())
		{
			final JsonObject json = new JsonObject();
			json.addProperty("kind", "clog");
			json.addProperty("rsn", clog.group("rsn").trim());
			json.addProperty("item", clog.group("item").trim());
			return json;
		}
		return null;
	}

	/** Clean text of a clan broadcast, for the "unknown message" samples that teach the site new formats. */
	public static String plain(String message)
	{
		final String text = Text.removeTags(message).replace(' ', ' ').trim();
		return text.length() > 220 ? text.substring(0, 220) : text;
	}
}
