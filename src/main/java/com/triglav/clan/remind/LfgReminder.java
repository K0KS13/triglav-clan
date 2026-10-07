package com.triglav.clan.remind;

import com.triglav.clan.TriglavConfig;
import com.triglav.clan.overview.Overview;
import com.triglav.clan.overview.OverviewClient;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

/**
 * An LFG the member joined: a chat reminder ten minutes before, the setup check against the post's
 * recommended gear, and a countdown next to the minimap from half an hour out. Everything is driven
 * by the game tick (one cheap clock comparison per tick) off the overview snapshot, so no extra
 * request is made. Each LFG is reminded once per session.
 */
@Singleton
public class LfgReminder
{
	private static final long REMIND_MINUTES = 10;
	private static final long INFOBOX_MINUTES = 30;
	private static final long LINGER_MINUTES = 5;
	private static final int MAX_LISTED = 5;

	private final Client client;
	private final ItemManager itemManager;
	private final InfoBoxManager infoBoxManager;
	private final OverviewClient overviewClient;
	private final TriglavConfig config;
	private final Set<String> reminded = new HashSet<>();
	private final LocationMarker marker;
	private final BufferedImage pluginIcon;
	private final IconCache iconCache;

	private LfgInfoBox infoBox;
	private BufferedImage infoBoxImage;

	@Inject
	private LfgReminder(Client client, ItemManager itemManager, InfoBoxManager infoBoxManager, OverviewClient overviewClient, TriglavConfig config,
		LocationMarker marker, IconCache iconCache)
	{
		this.marker = marker;
		this.iconCache = iconCache;
		this.pluginIcon = ImageUtil.resizeImage(ImageUtil.loadImageResource(LfgReminder.class, "/icon.png"), 32, 32);
		this.client = client;
		this.itemManager = itemManager;
		this.infoBoxManager = infoBoxManager;
		this.overviewClient = overviewClient;
		this.config = config;
	}

	/** Game thread. */
	public void onGameTick(Plugin plugin, Consumer<String> chat)
	{
		if (!config.lfgReminders())
		{
			removeInfoBox();
			return;
		}

		final Instant now = Instant.now();
		String nextKind = null;
		String nextTitle = null;
		Instant nextStart = null;
		Instant nextEnd = null;
		Overview.Loc nextLoc = null;
		String nextIcon = null;

		for (Overview.MyLfg lfg : overviewClient.current().myLfg)
		{
			final long minutes = Duration.between(now, lfg.startsAt).toMinutes();
			if (minutes < -LINGER_MINUTES)
			{
				continue;
			}

			if (nextStart == null || lfg.startsAt.isBefore(nextStart))
			{
				nextKind = "LFG";
				nextTitle = lfg.title;
				nextStart = lfg.startsAt;
				nextEnd = null;
				nextLoc = lfg.loc;
				nextIcon = lfg.icon;
			}

			if (minutes <= REMIND_MINUTES && reminded.add("lfg:" + lfg.id))
			{
				chat.accept(minutes <= 0 ? "LFG »" + lfg.title + "« se zacenja zdaj." : "LFG »" + lfg.title + "« se zacne cez " + Math.max(1, minutes) + " min.");
				if (lfg.loc != null)
				{
					chat.accept("lokacija: " + lfg.loc.label() + " (" + lfg.loc.x + ", " + lfg.loc.y + ") - oznaceno na zemljevidu.");
				}
				if (lfg.hasGear())
				{
					checkGear(lfg, chat);
				}
			}
		}

		// Clan events: ones the member is going to get the reminder; every running event (a BOTW or SOTW that
		// is on right now) shows in the box, since a competition is clan-wide whether or not you signed up.
		for (Overview.Event event : overviewClient.current().events)
		{
			final long minutes = Duration.between(now, event.startsAt).toMinutes();
			final boolean running = minutes <= 0 && (event.endsAt == null ? minutes >= -LINGER_MINUTES : now.isBefore(event.endsAt));
			if (!running && minutes < 0)
			{
				continue;
			}

			if (event.mine && minutes <= REMIND_MINUTES && reminded.add("event:" + event.id))
			{
				chat.accept(minutes <= 0 ? "Dogodek »" + event.title + "« se zacenja zdaj." : "Dogodek »" + event.title + "« se zacne cez " + Math.max(1, minutes) + " min.");
				if (event.loc != null)
				{
					chat.accept("lokacija: " + event.loc.label() + " (" + event.loc.x + ", " + event.loc.y + ") - oznaceno na zemljevidu.");
				}
			}

			final boolean show = running || (event.mine && minutes <= INFOBOX_MINUTES);
			if (show && (nextStart == null || running || event.startsAt.isBefore(nextStart)))
			{
				nextKind = "Dogodek";
				nextTitle = event.title;
				nextStart = event.startsAt;
				nextEnd = event.endsAt;
				nextLoc = event.loc;
				nextIcon = event.icon;
			}
		}

		final boolean showBox = nextStart != null
			&& (nextEnd != null && !now.isBefore(nextStart) || Duration.between(now, nextStart).toMinutes() <= INFOBOX_MINUTES);
		if (showBox)
		{
			showInfoBox(plugin, nextKind, nextTitle, nextStart, nextEnd, nextLoc, nextIcon);
			marker.show(nextLoc, nextTitle);
		}
		else
		{
			removeInfoBox();
		}
	}

