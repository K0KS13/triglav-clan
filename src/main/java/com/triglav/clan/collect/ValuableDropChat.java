package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.client.util.Text;

/**
 * The game's own "Valuable drop: 3 x Rune arrow (2,500 coins)" message, shown for any drop above the player's in-game
 * valuable-drop threshold, whatever its source. It covers loot the Loot Tracker never reports, such as the Gauntlet's
 * reward chest, so the drop still reaches the site with a screenshot.
 */
public final class ValuableDropChat
{
	private static final Pattern DROP = Pattern.compile(
		"^Valuable drop: (?:(?<qty>[\\d,]+) x )?(?<item>.+?) \\((?<value>[\\d,]+) coins\\)\\.?$");

	private ValuableDropChat()
	{
	}

	/** @return {item, quantity, value} (value = total coins), or null if this is not a valuable-drop message */
	public static JsonObject parse(String message)
	{
		final Matcher matcher = DROP.matcher(Text.removeTags(message).replace(' ', ' ').trim());
		if (!matcher.matches())
		{
			return null;
		}

		final JsonObject drop = new JsonObject();
		drop.addProperty("item", matcher.group("item").trim());
		drop.addProperty("quantity", matcher.group("qty") == null ? 1 : Integer.parseInt(matcher.group("qty").replace(",", "")));
		drop.addProperty("value", Long.parseLong(matcher.group("value").replace(",", "")));
		return drop;
	}
}
