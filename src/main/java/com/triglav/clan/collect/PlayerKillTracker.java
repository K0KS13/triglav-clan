package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.WorldType;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemManager;

/**
 * A kill counts as ours when we damaged the player and they died shortly after - the same signal
 * Dink uses, since the game tells no one who landed the last hit. The victim's risk is their worn
 * equipment at that moment, read from their appearance (site: src/lib/dink/parse.ts sums
 * `victimEquipment`).
 *
 * Only the wilderness and real PvP worlds are reported. Safe minigames (Castle Wars, Soul Wars,
 * LMS, PvP Arena) would otherwise flood the clan's PK channel with kills that risked nothing.
 */
@Singleton
public class PlayerKillTracker
{
	/** Worn gear only; HAIR and JAW are appearance kits, never items. */
	private static final KitType[] EQUIPMENT_SLOTS;

	private static final long REPEAT_WINDOW_MILLIS = 5_000;

	static
	{
		final List<KitType> slots = new ArrayList<>();
		for (KitType kit : KitType.values())
		{
			if (kit != KitType.HAIR && kit != KitType.JAW)
			{
				slots.add(kit);
			}
		}
		EQUIPMENT_SLOTS = slots.toArray(new KitType[0]);
	}

	private final ItemManager itemManager;

	/** Weak keys: a player who despawns must not be kept alive by this map. */
	private final Map<Player, Integer> damaged = new WeakHashMap<>(4);
	private final Map<String, Long> recentlyReported = new HashMap<>(4);

	@Inject
	private PlayerKillTracker(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	public void reset()
	{
		damaged.clear();
		recentlyReported.clear();
	}

	public void onHitsplat(Client client, HitsplatApplied event)
	{
		final Hitsplat hitsplat = event.getHitsplat();
		if (hitsplat.getAmount() <= 0 || !hitsplat.isMine())
		{
			return;
		}

		final Actor actor = event.getActor();
		if (!(actor instanceof Player) || actor == client.getLocalPlayer())
		{
			return;
		}

		damaged.merge((Player) actor, hitsplat.getAmount(), Integer::sum);
	}

	/** @return one PLAYER_KILL `extra` payload per player we damaged who just died, if any */
	public List<JsonObject> onGameTick(Client client)
	{
		if (damaged.isEmpty())
		{
			return null;
		}

		List<JsonObject> kills = null;
		if (isPvpSituation(client))
		{
			final long now = System.currentTimeMillis();
			recentlyReported.values().removeIf(at -> now - at > REPEAT_WINDOW_MILLIS);

			for (Player victim : damaged.keySet())
			{
				final String name = victim.getName();
				// A multi-tick special attack can land after the kill; report each victim once.
				if (!victim.isDead() || name == null || recentlyReported.containsKey(name))
				{
					continue;
				}

				recentlyReported.put(name, now);
				if (kills == null)
				{
					kills = new ArrayList<>(1);
				}
				kills.add(build(client, victim, name));
			}
		}

		damaged.clear();
		return kills;
	}

	private JsonObject build(Client client, Player victim, String name)
	{
		final JsonObject extra = new JsonObject();
		extra.addProperty("victimName", name);
		extra.addProperty("victimCombatLevel", victim.getCombatLevel());
		extra.addProperty("world", client.getWorld());

		final JsonObject equipment = new JsonObject();
		final PlayerComposition appearance = victim.getPlayerComposition();
		if (appearance != null)
		{
			for (KitType slot : EQUIPMENT_SLOTS)
			{
				final int itemId = appearance.getEquipmentId(slot);
				if (itemId < 0)
				{
					continue;
				}

				final int canonicalId = itemManager.canonicalize(itemId);
				final JsonObject item = new JsonObject();
				item.addProperty("id", canonicalId);
				item.addProperty("name", itemManager.getItemComposition(canonicalId).getName());
				item.addProperty("quantity", 1);
				item.addProperty("priceEach", itemManager.getItemPrice(canonicalId));
				equipment.add(slot.name(), item);
			}
		}
		extra.add("victimEquipment", equipment);

		return extra;
	}

	private static boolean isPvpSituation(Client client)
	{
		final Set<WorldType> world = client.getWorldType();
		if (world.contains(WorldType.LAST_MAN_STANDING)
			|| world.contains(WorldType.TOURNAMENT_WORLD)
			|| world.contains(WorldType.PVP_ARENA))
		{
			return false;
		}

		return client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1
			|| world.contains(WorldType.PVP)
			|| world.contains(WorldType.BOUNTY)
			|| world.contains(WorldType.HIGH_RISK)
			|| world.contains(WorldType.DEADMAN);
	}
}
