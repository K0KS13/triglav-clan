package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Builds the `skills` block of a Dink-style LOGIN snapshot (site: src/lib/dink/snapshot.ts).
 * The site expects virtual levels (up to 127 at 200M xp) per skill and a real, 99-capped
 * total level (max 2376 across every skill, Sailing included).
 */
public final class SkillSnapshot
{
	private SkillSnapshot()
	{
	}

	public static JsonObject build(Client client)
	{
		final JsonObject levels = new JsonObject();
		final JsonObject experience = new JsonObject();
		int totalLevel = 0;

		for (Skill skill : Skill.values())
		{
			if (skill == Skill.OVERALL)
			{
				continue;
			}

			final int xp = client.getSkillExperience(skill);
			totalLevel += client.getRealSkillLevel(skill);
			levels.addProperty(skill.getName(), Experience.getLevelForXp(xp));
			experience.addProperty(skill.getName(), xp);
		}

		final JsonObject skills = new JsonObject();
		skills.add("levels", levels);
		skills.add("experience", experience);
		skills.addProperty("totalLevel", totalLevel);
		skills.addProperty("totalExperience", client.getOverallExperience());
		return skills;
	}
}
