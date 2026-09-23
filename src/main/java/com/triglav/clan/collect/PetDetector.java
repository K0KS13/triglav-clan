package com.triglav.clan.collect;

import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.util.Text;

/**
 * "You have a funny feeling like you're being followed" never carries the pet's name, and there
 * is no reliable API for a full pet inventory (Dink gets that from the Character Summary widget).
 * The name is instead read off whichever NPC spawns next to the player in the few ticks after the
 * message - real OSRS always spawns the new pet right at the player's tile. If nothing spawns
 * close enough in time, nothing is reported: a generic name would collide with every other pet
 * under one achievement key on the site (see DECISIONS.md), which is worse than staying silent.
 */
@Singleton
public class PetDetector
{
	private static final String FUNNY_FEELING = "you have a funny feeling like you're being followed";
	private static final int ARM_TICKS = 5;

	private int armedTicksLeft;

	public void onChatMessage(ChatMessage event)
	{
		if (Text.removeTags(event.getMessage()).toLowerCase().contains(FUNNY_FEELING))
		{
			armedTicksLeft = ARM_TICKS;
		}
	}

	public void onGameTick()
	{
		if (armedTicksLeft > 0)
		{
			armedTicksLeft--;
		}
	}

	/** @return the pet's name if this spawn looks like the pet that was just unlocked, else null */
	public String onNpcSpawned(Client client, NpcSpawned event)
	{
		if (armedTicksLeft <= 0)
		{
			return null;
		}

		final NPC npc = event.getNpc();
		final Player local = client.getLocalPlayer();
		if (npc.getName() == null || local == null)
		{
			return null;
		}

		final WorldPoint npcPoint = npc.getWorldLocation();
		final WorldPoint playerPoint = local.getWorldLocation();
		if (npcPoint == null || playerPoint == null || npcPoint.distanceTo(playerPoint) > 1)
		{
			return null;
		}

		armedTicksLeft = 0;
		return npc.getName();
	}
}
