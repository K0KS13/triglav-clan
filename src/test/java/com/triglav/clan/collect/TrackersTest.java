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
