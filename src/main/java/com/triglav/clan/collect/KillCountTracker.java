package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.util.Text;

/**
 * Reads the "Your X kill count is: N" and "Fight duration: M:SS. Personal best: M:SS" game
 * messages, the same signals Dink uses - RuneLite has no structured kill count API. The count is
 * both attached to the next LOOT event for that NPC and sent as its own KILL_COUNT event, which
 * the site turns into a boss record (kill count + personal best, src/lib/dink/parse.ts).
 *
 * The two messages arrive on the same tick in either order, so the event is assembled on the game
 * tick after them. A duration without a boss name is dropped after a few ticks rather than kept,
 * so it cannot attach itself to an unrelated kill later on.
 */
@Singleton
public class KillCountTracker
{
	private static final Pattern KILL_COUNT = Pattern.compile(
		"Your (?<boss>.+?) (?:kill|chest|completion|harvest|success|opened|lap) ?count is: ?(?<count>[\\d,]+)\\b",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern KILL_COUNT_ALT = Pattern.compile(
		"Your (?:completed|subdued) (?<boss>.+?) count is: ?(?<count>[\\d,]+)\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern DURATION = Pattern.compile(
		"(?:Duration|time|Subdued in):? (?<time>[\\d:]+(?:\\.\\d+)?)\\.?(?: Personal best: (?<pb>[\\d:]+(?:\\.\\d+)?))?",
		Pattern.CASE_INSENSITIVE);

	/** A duration with no boss name is forgotten after this many ticks. */
	private static final int MAX_WAITING_TICKS = 10;

	private final ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();

	private String boss;
	private int count;
	private String time;
	private String personalBest;
	private boolean isPersonalBest;
	private int waitingTicks;

	public void reset()
	{
		boss = null;
		time = null;
		personalBest = null;
		isPersonalBest = false;
		waitingTicks = 0;
	}

	public void onChatMessage(ChatMessage event)
	{
		final String message = Text.removeTags(event.getMessage());

		Matcher matcher = KILL_COUNT.matcher(message);
		boolean found = matcher.find();
		if (!found)
		{
			matcher = KILL_COUNT_ALT.matcher(message);
			found = matcher.find();
		}

		if (found)
		{
			final String name = matcher.group("boss").trim();
			final int value = Integer.parseInt(matcher.group("count").replace(",", ""));
			counts.put(Text.standardize(name), value);
			boss = name;
			count = value;
			return;
		}

		final Matcher duration = DURATION.matcher(message);
		if (duration.find())
		{
			time = iso(duration.group("time"));
			personalBest = iso(duration.group("pb"));
			isPersonalBest = message.toLowerCase().contains("new personal best");
			if (isPersonalBest)
			{
				personalBest = time;
			}
		}
	}

	/** @return the KILL_COUNT `extra` payload once boss and count are known, or null */
	public JsonObject onGameTick()
	{
		if (boss == null)
		{
			if (time != null && ++waitingTicks > MAX_WAITING_TICKS)
			{
				reset();
			}
			return null;
		}

		final JsonObject extra = new JsonObject();
		extra.addProperty("boss", boss);
		extra.addProperty("count", count);
		if (time != null)
		{
			extra.addProperty("time", time);
			extra.addProperty("isPersonalBest", isPersonalBest);
			if (personalBest != null)
			{
				extra.addProperty("personalBest", personalBest);
			}
		}

		reset();
		return extra;
	}

	/** @return the last seen kill count for this NPC name, or -1 if none was seen this session */
	public int lastCount(String npcName)
	{
		return counts.getOrDefault(Text.standardize(npcName), -1);
	}

	/** "1:23.40" / "1:02:03" -> the ISO-8601 duration the site parses (src/lib/dink/parse.ts). */
	private static String iso(String clock)
	{
		if (clock == null || clock.isEmpty())
		{
			return null;
		}

		final String[] parts = clock.split(":");
		if (parts.length < 2 || parts.length > 3)
		{
			return null;
		}

		try
		{
			final int hours = parts.length == 3 ? Integer.parseInt(parts[0]) : 0;
			final int minutes = Integer.parseInt(parts[parts.length - 2]);
			final double seconds = Double.parseDouble(parts[parts.length - 1]);
			final StringBuilder out = new StringBuilder("PT");
			if (hours > 0)
			{
				out.append(hours).append('H');
			}
			if (minutes > 0)
			{
				out.append(minutes).append('M');
			}
			out.append(seconds == Math.floor(seconds) ? String.valueOf((int) seconds) : String.valueOf(seconds)).append('S');
			return out.toString();
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
}
