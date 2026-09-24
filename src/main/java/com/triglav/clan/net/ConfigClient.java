package com.triglav.clan.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Fetches GET /api/plugin/config/:key on startup, right after linking and then hourly, so a change
 * on the site (e.g. the screenshot threshold) reaches every plugin within the hour without a restart.
 * docs/plugin-brief.md §4 said 6h; an hour is still one tiny request per member.
 */
@Slf4j
@Singleton
public class ConfigClient
{
	private static final long REFRESH_HOURS = 1;

	private final OkHttpClient httpClient;
	private final KeyStore keyStore;
	private final ScheduledExecutorService executor;
	private final AtomicReference<RemoteConfig> current = new AtomicReference<>(RemoteConfig.DEFAULT);

	@Inject
	private ConfigClient(OkHttpClient httpClient, KeyStore keyStore, ScheduledExecutorService executor)
	{
		this.httpClient = httpClient;
		this.keyStore = keyStore;
		this.executor = executor;
		executor.scheduleWithFixedDelay(this::refresh, 0, REFRESH_HOURS, TimeUnit.HOURS);
	}

	public RemoteConfig current()
	{
		return current.get();
	}

	public void refreshNow()
	{
		executor.execute(this::refresh);
	}

	private void refresh()
	{
		final String clanCode = keyStore.ingestKey();
		if (clanCode == null)
		{
			return;
		}

		final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/config/" + clanCode);
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
			final int screenshotMinValue = body.has("screenshotMinValue") ? body.get("screenshotMinValue").getAsInt() : RemoteConfig.DEFAULT.screenshotMinValue;
			final int pollSeconds = body.has("pollSeconds") ? body.get("pollSeconds").getAsInt() : RemoteConfig.DEFAULT.pollSeconds;
			current.set(new RemoteConfig(screenshotMinValue, pollSeconds));
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Could not refresh remote config: {}", e.getMessage());
		}
	}
}
