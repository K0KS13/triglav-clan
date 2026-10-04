package com.triglav.clan.collect;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;

/**
 * Builds the `extra` block of a LOOT event (site: docs/plugin-brief.md §2, src/lib/dink/parse.ts).
 * `rarity` and `party` are left out - the site treats both as optional and we have no reliable
 * source for either without the drop-rate tables Dink ships.
 */
@Singleton
public class LootCollector
{
	private final ItemManager itemManager;
	private final KillCountTracker killCountTracker;

	@Inject
	private LootCollector(ItemManager itemManager, KillCountTracker killCountTracker)
	{
		this.itemManager = itemManager;
		this.killCountTracker = killCountTracker;
	}

	public JsonObject build(NpcLootReceived event)
	{
		final String npcName = event.getNpc().getName() == null ? "Unknown" : event.getNpc().getName();
		final JsonObject extra = build(npcName, "NPC", event.getItems());

		final int killCount = killCountTracker.lastCount(npcName);
		if (killCount >= 0)
		{
			extra.addProperty("killCount", killCount);
		}

		return extra;
	}

	/**
	 * Loot that isn't a plain NPC drop: raid chests, clue caskets, implings, pickpocketing.
	 * The site shows these in the drop feed exactly like NPC loot, with `category` telling them apart.
	 */
	public JsonObject build(String source, String category, Collection<ItemStack> stacks)
	{
		final JsonArray items = new JsonArray();
		for (ItemStack stack : stacks)
		{
			final int canonicalId = itemManager.canonicalize(stack.getId());
			final JsonObject item = new JsonObject();
			item.addProperty("id", canonicalId);
			item.addProperty("name", itemManager.getItemComposition(canonicalId).getName());
			item.addProperty("quantity", stack.getQuantity());
			item.addProperty("priceEach", itemManager.getItemPrice(canonicalId));
			items.add(item);
		}

		final JsonObject extra = new JsonObject();
		extra.add("items", items);
		extra.addProperty("source", source == null || source.isEmpty() ? "Unknown" : source);
		extra.addProperty("category", category);
		return extra;
	}
}
