package com.triglav.clan.collect;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Resizes and encodes a captured frame for the ingest endpoint. docs/plugin-brief.md §2 asks for
 * WebP to save bandwidth, but the JDK has no built-in WebP encoder and the site already
 * re-encodes every upload to WebP itself (src/lib/images.ts) - so PNG at the same width cap gets
 * the actual goal (small upload) without a third-party image codec dependency in a plugin that
 * has to pass RuneLite Plugin Hub review.
 */
public final class Screenshot
{
	private static final int MAX_WIDTH = 1600;

	private Screenshot()
	{
	}

	public static byte[] encode(BufferedImage image) throws IOException
	{
		final BufferedImage scaled = image.getWidth() <= MAX_WIDTH ? image : resize(image);
		try (ByteArrayOutputStream out = new ByteArrayOutputStream())
		{
			ImageIO.write(scaled, "png", out);
			return out.toByteArray();
		}
	}

	private static BufferedImage resize(BufferedImage image)
	{
		final int width = MAX_WIDTH;
		final int height = Math.round((float) image.getHeight() * width / image.getWidth());

		final BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = resized.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, 0, 0, width, height, null);
		g.dispose();
		return resized;
	}
}
