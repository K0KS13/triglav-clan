package com.triglav.clan.share;

import com.google.gson.JsonObject;
import com.triglav.clan.TriglavConfig;
import com.triglav.clan.net.ApiClient;
import com.triglav.clan.net.KeyStore;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Live location for the clan map (site: /zemljevid/zivo), strictly opt-in: nothing is sent unless the
 * member ticks "Deli mojo lokacijo" in the plugin settings. One position every ~30 s, no trail. It is
 * never sent where being findable is dangerous - the wilderness, PvP / Deadman / Last Man Standing worlds
 * and instanced areas (raids, where coordinates are meaningless anyway) - and the site is told to forget
 * it the moment sharing stops, the member logs out or enters such an area.
 */
@Slf4j
@Singleton
public class LocationSharer
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final int INTERVAL_TICKS = 50; // ~30 s

	private final Client client;
	private final TriglavConfig config;
	private final KeyStore keyStore;
	private final OkHttpClient httpClient;
	private final ScheduledExecutorService executor;

	private int ticks;
	private boolean sentSomething;

	@Inject
	private LocationSharer(Client client, TriglavConfig config, KeyStore keyStore, OkHttpClient httpClient, ScheduledExecutorService executor)
	{
		this.client = client;
		this.config = config;
		this.keyStore = keyStore;
		this.httpClient = httpClient;
		this.executor = executor;
	}

	/** Game thread, every tick. */
	public void onGameTick()
	{
		if (!config.shareLocation() || !keyStore.isLinked() || client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			clear();
			return;
		}

		if (isSensitive(client))
		{
			clear();
			return;
		}

		if (++ticks < INTERVAL_TICKS && sentSomething)
		{
			return;
		}
		ticks = 0;

		final WorldPoint here = client.getLocalPlayer().getWorldLocation();
		final JsonObject body = new JsonObject();
		body.addProperty("x", here.getX());
		body.addProperty("y", here.getY());
		body.addProperty("plane", here.getPlane());
		body.addProperty("world", client.getWorld());
		sentSomething = true;
		post(body);
	}

	/** Forget the last position on the site; a no-op if nothing was sent. Safe from any thread. */
	public void clear()
	{
		ticks = 0;
		if (!sentSomething)
		{
			return;
		}

		sentSomething = false;
		final JsonObject body = new JsonObject();
		body.addProperty("clear", true);
		post(body);
	}

	/** Wilderness, PvP-type worlds and instances: never share, and withdraw anything already shared. */
	static boolean isSensitive(Client client)
	{
		final Set<WorldType> world = client.getWorldType();
		return client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1
			|| client.isInInstancedRegion()
			|| world.contains(WorldType.PVP)
			|| world.contains(WorldType.BOUNTY)
			|| world.contains(WorldType.HIGH_RISK)
			|| world.contains(WorldType.DEADMAN)
			|| world.contains(WorldType.LAST_MAN_STANDING)
			|| world.contains(WorldType.PVP_ARENA)
			|| world.contains(WorldType.TOURNAMENT_WORLD);
	}

	private void post(JsonObject body)
	{
		final String key = keyStore.ingestKey();
		if (key == null)
		{
			return;
		}

		executor.execute(() ->
		{
			final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/location/" + key);
			if (url == null)
			{
				return;
			}

			try (Response response = httpClient.newCall(new Request.Builder().url(url).post(RequestBody.create(JSON, body.toString())).build()).execute())
			{
				if (!response.isSuccessful())
				{
					log.debug("Location share rejected: HTTP {}", response.code());
				}
			}
			catch (IOException | RuntimeException e)
			{
				log.debug("Location share failed: {}", e.getMessage());
			}
		});
	}
}
