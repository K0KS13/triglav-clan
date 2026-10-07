package com.triglav.clan.overview;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.triglav.clan.net.ApiClient;
import com.triglav.clan.net.KeyStore;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Polls GET /api/plugin/overview/:key once a minute: events, LFG, clan goals, deaths, points and
 * shop in one request instead of one per panel section. The latest snapshot is kept, and a listener
 * is told whenever a fresh one lands. Purchases go through POST /api/plugin/shop/:key.
 */
@Slf4j
@Singleton
public class OverviewClient
{
	private static final int POLL_SECONDS = 60;
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	private final OkHttpClient httpClient;
	private final KeyStore keyStore;
	private final ScheduledExecutorService executor;
	private final AtomicReference<Overview> current = new AtomicReference<>(Overview.EMPTY);
	private volatile Consumer<Overview> listener = overview ->
	{
	};

	@Inject
	private OverviewClient(OkHttpClient httpClient, KeyStore keyStore, ScheduledExecutorService executor)
	{
		this.httpClient = httpClient;
		this.keyStore = keyStore;
		this.executor = executor;
		executor.scheduleWithFixedDelay(this::refresh, 3, POLL_SECONDS, TimeUnit.SECONDS);
	}

	public Overview current()
	{
		return current.get();
	}

	public void setListener(Consumer<Overview> listener)
	{
		this.listener = listener;
	}

	public void refreshNow()
	{
		executor.execute(this::refresh);
	}

	private void refresh()
	{
		final String key = keyStore.ingestKey();
		if (key == null)
		{
			current.set(Overview.EMPTY);
			return;
		}

		final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/overview/" + key);
		if (url == null)
		{
			return;
		}

		try (Response response = httpClient.newCall(new Request.Builder().url(url).build()).execute())
		{
			if (!response.isSuccessful() || response.body() == null)
			{
				return;
			}

			final Overview overview = Overview.parse(new JsonParser().parse(response.body().string()).getAsJsonObject());
			current.set(overview);
			listener.accept(overview);
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Overview poll failed: {}", e.getMessage());
		}
	}

	/** Join (or, with leave=true, leave) an LFG; @param onResult human-readable result for the game chat */
	public void joinLfg(String postId, boolean leave, Consumer<String> onResult)
	{
		final String key = keyStore.ingestKey();
		if (key == null)
		{
			onResult.accept("najprej poveži račun.");
			return;
		}

		executor.execute(() ->
		{
			final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/lfg/" + key + "/join");
			if (url == null)
			{
				return;
			}

			final JsonObject body = new JsonObject();
			body.addProperty("postId", postId);
			body.addProperty("leave", leave);
			final Request request = new Request.Builder().url(url).post(RequestBody.create(JSON, body.toString())).build();

			try (Response response = httpClient.newCall(request).execute())
			{
				final String raw = response.body() == null ? "" : response.body().string();
				final JsonObject json = raw.isEmpty() ? new JsonObject() : new JsonParser().parse(raw).getAsJsonObject();
				if (response.isSuccessful())
				{
					onResult.accept(json.has("message") ? json.get("message").getAsString() : "ok.");
					refresh();
				}
				else
				{
					onResult.accept(json.has("error") ? json.get("error").getAsString() : "ni uspelo (HTTP " + response.code() + ").");
				}
			}
			catch (IOException | RuntimeException e)
			{
				log.warn("LFG join failed", e);
				onResult.accept("ni uspelo — stran ni dosegljiva.");
			}
		});
	}

	/** @param onResult human-readable result for the game chat */
	public void buy(String itemId, Consumer<String> onResult)
	{
		final String key = keyStore.ingestKey();
		if (key == null)
		{
			onResult.accept("najprej poveži račun.");
			return;
		}

		executor.execute(() ->
		{
			final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/shop/" + key);
			if (url == null)
			{
				return;
			}

			final JsonObject body = new JsonObject();
			body.addProperty("itemId", itemId);
			final Request request = new Request.Builder().url(url).post(RequestBody.create(JSON, body.toString())).build();

			try (Response response = httpClient.newCall(request).execute())
			{
				final String raw = response.body() == null ? "" : response.body().string();
				final JsonObject json = raw.isEmpty() ? new JsonObject() : new JsonParser().parse(raw).getAsJsonObject();
				if (response.isSuccessful())
				{
					onResult.accept(json.has("message") ? json.get("message").getAsString() : "nakup oddan.");
					refresh();
				}
				else
				{
					onResult.accept(json.has("error") ? json.get("error").getAsString() : "nakupa ni bilo mogoče oddati (HTTP " + response.code() + ").");
				}
			}
			catch (IOException | RuntimeException e)
			{
				log.warn("Shop purchase failed", e);
				onResult.accept("nakupa ni bilo mogoče oddati — stran ni dosegljiva.");
			}
		});
	}
}
