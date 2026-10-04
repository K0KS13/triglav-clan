package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.util.Text;

/**
 * "You have completed 80 Master Treasure Trails." - the count and tier of a finished clue.
 * The casket's contents are not read here: they arrive as a normal loot event (LootReceived with
 * type EVENT) and go to the site as LOOT, so the drop feed shows them without any double counting.
 */
public final class ClueChat
{
	private static final Pattern COMPLETED = Pattern.compile(
		"You have completed (?<count>[\\d,]+) (?<tier>\\w+) Treasure Trails?\\.");

	private ClueChat()
	{
	}

	/** @return the CLUE `extra` payload, or null if this message isn't a completed clue */
	public static JsonObject parse(ChatMessage event)
	{
		final Matcher matcher = COMPLETED.matcher(Text.removeTags(event.getMessage()));
		if (!matcher.find())
		{
			return null;
		}

		final JsonObject extra = new JsonObject();
		extra.addProperty("clueType", matcher.group("tier"));
		extra.addProperty("numberCompleted", Integer.parseInt(matcher.group("count").replace(",", "")));
		return extra;
	}
}
