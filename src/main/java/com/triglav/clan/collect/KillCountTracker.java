package com.triglav.clan.collect;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.util.Text;

/**
 * Reads "Your X kill count is: N" game messages, the same signal Dink uses, so a LOOT event
 * fired right after a kill can carry `killCount` without any separate boss-tracking API.
 */
@Singleton
public class KillCountTracker
{
	private static final Pattern KILL_COUNT = Pattern.compile(
		"Your (.+?) (?:kill|completion|chest) count is: ([0-9,]+)\\.?", Pattern.CASE_INSENSITIVE);

	private final ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();

	public void onChatMessage(ChatMessage event)
	{
		final Matcher matcher = KILL_COUNT.matcher(Text.removeTags(event.getMessage()));
		if (!matcher.find())
		{
			return;
		}

		final String boss = Text.standardize(matcher.group(1));
		final int count = Integer.parseInt(matcher.group(2).replace(",", ""));
		counts.put(boss, count);
	}

	/** @return the last seen kill count for this NPC name, or -1 if none was seen this session */
	public int lastCount(String npcName)
	{
		return counts.getOrDefault(Text.standardize(npcName), -1);
	}
}
