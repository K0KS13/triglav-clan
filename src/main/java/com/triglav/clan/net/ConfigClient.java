package com.triglav.clan.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.triglav.clan.TriglavConfig;
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
 * Fetches GET /api/plugin/config/:key on startup and every 6h (docs/plugin-brief.md §4),
 * so the screenshot threshold follows whatever the site has configured.
 */
@Slf4j
@Singleton
public class ConfigClient
{
	private static final long REFRESH_HOURS = 6;

	private final OkHttpClient httpClient;
	private final TriglavConfig config;
	private final AtomicReference<RemoteConfig> current = new AtomicReference<>(RemoteConfig.DEFAULT);

	@Inject
	private ConfigClient(OkHttpClient httpClient, TriglavConfig config, ScheduledExecutorService executor)
	{
		this.httpClient = httpClient;
		this.config = config;
		executor.scheduleWithFixedDelay(this::refresh, 0, REFRESH_HOURS, TimeUnit.HOURS);
	}

	public RemoteConfig current()
	{
		return current.get();
	}

	private void refresh()
	{
		final String clanCode = config.clanCode() == null ? "" : config.clanCode().trim();
		if (clanCode.isEmpty())
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
