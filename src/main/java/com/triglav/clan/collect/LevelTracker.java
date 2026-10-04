package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;

/**
 * Turns RuneLite's StatChanged into a Dink-shaped LEVEL event (site: src/lib/achievements.ts,
 * which raises a 99 milestone from `levelledSkills` and "maxed" from `allSkills`).
 *
 * StatChanged fires for every skill right after login, so the levels are primed from the client
 * when the LOGIN snapshot goes out and only a later increase counts as a level-up. Two skills can
 * level on the same tick (Hitpoints alongside a combat skill), so they are batched into one event.
 */
@Singleton
public class LevelTracker
{
	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
	private final Map<String, Integer> pending = new LinkedHashMap<>();

	/** Called with the LOGIN snapshot: what we see now is the baseline, not a level-up. */
	public void prime(Client client)
	{
		levels.clear();
		pending.clear();
		for (Skill skill : Skill.values())
		{
			if (skill != Skill.OVERALL)
			{
				levels.put(skill, client.getRealSkillLevel(skill));
			}
		}
	}

	public void reset()
	{
		levels.clear();
		pending.clear();
	}

	public void onStatChanged(StatChanged event)
	{
		final Skill skill = event.getSkill();
		if (skill == Skill.OVERALL)
		{
			return;
		}

		final Integer previous = levels.put(skill, event.getLevel());
		if (previous != null && event.getLevel() > previous)
		{
			pending.put(skill.getName(), event.getLevel());
		}
	}

	/** @return the LEVEL `extra` payload once a level-up is pending, or null */
	public JsonObject onGameTick(Client client)
	{
		if (pending.isEmpty())
		{
			return null;
		}

		final JsonObject levelledSkills = new JsonObject();
		for (Map.Entry<String, Integer> entry : pending.entrySet())
		{
			levelledSkills.addProperty(entry.getKey(), entry.getValue());
		}
		pending.clear();

		final JsonObject allSkills = new JsonObject();
		for (Skill skill : Skill.values())
		{
			if (skill != Skill.OVERALL)
			{
				allSkills.addProperty(skill.getName(), client.getRealSkillLevel(skill));
			}
		}

		final Player local = client.getLocalPlayer();
		final JsonObject combatLevel = new JsonObject();
		combatLevel.addProperty("value", local == null ? 0 : local.getCombatLevel());

		final JsonObject extra = new JsonObject();
		extra.add("levelledSkills", levelledSkills);
		extra.add("allSkills", allSkills);
		extra.add("combatLevel", combatLevel);
		return extra;
	}
}
