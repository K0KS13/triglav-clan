package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;

/**
 * Combat achievements from the completion message, with the point totals read from varbits
 * (site: `currentTier` keeps Account.caTier in sync for rank recommendations, `justCompletedTier`
 * raises the milestone). Per-task events carry no milestone on their own, so they cost nothing
 * beyond one stored event.
 */
@Singleton
public class CombatTaskTracker
{
	private static final Pattern TASK = Pattern.compile(
		"Congratulations, you've completed an? (?<tier>\\w+) combat task: (?<task>.+?)\\.");
	private static final Pattern POINTS_SUFFIX = Pattern.compile("\\s*\\(\\d+ points?\\)$");

	private static final String[] TIERS = {"EASY", "MEDIUM", "HARD", "ELITE", "MASTER", "GRANDMASTER"};

	/** Tasks completed per tier. */
	private static final int[] COMPLETED_VARBITS = {
		VarbitID.CA_TOTAL_TASKS_COMPLETED_EASY, VarbitID.CA_TOTAL_TASKS_COMPLETED_MEDIUM,
		VarbitID.CA_TOTAL_TASKS_COMPLETED_HARD, VarbitID.CA_TOTAL_TASKS_COMPLETED_ELITE,
		VarbitID.CA_TOTAL_TASKS_COMPLETED_MASTER, VarbitID.CA_TOTAL_TASKS_COMPLETED_GRANDMASTER,
	};

	/** Cumulative points that unlock each tier's rewards; 0 until the client has them. */
	private static final int[] THRESHOLD_VARBITS = {
		VarbitID.CA_THRESHOLD_EASY, VarbitID.CA_THRESHOLD_MEDIUM, VarbitID.CA_THRESHOLD_HARD,
		VarbitID.CA_THRESHOLD_ELITE, VarbitID.CA_THRESHOLD_MASTER, VarbitID.CA_THRESHOLD_GRANDMASTER,
	};

	/** Points a single task of each tier is worth. */
	private static final int[] TIER_POINTS = {1, 2, 3, 4, 5, 6};

	/**
	 * @return the COMBAT_ACHIEVEMENT `extra` payload, or null when the message is something else.
	 * Reads varbits, so it must run on the client thread, one tick after the message (the client
	 * has not applied the new totals yet while the message itself is being handled).
	 */
	public JsonObject parse(Client client, String message)
	{
		final Matcher matcher = TASK.matcher(message);
		if (!matcher.find())
		{
			return null;
		}

		final int tierIndex = indexOfTier(matcher.group("tier"));
		if (tierIndex < 0)
		{
			return null;
		}

		final JsonObject extra = new JsonObject();
		extra.addProperty("tier", TIERS[tierIndex]);
		extra.addProperty("task", POINTS_SUFFIX.matcher(matcher.group("task")).replaceFirst(""));

		final int totalPoints = totalPoints(client);
		extra.addProperty("totalPoints", totalPoints);

		final String current = tierFor(client, totalPoints);
		if (current != null)
		{
			extra.addProperty("currentTier", current);

			// The same task that pushed us over a reward threshold is the one that completed the tier.
			final String before = tierFor(client, totalPoints - TIER_POINTS[tierIndex]);
			if (!current.equals(before))
			{
				extra.addProperty("justCompletedTier", current);
			}
		}

		return extra;
	}

	private static int indexOfTier(String name)
	{
		for (int i = 0; i < TIERS.length; i++)
		{
			if (TIERS[i].equalsIgnoreCase(name))
			{
				return i;
			}
		}
		return -1;
	}

	private int totalPoints(Client client)
	{
		int total = 0;
		for (int i = 0; i < TIERS.length; i++)
		{
			total += client.getVarbitValue(COMPLETED_VARBITS[i]) * TIER_POINTS[i];
		}
		return total;
	}

	/** @return the highest tier whose reward threshold these points reach, or null if unknown */
	private String tierFor(Client client, int points)
	{
		String tier = null;
		for (int i = 0; i < TIERS.length; i++)
		{
			final int threshold = client.getVarbitValue(THRESHOLD_VARBITS[i]);
			if (threshold > 0 && points >= threshold)
			{
				tier = TIERS[i];
			}
		}
		return tier;
	}
}
