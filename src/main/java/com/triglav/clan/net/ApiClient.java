package com.triglav.clan.net;

import com.google.gson.JsonObject;
import com.triglav.clan.TriglavConfig;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Talks to the clan site's ingest endpoint, which speaks the same envelope as Dink
 * (see docs/plugin-brief.md §2 in the site repo). Everything queues and drains off the
 * client thread, so a slow or unreachable site never stutters the game.
 */
@Slf4j
@Singleton
public class ApiClient
{
	/**
	 * Overridable with -Dtriglav.siteUrl=http://localhost:3000 for local development against
	 * `docker compose up` in the site repo. Production members never need to touch this.
	 */
	private static final String DEFAULT_SITE_URL = "https://clan.kokalj.dev";

	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final MediaType WEBP = MediaType.parse("image/webp");

	/** Queue cap from docs/plugin-brief.md §6. */
	private static final int MAX_QUEUE = 200;
	/** Give up on an individual event after this many failed attempts. */
	private static final int MAX_ATTEMPTS = 3;
	private static final int DRAIN_INTERVAL_SECONDS = 5;

	private final OkHttpClient httpClient;
	private final TriglavConfig config;
	private final Deque<QueuedEvent> queue = new ArrayDeque<>();

	@Inject
	private ApiClient(OkHttpClient httpClient, TriglavConfig config, ScheduledExecutorService executor)
	{
		this.httpClient = httpClient;
		this.config = config;
		executor.scheduleWithFixedDelay(this::drain, DRAIN_INTERVAL_SECONDS, DRAIN_INTERVAL_SECONDS, TimeUnit.SECONDS);
	}

	public static String siteUrl()
	{
		final String override = System.getProperty("triglav.siteUrl");
		return override == null || override.trim().isEmpty() ? DEFAULT_SITE_URL : override.trim();
	}

	/** @return true if a clan code is configured, i.e. pairing has been completed */
	public boolean isPaired()
	{
		return !trimmedClanCode().isEmpty();
	}

	/**
	 * Queues a Dink-shaped event for delivery. Safe to call from any thread.
	 *
	 * @param screenshot WebP bytes, or null to send without one
	 */
	public synchronized void send(JsonObject envelope, byte[] screenshot)
	{
		if (!isPaired())
		{
			return;
		}

		if (queue.size() >= MAX_QUEUE)
		{
			final QueuedEvent dropped = queue.pollFirst();
			log.warn("Ingest queue full, dropping oldest event: {}", dropped == null ? "?" : dropped.type);
		}

		queue.addLast(new QueuedEvent(envelope, screenshot));
	}

	private void drain()
	{
		QueuedEvent event;
		synchronized (this)
		{
			event = queue.pollFirst();
		}

		if (event == null)
		{
			return;
		}

		if (deliver(event))
		{
			return;
		}

		event.attempts++;
		if (event.attempts < MAX_ATTEMPTS)
		{
			synchronized (this)
			{
				queue.addFirst(event);
			}
		}
		else
		{
			log.warn("Giving up on event after {} attempts: {}", event.attempts, event.type);
		}
	}

	private boolean deliver(QueuedEvent event)
	{
		final String clanCode = trimmedClanCode();
		if (clanCode.isEmpty())
		{
			return false;
		}

		final HttpUrl url = HttpUrl.parse(siteUrl() + "/api/dink/" + clanCode);
		if (url == null)
		{
			log.warn("Invalid site URL, cannot deliver {}", event.type);
			return false;
		}

		final RequestBody body = event.screenshot == null
			? RequestBody.create(JSON, event.json.toString())
			: new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("payload_json", event.json.toString())
				.addFormDataPart("file", "screenshot.webp", RequestBody.create(WEBP, event.screenshot))
				.build();

		final Request request = new Request.Builder().url(url).post(body).build();

		try (Response response = httpClient.newCall(request).execute())
		{
			if (response.isSuccessful())
			{
				return true;
			}
			log.warn("Ingest rejected {} with HTTP {}", event.type, response.code());
			return false;
		}
		catch (IOException e)
		{
			log.debug("Ingest unreachable for {}: {}", event.type, e.getMessage());
			return false;
		}
	}

	private String trimmedClanCode()
	{
		final String code = config.clanCode();
		return code == null ? "" : code.trim();
	}

	private static final class QueuedEvent
	{
		final String type;
		final JsonObject json;
		final byte[] screenshot;
		int attempts;

		QueuedEvent(JsonObject json, byte[] screenshot)
		{
			this.json = json;
			this.type = json.has("type") ? json.get("type").getAsString() : "?";
			this.screenshot = screenshot;
		}
	}
}
