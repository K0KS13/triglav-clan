package com.triglav.clan.collect;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.util.Text;

/**
 * "New item added to your collection log: X" is the stable game message for a new unlock. It
 * carries no counts, only the item name - the site only needs `itemName` to record the
 * achievement (src/lib/achievements.ts, COLLECTION case), so that is all this sends.
 */
public final class CollectionLogChat
{
	private static final Pattern NEW_ITEM = Pattern.compile("New item added to your collection log: (.+)", Pattern.CASE_INSENSITIVE);

	private CollectionLogChat()
	{
	}

	/** @return the unlocked item's name, or null if this message isn't a collection log unlock */
	public static String newItemName(ChatMessage event)
	{
		final Matcher matcher = NEW_ITEM.matcher(Text.removeTags(event.getMessage()));
		return matcher.find() ? matcher.group(1).trim() : null;
	}
}
