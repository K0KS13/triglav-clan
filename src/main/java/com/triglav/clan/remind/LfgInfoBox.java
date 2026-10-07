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
	private volatile Instant endsAt;
	private volatile String title = "";
	private volatile String kind = "LFG";

	LfgInfoBox(BufferedImage image, Plugin plugin)
	{
		super(image, plugin);
	}

	/** @param endsAt when the thing ends, or null; while it runs the box counts down to this instead */
	void update(String kind, String title, Instant startsAt, Instant endsAt)
	{
		this.kind = kind;
		this.title = title;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
	}

	private boolean running()
	{
		return endsAt != null && !Instant.now().isBefore(startsAt);
	}

	@Override
	public String getText()
	{
		if (running())
		{
			final long left = Duration.between(Instant.now(), endsAt).toMinutes();
			return left >= 60 ? (left / 60) + "h" : Math.max(0, left) + "m";
		}

		final long minutes = Duration.between(Instant.now(), startsAt).toMinutes();
		return minutes <= 0 ? "zdaj" : minutes + "m";
	}

	@Override
	public Color getTextColor()
	{
		if (running())
		{
			return Color.GREEN;
		}
		return Duration.between(Instant.now(), startsAt).toMinutes() <= 5 ? Color.ORANGE : Color.WHITE;
	}

	@Override
	public String getTooltip()
	{
		return kind + ": " + title + (running() ? " (v teku)" : "");
	}
}
