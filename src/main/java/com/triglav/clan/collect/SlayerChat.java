package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.util.Text;

/**
 * A finished slayer task arrives as two messages: what was killed ("You have completed your task!
 * You killed 134 Hellhounds.") and, from a slayer master, the running totals ("You've completed
 * 342 tasks and received 15 points, giving you a total of 1,234."). The second one is what the
 * site turns into a task-count milestone, so the first is held until it shows up.
 */
@Singleton
public class SlayerChat
{
	private static final Pattern TASK = Pattern.compile(
		"You have completed your task! You killed (?<task>[\\d,]+ [^.]+)\\..*");
	private static final Pattern TOTALS = Pattern.compile(
		"You've completed (?:at least )?(?<tasks>[\\d,]+) (?:Wilderness |Mortimer )?tasks?"
			+ "(?: and received (?<points>[\\d,]+) points, giving you a total of [\\d,]+"
			+ "| and reached the maximum amount of Slayer points \\((?<maxPoints>[\\d,]+)\\))?");

	private String lastTask = "";

	public void reset()
	{
		lastTask = "";
	}

	/** @return the SLAYER `extra` payload once the totals message lands, or null */
	public JsonObject parse(ChatMessage event)
	{
		final String message = Text.removeTags(event.getMessage());

		final Matcher task = TASK.matcher(message);
		if (task.find())
		{
			lastTask = task.group("task");
			return null;
		}

		final Matcher totals = TOTALS.matcher(message);
		if (!totals.find())
		{
			return null;
		}

		final JsonObject extra = new JsonObject();
		if (!lastTask.isEmpty())
		{
			extra.addProperty("slayerTask", lastTask);
			lastTask = "";
		}
		extra.addProperty("slayerCompleted", totals.group("tasks"));

		final String points = totals.group("points") == null ? totals.group("maxPoints") : totals.group("points");
		extra.addProperty("slayerPoints", points == null ? "0" : points);
		return extra;
	}
}
