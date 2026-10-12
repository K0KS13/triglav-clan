package com.triglav.clan.collect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.VarPlayerID;
import org.junit.Test;

public class TrackersTest
{
	@Test
	public void xpMilestoneNeedsACrossingAfterLogin()
	{
		final Client client = mock(Client.class);
		when(client.getSkillExperience(any(Skill.class))).thenReturn(49_999_000);

		final XpMilestoneTracker tracker = new XpMilestoneTracker();
		tracker.prime(client);

		// The burst of StatChanged right after login carries the same XP: nothing happens.
		tracker.onStatChanged(new StatChanged(Skill.ATTACK, 49_999_000, 99, 99));
		assertNull(tracker.onGameTick());

		tracker.onStatChanged(new StatChanged(Skill.ATTACK, 50_000_010, 99, 99));
		final JsonObject milestone = tracker.onGameTick();
		assertNotNull(milestone);
		assertEquals("Attack", milestone.getAsJsonArray("milestoneAchieved").get(0).getAsString());
		assertEquals(50_000_010, milestone.getAsJsonObject("xpData").get("Attack").getAsInt());
		assertEquals(50_000_000, milestone.get("interval").getAsInt());

		// Reported once, and not again until the next 50M.
		assertNull(tracker.onGameTick());
		tracker.onStatChanged(new StatChanged(Skill.ATTACK, 50_000_500, 99, 99));
		assertNull(tracker.onGameTick());
	}

	@Test
	public void duelArenaStatsAreNotLevelUps()
	{
		final Client client = mock(Client.class);
		when(client.getRealSkillLevel(any(Skill.class))).thenReturn(70);

		final LevelTracker tracker = new LevelTracker();
		tracker.prime(client);

		// The arena sets every skill to 99 at once; leaving restores 70. Neither is a level-up.
		tracker.onStatChanged(new StatChanged(Skill.ATTACK, 13_034_431, 99, 99));
		tracker.onStatChanged(new StatChanged(Skill.MINING, 13_034_431, 99, 99));
		tracker.onStatChanged(new StatChanged(Skill.COOKING, 13_034_431, 99, 99));
		assertNull(tracker.onGameTick(client));
		tracker.onStatChanged(new StatChanged(Skill.ATTACK, 777_892, 70, 70));
		assertNull(tracker.onGameTick(client));

		// A lamp on one skill is real even when it jumps many levels: 70 -> 99 is a 99.
		tracker.onStatChanged(new StatChanged(Skill.SMITHING, 13_034_431, 99, 99));
		final JsonObject lamp = tracker.onGameTick(client);
		assertNotNull(lamp);
		assertEquals(99, lamp.getAsJsonObject("levelledSkills").get("Smithing").getAsInt());

		// A normal level-up still counts.
		tracker.onStatChanged(new StatChanged(Skill.ATTACK, 800_000, 71, 71));
		assertNotNull(tracker.onGameTick(client));
	}

	@Test
	public void duelArenaXpIsNotAMilestone()
	{
		final Client client = mock(Client.class);
		when(client.getSkillExperience(any(Skill.class))).thenReturn(777_000);

		final XpMilestoneTracker tracker = new XpMilestoneTracker();
		tracker.prime(client);
		for (Skill skill : new Skill[] {Skill.ATTACK, Skill.MINING, Skill.COOKING}) // many skills jump together
		{
			tracker.onStatChanged(new StatChanged(skill, 80_000_000, 99, 99));
		}
		assertNull(tracker.onGameTick());

		// One skill jumping past 50M by a big gain is real (e.g. a lamp).
		tracker.onStatChanged(new StatChanged(Skill.FISHING, 60_000_000, 99, 99));
		assertNotNull(tracker.onGameTick());
	}

	@Test
	public void collectionLogTotalsStayQuietUntilKnown()
	{
		final Client client = mock(Client.class);
		assertNull(CollectionLogTotals.snapshot(client)); // varps read 0 before the client has them

		when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT)).thenReturn(612);
		when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX)).thenReturn(1_699);
		final JsonObject totals = CollectionLogTotals.snapshot(client);
		assertNotNull(totals);
		assertEquals(612, totals.get("completed").getAsInt());
		assertEquals(1699, totals.get("total").getAsInt());

		// A count above the maximum is a half-loaded read, not a result.
		when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT)).thenReturn(2_000);
		assertNull(CollectionLogTotals.snapshot(client));
	}

	@Test
	public void partialQuestStepsAreNotQuests()
	{
		final QuestTracker tracker = new QuestTracker();
		assertEquals(true, tracker.isPartial("You have kind of completed Recipe for Disaster!"));
		assertEquals(false, tracker.isPartial("You have completed The Corsair Curse!"));
		assertEquals("Skrach Uglogwee", QuestTracker.normalise("Skrach 'Bone Crusher' Uglogwee", "x"));
	}
}
