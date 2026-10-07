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

	private final Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
	private final JsonArray pending = new JsonArray();

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
		if (previous != null && event.getXp() / INTERVAL > previous / INTERVAL)
		{
			pending.add(skill.getName());
		}
	}

	/** @return the XP_MILESTONE `extra` payload once a milestone is pending, or null */
	public JsonObject onGameTick()
	{
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
