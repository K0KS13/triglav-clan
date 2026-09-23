package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Varbits;

/**
 * `achievementDiary` block (tier completed/total, site: src/lib/dink/snapshot.ts). Every diary
 * tier is its own boolean varbit named e.g. `DIARY_ARDOUGNE_MEDIUM` in the stable, hand-maintained
 * {@link Varbits} class, so the tier list is read via reflection instead of copied into this file
 * by hand — it then keeps up automatically when a new diary area ships.
 * <p>
 * This is tier-level completion, not the per-task breakdown (`achievementDiaryTasks` in the site's
 * type) — task-level state has no equivalent named-varbit family and would need the same widget
 * scraping Dink does, which is not implemented here (see DECISIONS.md).
 */
@Slf4j
public final class DiarySnapshot
{
	private static final Pattern DIARY_TIER = Pattern.compile("^DIARY_.+_(EASY|MEDIUM|HARD|ELITE)$");
	private static final int[] TIER_IDS = resolveTierVarbitIds();

	private DiarySnapshot()
	{
	}

	public static JsonObject build(Client client)
	{
		int completed = 0;
		for (int id : TIER_IDS)
		{
			if (client.getVarbitValue(id) != 0)
			{
				completed++;
			}
		}

		final JsonObject diary = new JsonObject();
		diary.addProperty("completed", completed);
		diary.addProperty("total", TIER_IDS.length);
		return diary;
	}

	private static int[] resolveTierVarbitIds()
	{
		final java.util.List<Integer> ids = new java.util.ArrayList<>();
		for (Field field : Varbits.class.getFields())
		{
			if (Modifier.isStatic(field.getModifiers()) && field.getType() == int.class && DIARY_TIER.matcher(field.getName()).matches())
			{
				try
				{
					ids.add(field.getInt(null));
				}
				catch (IllegalAccessException e)
				{
					log.warn("Could not read {}", field.getName(), e);
				}
			}
		}
		return ids.stream().mapToInt(Integer::intValue).toArray();
	}
}
