package com.triglav.clan.star;

import com.google.gson.JsonObject;
import com.triglav.clan.TriglavConfig;
import com.triglav.clan.net.ApiClient;
import com.triglav.clan.net.KeyStore;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Reports a crashed shooting star the player has in view (world, tile and size) to the clan site
 * (clan.kokalj.dev/orodja/zvezde), so members can see where the stars are. Only the star itself is sent,
 * never the player's position; it is the same information every player on that world sees on the ground.
 */
@Slf4j
@Singleton
public class StarReporter
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	/** The same star seen again (scene reload, walking back) is not re-sent within this time. */
	private static final long RESEND_MS = 2 * 60_000;

	private final Client client;
	private final TriglavConfig config;
	private final KeyStore keyStore;
	private final OkHttpClient httpClient;
	private final ScheduledExecutorService executor;

	private String lastSignature = "";
	private long lastSentAt;

	@Inject
	private StarReporter(Client client, TriglavConfig config, KeyStore keyStore, OkHttpClient httpClient, ScheduledExecutorService executor)
	{
		this.client = client;
		this.config = config;
		this.keyStore = keyStore;
		this.httpClient = httpClient;
		this.executor = executor;
	}

	/** Size of a crashed star (1-9) from its object id, or 0 if it is not a star. */
	public static int starSize(int objectId)
	{
		switch (objectId)
		{
			case 41020:
				return 9;
			case 41021:
				return 8;
			case 41223:
				return 7;
			case 41224:
				return 6;
			case 41225:
				return 5;
			case 41226:
				return 4;
			case 41227:
				return 3;
			case 41228:
				return 2;
			case 41229:
				return 1;
			default:
				return 0;
		}
	}

	/** Game thread. */
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		final GameObject object = event.getGameObject();
		final int size = starSize(object.getId());
		if (size == 0 || !config.reportStars() || !keyStore.isLinked() || client.isInInstancedRegion())
		{
			return;
		}

		final WorldPoint at = object.getWorldLocation();
		final int world = client.getWorld();
		final String signature = world + ":" + at.getX() + ":" + at.getY() + ":" + size;
		final long now = System.currentTimeMillis();
		if (signature.equals(lastSignature) && now - lastSentAt < RESEND_MS)
		{
			return;
		}
		lastSignature = signature;
		lastSentAt = now;

		final String key = keyStore.ingestKey();
		if (key == null)
		{
			return;
		}

		final JsonObject body = new JsonObject();
		body.addProperty("ingestKey", key);
		body.addProperty("world", world);
		body.addProperty("x", at.getX());
		body.addProperty("y", at.getY());
		body.addProperty("plane", at.getPlane());
		body.addProperty("size", size);
		executor.execute(() -> post(body));
	}

	private void post(JsonObject body)
	{
		final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/star");
		if (url == null)
		{
			return;
		}

		try (Response response = httpClient.newCall(new Request.Builder().url(url).post(RequestBody.create(JSON, body.toString())).build()).execute())
		{
			if (!response.isSuccessful())
			{
				log.debug("Star report rejected: HTTP {}", response.code());
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Star report failed: {}", e.getMessage());
		}
	}
}
