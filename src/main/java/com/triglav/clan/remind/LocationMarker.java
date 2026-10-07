package com.triglav.clan.remind;

import com.triglav.clan.overview.Overview;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * Where the group meets, shown on the world map (always, with the place name on hover and a click
 * that jumps the map to it) and as a pin on the minimap while the spot is in view
 * (see {@link LocationOverlay}). One marker at a time: the one for the box that is showing.
 */
@Singleton
public class LocationMarker
{
	static final Color GOLD = new Color(0xd8a93f);

	private final WorldMapPointManager worldMapPointManager;
	private final BufferedImage pin = pinImage();

	private WorldMapPoint point;
	private volatile WorldPoint target;
	private Overview.Loc shown;
	private String shownLabel;

	@Inject
	private LocationMarker(WorldMapPointManager worldMapPointManager)
	{
		this.worldMapPointManager = worldMapPointManager;
	}

	/** @return where the minimap pin goes, or null when there is nothing to show */
	WorldPoint target()
	{
		return target;
	}

	/** Game thread. Re-sets only when the location actually changed. */
	void show(Overview.Loc loc, String label)
	{
		if (loc == null)
		{
			clear();
			return;
		}

		if (shown != null && shown.x == loc.x && shown.y == loc.y && shown.plane == loc.plane && label.equals(shownLabel))
		{
			return;
		}

		clear();
		shown = loc;
		shownLabel = label;
		target = new WorldPoint(loc.x, loc.y, loc.plane);
		point = WorldMapPoint.builder()
			.worldPoint(target)
			.image(pin)
			.tooltip(label + " · " + loc.label())
			.snapToEdge(true)
			.jumpOnClick(true)
			.build();
		worldMapPointManager.add(point);
	}

	void clear()
	{
		if (point != null)
		{
			worldMapPointManager.remove(point);
			point = null;
		}
		target = null;
		shown = null;
		shownLabel = null;
	}

	private static BufferedImage pinImage()
	{
		final BufferedImage image = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.BLACK);
		g.fillOval(1, 1, 16, 16);
		g.setColor(GOLD);
		g.fillOval(3, 3, 12, 12);
		g.setColor(Color.BLACK);
		g.fillOval(7, 7, 4, 4);
		g.dispose();
		return image;
	}
}
