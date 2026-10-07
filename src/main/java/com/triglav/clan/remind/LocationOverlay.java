package com.triglav.clan.remind;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** A gold pin on the minimap at the meeting spot, whenever that spot is inside the loaded area. */
@Singleton
public class LocationOverlay extends Overlay
{
	private static final int RADIUS = 5;

	private final Client client;
	private final LocationMarker marker;

	@Inject
	private LocationOverlay(Client client, LocationMarker marker)
	{
		this.client = client;
		this.marker = marker;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		final WorldPoint target = marker.target();
		if (target == null || client.getLocalPlayer() == null || target.getPlane() != client.getPlane())
		{
			return null;
		}

		final LocalPoint local = LocalPoint.fromWorld(client, target);
		if (local == null)
		{
			return null; // outside the loaded scene: the world map marker still shows it
		}

		final Point minimap = Perspective.localToMinimap(client, local);
		if (minimap == null)
		{
			return null;
		}

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.BLACK);
		g.fillOval(minimap.getX() - RADIUS - 1, minimap.getY() - RADIUS - 1, RADIUS * 2 + 2, RADIUS * 2 + 2);
		g.setColor(LocationMarker.GOLD);
		g.fillOval(minimap.getX() - RADIUS, minimap.getY() - RADIUS, RADIUS * 2, RADIUS * 2);
		return null;
	}
}
