package com.triglav.clan.collect;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;

/**
 * XP_MILESTONE in Dink's shape: `milestoneAchieved` (skill names), `xpData` (current XP of every
 * skill) and `interval`. The site only puts milestones from 50M XP up on the wall, so the
 * interval is 50M rather than Dink's 5M default - anything smaller would be stored and ignored.
 * Like the level tracker, XP is primed at login so the first StatChanged burst is not a milestone.
 */
@Singleton
public class XpMilestoneTracker
{
	public static final int INTERVAL = 50_000_000;
	/** The Duel Arena's temporary 99s jump many skills by millions of XP at once; a lone big gain (a lamp) is real. */
	private static final int BIG_GAIN = 5_000_000;
	private static final int BULK_SKILLS = 3;

	private final Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
	private final JsonArray pending = new JsonArray();
	private final java.util.List<String> bigGains = new java.util.ArrayList<>();
	private final java.util.List<String> lastBigCrossing = new java.util.ArrayList<>();

	public void prime(Client client)
	{
		xp.clear();
		while (pending.size() > 0)
		{
			pending.remove(0);
		}

		for (Skill skill : Skill.values())
		{
			if (skill != Skill.OVERALL)
			{
				xp.put(skill, client.getSkillExperience(skill));
			}
		}
	}

	public void reset()
	{
		xp.clear();
		while (pending.size() > 0)
		{
			pending.remove(0);
		}
	}

	public void onStatChanged(StatChanged event)
	{
		final Skill skill = event.getSkill();
		if (skill == Skill.OVERALL)
		{
			return;
		}

		final Integer previous = xp.put(skill, event.getXp());
		if (previous == null)
		{
			return;
		}

		if (event.getXp() - previous >= BIG_GAIN)
		{
			bigGains.add(skill.getName());
		}
		if (event.getXp() / INTERVAL > previous / INTERVAL && event.getXp() - previous < BIG_GAIN)
		{
			pending.add(skill.getName());
		}
		else if (event.getXp() / INTERVAL > previous / INTERVAL)
		{
			lastBigCrossing.add(skill.getName());
		}
	}

	/** @return the XP_MILESTONE `extra` payload once a milestone is pending, or null */
	public JsonObject onGameTick()
	{
		if (bigGains.size() < BULK_SKILLS)
		{
			lastBigCrossing.forEach(pending::add); // a lone big gain (lamp) that crossed a milestone is real
		}
		bigGains.clear();
		lastBigCrossing.clear();

		if (pending.size() == 0)
		{
			return null;
		}

		final JsonObject xpData = new JsonObject();
		for (Map.Entry<Skill, Integer> entry : xp.entrySet())
		{
			xpData.addProperty(entry.getKey().getName(), entry.getValue());
		}

		final JsonObject extra = new JsonObject();
		extra.add("milestoneAchieved", pending.deepCopy());
		extra.add("xpData", xpData);
		extra.addProperty("interval", INTERVAL);

		while (pending.size() > 0)
		{
			pending.remove(0);
		}
		return extra;
	}
}
