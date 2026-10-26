package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import com.triglav.clan.TriglavConfig;
import com.triglav.clan.net.ApiClient;
import com.triglav.clan.net.KeyStore;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ChatMessage;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Reports "X received a drop: ..." and "X has been defeated by Y ..." clan broadcasts to the clan site (clan.kokalj.dev). The site de-duplicates
 * them against drops the player's own client already sent, so a drop never counts twice.
 */
@Slf4j
@Singleton
public class ClanBroadcastReporter
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	/** The same broadcast seen again (several chat tabs, rejoining) is not re-sent within this time. */
	private static final long RESEND_MS = 2 * 60_000;

	private final TriglavConfig config;
	private final KeyStore keyStore;
	private final OkHttpClient httpClient;
	private final ScheduledExecutorService executor;

	private String lastSignature = "";
	private long lastSentAt;

	@Inject
	private ClanBroadcastReporter(TriglavConfig config, KeyStore keyStore, OkHttpClient httpClient, ScheduledExecutorService executor)
	{
		this.config = config;
		this.keyStore = keyStore;
		this.httpClient = httpClient;
		this.executor = executor;
	}

	/** Game thread. */
	public void onChatMessage(ChatMessage event)
	{
		if (!config.reportClanBroadcasts() || !keyStore.isLinked())
		{
			return;
		}

		JsonObject drop = ClanBroadcastChat.parse(event.getMessage());
		final boolean death = drop == null;
		if (death)
		{
			drop = ClanBroadcastChat.parseDeath(event.getMessage());
		}
		final String key = keyStore.ingestKey();
		if (drop == null || key == null)
		{
			return;
		}

		final String signature = death
			? "death|" + drop.get("rsn").getAsString() + "|" + drop.get("killer").getAsString() + "|" + drop.get("value").getAsLong()
			: drop.get("rsn").getAsString() + "|" + drop.get("item").getAsString() + "|" + drop.get("value").getAsLong();
		final long now = System.currentTimeMillis();
		if (signature.equals(lastSignature) && now - lastSentAt < RESEND_MS)
		{
			return;
		}
		lastSignature = signature;
		lastSentAt = now;

		drop.addProperty("ingestKey", key);
		final JsonObject payload = drop;
		executor.execute(() -> post(payload, death ? "/api/plugin/broadcast-death" : "/api/plugin/broadcast"));
	}

	private void post(JsonObject body, String path)
	{
		final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + path);
		if (url == null)
		{
			return;
		}

		try (Response response = httpClient.newCall(new Request.Builder().url(url).post(RequestBody.create(JSON, body.toString())).build()).execute())
		{
			if (!response.isSuccessful())
			{
				log.debug("Clan broadcast rejected: HTTP {}", response.code());
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Clan broadcast failed: {}", e.getMessage());
		}
	}
}
