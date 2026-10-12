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
	/**
	 * The Duel Arena temporarily sets every skill to 99 (level and XP) and restores the real values afterwards. That shows as many
	 * skills jumping a lot on the same tick. A lamp can also jump one skill by several levels (that is real), so a big jump is only
	 * dropped when at least this many skills jump together.
	 */
	private static final int BIG_JUMP = 5;
	private static final int BULK_SKILLS = 3;

	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
	private final Map<String, Integer> pending = new LinkedHashMap<>();
	/** level-ups of BIG_JUMP or more levels seen this tick; kept unless a bulk of skills jumped together */
	private final Map<String, Integer> bigJumps = new LinkedHashMap<>();

	/** Called with the LOGIN snapshot: what we see now is the baseline, not a level-up. */
	public void prime(Client client)
	{
		levels.clear();
		pending.clear();
		bigJumps.clear();
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
		bigJumps.clear();
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
			(event.getLevel() - previous >= BIG_JUMP ? bigJumps : pending).put(skill.getName(), event.getLevel());
		}
	}

	/** @return the LEVEL `extra` payload once a level-up is pending, or null */
	public JsonObject onGameTick(Client client)
	{
		if (bigJumps.size() < BULK_SKILLS)
		{
			pending.putAll(bigJumps);
		}
		bigJumps.clear();

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
