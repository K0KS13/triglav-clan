package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.client.game.ItemManager;

/**
 * Approximates DEATH's `valueLost` (site: lestvica smrti) from the equipment+inventory GE value
 * right before and a few ticks after death, once the game has actually removed what was lost.
 * This is the same technique community death-value trackers use; it is an approximation, not
 * exact OSRS death mechanics (protected items, wilderness skull state, PvP looting by the killer
 * are not modelled individually - they just show up as "how much less you're carrying now").
 * `isPvp`/`killerName` come from who the local player was interacting with at the moment of death,
 * which misses deaths from something you were not targeting (poison, ranged from an unseen enemy) -
 * in that case this reports isPvp=false rather than guessing, since a false PvP flag is more
 * visible and more wrong than an unflagged one (site surfaces PvP deaths specially).
 */
@Singleton
public class DeathTracker
{
	private static final int AFTER_DEATH_DELAY_TICKS = 3;

	private final ItemManager itemManager;
	private int ticksUntilCheck;
	private long valueBeforeDeath;
	private boolean pvpAtDeath;
	private String killerAtDeath;

	@Inject
	private DeathTracker(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	/** Call on ActorDeath once it's confirmed to be the local player. */
	public void onLocalPlayerDied(Client client)
	{
		valueBeforeDeath = containerValue(client);
		ticksUntilCheck = AFTER_DEATH_DELAY_TICKS;

		final Actor interacting = client.getLocalPlayer().getInteracting();
		pvpAtDeath = interacting instanceof Player;
		killerAtDeath = pvpAtDeath ? interacting.getName() : null;
	}

	/** @return the DEATH `extra` payload once ready, or null if still waiting / nothing pending */
	public JsonObject onGameTick(Client client)
	{
		if (ticksUntilCheck <= 0)
		{
			return null;
		}

		if (--ticksUntilCheck > 0)
		{
			return null;
		}

		final long lost = Math.max(0, valueBeforeDeath - containerValue(client));
		final JsonObject extra = new JsonObject();
		extra.addProperty("valueLost", lost);
		extra.addProperty("isPvp", pvpAtDeath);
		if (killerAtDeath != null)
		{
			extra.addProperty("killerName", killerAtDeath);
		}
		return extra;
	}

	private long containerValue(Client client)
	{
		return containerValue(client, InventoryID.INVENTORY) + containerValue(client, InventoryID.EQUIPMENT);
	}

	private long containerValue(Client client, InventoryID id)
	{
		final ItemContainer container = client.getItemContainer(id);
		if (container == null)
		{
			return 0;
		}

		long total = 0;
		for (Item item : container.getItems())
		{
			if (item.getId() <= 0)
			{
				continue;
			}
			final int canonicalId = itemManager.canonicalize(item.getId());
			total += (long) itemManager.getItemPrice(canonicalId) * item.getQuantity();
		}
		return total;
	}
}
