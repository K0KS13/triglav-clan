package com.triglav.clan.remind;

import com.triglav.clan.net.ApiClient;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.ImageUtil;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Icons for the countdown box (a boss, a skill, an event type). The site resizes and caches them
 * (src/app/api/icon), so the plugin only ever talks to the clan site. Fetched once per URL off the
 * game thread; until an icon arrives (or if it never does) the caller keeps using its fallback.
 */
@Slf4j
@Singleton
public class IconCache
{
	private static final int SIZE = 32;

	private final OkHttpClient httpClient;
	private final ScheduledExecutorService executor;
	private final Map<String, BufferedImage> images = new ConcurrentHashMap<>();
	private final Set<String> requested = ConcurrentHashMap.newKeySet();

	@Inject
	private IconCache(OkHttpClient httpClient, ScheduledExecutorService executor)
	{
		this.httpClient = httpClient;
		this.executor = executor;
	}

	/** @return the icon if it has already been fetched, otherwise null (and a fetch is started) */
	public BufferedImage get(String url)
	{
		if (url == null || url.isEmpty())
		{
			return null;
		}

		final BufferedImage cached = images.get(url);
		if (cached == null && requested.add(url))
		{
			executor.execute(() -> fetch(url));
		}
		return cached;
	}

	private void fetch(String url)
	{
		// Only the clan site: the plugin never fetches from anywhere else.
		final HttpUrl parsed = HttpUrl.parse(url);
		final HttpUrl site = HttpUrl.parse(ApiClient.siteUrl());
		if (parsed == null || site == null || !parsed.host().equals(site.host()))
		{
			return;
		}

		try (Response response = httpClient.newCall(new Request.Builder().url(parsed).build()).execute())
		{
			if (!response.isSuccessful() || response.body() == null)
			{
				requested.remove(url); // try again later
				return;
			}

			try (InputStream in = response.body().byteStream())
			{
				final BufferedImage image = ImageIO.read(in);
				if (image != null)
				{
					images.put(url, ImageUtil.resizeImage(image, SIZE, SIZE, true));
				}
			}
		}
		catch (IOException | RuntimeException e)
		{
			requested.remove(url);
			log.debug("Icon fetch failed for {}: {}", url, e.getMessage());
		}
	}
}
