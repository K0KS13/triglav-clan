package com.triglav.clan.remind;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

/** Small countdown next to the minimap while an LFG the member joined is about to start. */
final class LfgInfoBox extends InfoBox
{
	private volatile Instant startsAt = Instant.EPOCH;
	private volatile String title = "";

	LfgInfoBox(BufferedImage image, Plugin plugin)
	{
		super(image, plugin);
	}

	void update(String title, Instant startsAt)
	{
		this.title = title;
		this.startsAt = startsAt;
	}

	@Override
	public String getText()
	{
		final long minutes = Duration.between(Instant.now(), startsAt).toMinutes();
		return minutes <= 0 ? "zdaj" : minutes + "m";
	}

	@Override
	public Color getTextColor()
	{
		return Duration.between(Instant.now(), startsAt).toMinutes() <= 5 ? Color.ORANGE : Color.WHITE;
	}

	@Override
	public String getTooltip()
	{
		return "LFG: " + title;
	}
}
