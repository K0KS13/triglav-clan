package com.triglav.clan.remind;

import com.triglav.clan.overview.Overview;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Compares what the member is carrying with the setup an LFG recommends. Equipment and inventory
 * are checked together: a setup that wants a whip equipped is satisfied by a whip in the inventory
 * too, since the point is "did I bring it", not "is it on". Item ids are canonicalised on both
 * sides so noted/placeholder variants match.
 */
public final class GearCheck
{
	private GearCheck()
	{
	}

	/** @param have canonical item id -> total quantity across inventory and worn equipment */
	public static List<String> missing(Overview.MyLfg lfg, Map<Integer, Integer> have, IntUnaryOperator canonical)
	{
		final Map<Integer, Integer> needed = new LinkedHashMap<>();
		final Map<Integer, String> names = new LinkedHashMap<>();
		for (Overview.GearItem item : lfg.equipment)
		{
			need(needed, names, canonical.applyAsInt(item.id), item.name, 1);
		}
		for (Overview.GearItem item : lfg.inventory)
		{
			need(needed, names, canonical.applyAsInt(item.id), item.name, item.quantity);
		}

		final List<String> out = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : needed.entrySet())
		{
			final int missing = entry.getValue() - have.getOrDefault(entry.getKey(), 0);
			if (missing > 0)
			{
				out.add(missing > 1 ? names.get(entry.getKey()) + " x" + missing : names.get(entry.getKey()));
			}
		}
		return out;
	}

	private static void need(Map<Integer, Integer> needed, Map<Integer, String> names, int id, String name, int quantity)
	{
		needed.merge(id, quantity, Integer::sum);
		names.putIfAbsent(id, name);
	}
}