	/** The soonest joined LFG that has a recommended setup, or null. */
	public Overview.MyLfg nextWithGear()
	{
		final Instant now = Instant.now();
		Overview.MyLfg best = null;
		for (Overview.MyLfg lfg : overviewClient.current().myLfg)
		{
			if (lfg.hasGear() && Duration.between(now, lfg.startsAt).toMinutes() >= -LINGER_MINUTES
				&& (best == null || lfg.startsAt.isBefore(best.startsAt)))
			{
				best = lfg;
			}
		}
		return best;
	}

	/** Game thread: reads inventory and worn equipment. */
	public void checkGear(Overview.MyLfg lfg, Consumer<String> chat)
	{
		final Map<Integer, Integer> have = new HashMap<>();
		count(have, client.getItemContainer(InventoryID.INVENTORY));
		count(have, client.getItemContainer(InventoryID.EQUIPMENT));

		final List<String> missing = GearCheck.missing(lfg, have, itemManager::canonicalize);
		if (missing.isEmpty())
		{
			chat.accept("oprema za »" + lfg.title + "« (" + lfg.gearTitle + ") je v redu.");
			return;
		}

		final List<String> shown = missing.size() > MAX_LISTED ? missing.subList(0, MAX_LISTED) : missing;
		chat.accept("za »" + lfg.title + "« manjka: " + String.join(", ", shown)
			+ (missing.size() > MAX_LISTED ? " in se " + (missing.size() - MAX_LISTED) + " drugega" : "") + ".");
	}

	public void shutDown()
	{
		removeInfoBox();
		reminded.clear();
	}

	private void count(Map<Integer, Integer> have, ItemContainer container)
	{
		if (container == null)
		{
			return;
		}

		for (Item item : container.getItems())
		{
			if (item.getId() > 0)
			{
				have.merge(itemManager.canonicalize(item.getId()), item.getQuantity(), Integer::sum);
			}
		}
	}

	private void showInfoBox(Plugin plugin, String kind, String title, Instant startsAt, Instant endsAt, Overview.Loc loc, String iconUrl)
	{
		// The boss, skill or event-type icon from the site once it has arrived; the plugin icon until then.
		final BufferedImage wanted = iconCache.get(iconUrl) != null ? iconCache.get(iconUrl) : pluginIcon;
		if (infoBox == null)
		{
			infoBox = new LfgInfoBox(wanted, plugin);
			infoBoxImage = wanted;
			infoBoxManager.addInfoBox(infoBox);
		}
		else if (infoBoxImage != wanted)
		{
			infoBox.setImage(wanted);
			infoBoxImage = wanted;
		}
		infoBox.update(kind, title, startsAt, endsAt, loc == null ? null : loc.label());
	}

	private void removeInfoBox()
	{
		marker.clear();
		if (infoBox != null)
		{
			infoBoxManager.removeInfoBox(infoBox);
			infoBox = null;
		}
	}
}
