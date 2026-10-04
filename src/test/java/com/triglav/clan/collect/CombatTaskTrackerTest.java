package com.triglav.clan.collect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import org.junit.Before;
import org.junit.Test;

public class CombatTaskTrackerTest
{
	private final CombatTaskTracker tracker = new CombatTaskTracker();
	private Client client;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		// Reward thresholds as the client reports them.
		when(client.getVarbitValue(VarbitID.CA_THRESHOLD_EASY)).thenReturn(33);
		when(client.getVarbitValue(VarbitID.CA_THRESHOLD_MEDIUM)).thenReturn(115);
		when(client.getVarbitValue(VarbitID.CA_THRESHOLD_HARD)).thenReturn(304);
		when(client.getVarbitValue(VarbitID.CA_THRESHOLD_ELITE)).thenReturn(820);
		when(client.getVarbitValue(VarbitID.CA_THRESHOLD_MASTER)).thenReturn(1465);
		when(client.getVarbitValue(VarbitID.CA_THRESHOLD_GRANDMASTER)).thenReturn(2005);
	}

	private void tasksCompleted(int easy, int medium, int hard, int elite, int master, int grandmaster)
	{
		when(client.getVarbitValue(VarbitID.CA_TOTAL_TASKS_COMPLETED_EASY)).thenReturn(easy);
		when(client.getVarbitValue(VarbitID.CA_TOTAL_TASKS_COMPLETED_MEDIUM)).thenReturn(medium);
		when(client.getVarbitValue(VarbitID.CA_TOTAL_TASKS_COMPLETED_HARD)).thenReturn(hard);
		when(client.getVarbitValue(VarbitID.CA_TOTAL_TASKS_COMPLETED_ELITE)).thenReturn(elite);
		when(client.getVarbitValue(VarbitID.CA_TOTAL_TASKS_COMPLETED_MASTER)).thenReturn(master);
		when(client.getVarbitValue(VarbitID.CA_TOTAL_TASKS_COMPLETED_GRANDMASTER)).thenReturn(grandmaster);
	}

	@Test
	public void ignoresOtherMessages()
	{
		assertNull(tracker.parse(client, "Congratulations, you've completed a quest: Dragon Slayer II."));
		assertNull(tracker.parse(client, "You have completed 80 Master Treasure Trails."));
	}

	@Test
	public void readsTierAndTask()
	{
		tasksCompleted(33, 20, 0, 0, 0, 0); // 33 + 40 = 73 points, past Easy, short of Medium
		final JsonObject task = tracker.parse(client, "Congratulations, you've completed a medium combat task: Lizard Shaman Novice (2 points).");
		assertNotNull(task);
		assertEquals("MEDIUM", task.get("tier").getAsString());
		assertEquals("Lizard Shaman Novice", task.get("task").getAsString());
		assertEquals(73, task.get("totalPoints").getAsInt());
		assertEquals("EASY", task.get("currentTier").getAsString());
		assertFalse(task.has("justCompletedTier"));
	}

	@Test
	public void reportsTheTaskThatUnlockedTheNextTier()
	{
		tasksCompleted(33, 41, 0, 0, 0, 0); // 33 + 82 = 115 points, exactly the Medium threshold
		final JsonObject task = tracker.parse(client, "Congratulations, you've completed a medium combat task: Lizard Shaman Novice.");
		assertNotNull(task);
		assertEquals("MEDIUM", task.get("currentTier").getAsString());
		assertEquals("MEDIUM", task.get("justCompletedTier").getAsString());
	}

	@Test
	public void staysQuietAboutTiersBeforeTheClientHasThresholds()
	{
		when(client.getVarbitValue(VarbitID.CA_THRESHOLD_EASY)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.CA_THRESHOLD_MEDIUM)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.CA_THRESHOLD_HARD)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.CA_THRESHOLD_ELITE)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.CA_THRESHOLD_MASTER)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.CA_THRESHOLD_GRANDMASTER)).thenReturn(0);
		tasksCompleted(10, 0, 0, 0, 0, 0);

		final JsonObject task = tracker.parse(client, "Congratulations, you've completed an easy combat task: Pest Control Novice.");
		assertNotNull(task);
		assertEquals(10, task.get("totalPoints").getAsInt());
		assertFalse(task.has("currentTier"));
		assertFalse(task.has("justCompletedTier"));
	}
}
