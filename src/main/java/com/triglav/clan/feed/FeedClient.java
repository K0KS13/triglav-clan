package com.triglav.clan.feed;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.triglav.clan.TriglavConfig;
import com.triglav.clan.net.ApiClient;
import com.triglav.clan.net.KeyStore;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Polls GET /api/plugin/feed/:key every 60s (docs/plugin-brief.md §5.2) and hands new messages
 * to a callback, which the plugin prints to the game chat. Remembers `id`s so a message is
 * never shown twice, bounded so it cannot grow forever across a long session.
 */
@Slf4j
@Singleton
public class FeedClient
{
	private static final int POLL_SECONDS = 60;
	private static final int MAX_SEEN = 500;

	private final OkHttpClient httpClient;
	private final TriglavConfig config;
	private final KeyStore keyStore;
	private final Set<String> seen = new LinkedHashSet<>();

	@Inject
	private FeedClient(OkHttpClient httpClient, TriglavConfig config, KeyStore keyStore, ScheduledExecutorService executor)
	{
		this.httpClient = httpClient;
		this.config = config;
		this.keyStore = keyStore;
		executor.scheduleWithFixedDelay(this::poll, POLL_SECONDS, POLL_SECONDS, TimeUnit.SECONDS);
	}

	/** Set by the plugin once it knows where messages should go. */
	private volatile Consumer<String> sink;

	public void setSink(Consumer<String> sink)
	{
		this.sink = sink;
	}

	private void poll()
	{
		final Consumer<String> currentSink = sink;
		if (currentSink == null || !config.inGameMessages())
		{
			return;
		}

		final String clanCode = keyStore.ingestKey();
		if (clanCode == null)
		{
			return;
		}

		final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/feed/" + clanCode);
		if (url == null)
		{
			return;
		}

		final Request request = new Request.Builder().url(url).build();
		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful() || response.body() == null)
			{
				return;
			}

			final JsonObject body = new JsonParser().parse(response.body().string()).getAsJsonObject();
			final JsonArray messages = body.getAsJsonArray("messages");
			if (messages == null)
			{
				return;
			}

			for (int i = 0; i < messages.size(); i++)
			{
				final JsonObject message = messages.get(i).getAsJsonObject();
				final String id = message.get("id").getAsString();
				if (seen.contains(id))
				{
					continue;
				}

				seen.add(id);
				while (seen.size() > MAX_SEEN)
				{
					seen.remove(seen.iterator().next());
				}

				currentSink.accept(message.get("text").getAsString());
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Feed poll failed: {}", e.getMessage());
		}
	}
}
