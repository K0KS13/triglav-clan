package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.Varbits;

/**
 * `achievementDiary` block (tier completed/total, site: src/lib/dink/snapshot.ts). Every diary
 * tier is its own boolean varbit in {@link Varbits}. The list is written out by hand (Plugin Hub
 * does not allow reflection), so a new diary area needs a line added here.
 * <p>
 * This is tier-level completion, not the per-task breakdown (`achievementDiaryTasks` in the site's
 * type) — task-level state has no equivalent varbit family (see DECISIONS.md).
 */
public final class DiarySnapshot
{
	private static final int[] TIER_IDS = {
		Varbits.DIARY_ARDOUGNE_EASY, Varbits.DIARY_ARDOUGNE_MEDIUM, Varbits.DIARY_ARDOUGNE_HARD, Varbits.DIARY_ARDOUGNE_ELITE,
		Varbits.DIARY_DESERT_EASY, Varbits.DIARY_DESERT_MEDIUM, Varbits.DIARY_DESERT_HARD, Varbits.DIARY_DESERT_ELITE,
		Varbits.DIARY_FALADOR_EASY, Varbits.DIARY_FALADOR_MEDIUM, Varbits.DIARY_FALADOR_HARD, Varbits.DIARY_FALADOR_ELITE,
		Varbits.DIARY_FREMENNIK_EASY, Varbits.DIARY_FREMENNIK_MEDIUM, Varbits.DIARY_FREMENNIK_HARD, Varbits.DIARY_FREMENNIK_ELITE,
		Varbits.DIARY_KANDARIN_EASY, Varbits.DIARY_KANDARIN_MEDIUM, Varbits.DIARY_KANDARIN_HARD, Varbits.DIARY_KANDARIN_ELITE,
		Varbits.DIARY_KARAMJA_EASY, Varbits.DIARY_KARAMJA_MEDIUM, Varbits.DIARY_KARAMJA_HARD, Varbits.DIARY_KARAMJA_ELITE,
		Varbits.DIARY_KOUREND_EASY, Varbits.DIARY_KOUREND_MEDIUM, Varbits.DIARY_KOUREND_HARD, Varbits.DIARY_KOUREND_ELITE,
		Varbits.DIARY_LUMBRIDGE_EASY, Varbits.DIARY_LUMBRIDGE_MEDIUM, Varbits.DIARY_LUMBRIDGE_HARD, Varbits.DIARY_LUMBRIDGE_ELITE,
		Varbits.DIARY_MORYTANIA_EASY, Varbits.DIARY_MORYTANIA_MEDIUM, Varbits.DIARY_MORYTANIA_HARD, Varbits.DIARY_MORYTANIA_ELITE,
		Varbits.DIARY_VARROCK_EASY, Varbits.DIARY_VARROCK_MEDIUM, Varbits.DIARY_VARROCK_HARD, Varbits.DIARY_VARROCK_ELITE,
		Varbits.DIARY_WESTERN_EASY, Varbits.DIARY_WESTERN_MEDIUM, Varbits.DIARY_WESTERN_HARD, Varbits.DIARY_WESTERN_ELITE,
		Varbits.DIARY_WILDERNESS_EASY, Varbits.DIARY_WILDERNESS_MEDIUM, Varbits.DIARY_WILDERNESS_HARD, Varbits.DIARY_WILDERNESS_ELITE,
	};

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
}
