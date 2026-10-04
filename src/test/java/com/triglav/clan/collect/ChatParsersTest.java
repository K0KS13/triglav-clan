package com.triglav.clan.collect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.gson.JsonObject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import org.junit.Test;

public class ChatParsersTest
{
	private static ChatMessage message(String text)
	{
		return new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "", text, "", 0);
	}

	@Test
	public void readsCompletedClues()
	{
		final JsonObject clue = ClueChat.parse(message("You have completed 1,234 Master Treasure Trails."));
		assertNotNull(clue);
		assertEquals("Master", clue.get("clueType").getAsString());
		assertEquals(1234, clue.get("numberCompleted").getAsInt());

		assertEquals(1, ClueChat.parse(message("You have completed 1 Beginner Treasure Trail.")).get("numberCompleted").getAsInt());
		assertNull(ClueChat.parse(message("You have completed your task! You killed 134 Hellhounds.")));
	}

	@Test
	public void slayerNeedsBothMessages()
	{
		final SlayerChat slayer = new SlayerChat();
		assertNull(slayer.parse(message("You have completed your task! You killed 134 Hellhounds.")));

		final JsonObject done = slayer.parse(message(
			"You've completed 342 tasks and received 15 points, giving you a total of 1,234; return to a Slayer master."));
		assertNotNull(done);
		assertEquals("134 Hellhounds", done.get("slayerTask").getAsString());
		assertEquals("342", done.get("slayerCompleted").getAsString());
		assertEquals("15", done.get("slayerPoints").getAsString());

		// The task name is consumed, so the next completion doesn't reuse it.
		final JsonObject next = slayer.parse(message("You've completed 343 tasks and received 15 points, giving you a total of 1,249."));
		assertNotNull(next);
		assertNull(next.get("slayerTask"));
	}

	@Test
	public void killCountWaitsForTheGameTick()
	{
		final KillCountTracker tracker = new KillCountTracker();
		assertNull(tracker.onGameTick());

		tracker.onChatMessage(message("Your Zulrah kill count is: 1,021."));
		final JsonObject kill = tracker.onGameTick();
		assertNotNull(kill);
		assertEquals("Zulrah", kill.get("boss").getAsString());
		assertEquals(1021, kill.get("count").getAsInt());
		assertNull(kill.get("time"));
		assertEquals(1021, tracker.lastCount("zulrah"));

		// Nothing is left over for the next kill.
		assertNull(tracker.onGameTick());
	}

	@Test
	public void killCountCarriesDurationAndPersonalBest()
	{
		final KillCountTracker tracker = new KillCountTracker();
		tracker.onChatMessage(message("Fight duration: <col=ff0000>1:34.20</col>. Personal best: 1:20.40"));
		tracker.onChatMessage(message("Your Vorkath kill count is: 300."));

		final JsonObject kill = tracker.onGameTick();
		assertNotNull(kill);
		assertEquals("PT1M34.2S", kill.get("time").getAsString());
		assertEquals("PT1M20.4S", kill.get("personalBest").getAsString());
		assertEquals(false, kill.get("isPersonalBest").getAsBoolean());
	}

	@Test
	public void killCountMarksANewPersonalBest()
	{
		final KillCountTracker tracker = new KillCountTracker();
		tracker.onChatMessage(message("Fight duration: <col=ff0000>1:02:03</col> (new personal best)."));
		tracker.onChatMessage(message("Your Chambers of Xeric completion count is: 42."));

		final JsonObject kill = tracker.onGameTick();
		assertNotNull(kill);
		assertEquals("Chambers of Xeric", kill.get("boss").getAsString());
		assertEquals("PT1H2M3S", kill.get("time").getAsString());
		assertEquals("PT1H2M3S", kill.get("personalBest").getAsString());
		assertEquals(true, kill.get("isPersonalBest").getAsBoolean());
	}

	@Test
	public void killCountForgetsADurationWithNoBoss()
	{
		final KillCountTracker tracker = new KillCountTracker();
		tracker.onChatMessage(message("Fight duration: 1:34.20. Personal best: 1:20.40"));
		for (int tick = 0; tick < 11; tick++)
		{
			assertNull(tracker.onGameTick());
		}

		tracker.onChatMessage(message("Your Zulrah kill count is: 5."));
		final JsonObject kill = tracker.onGameTick();
		assertNotNull(kill);
		assertNull(kill.get("time"));
	}
}
