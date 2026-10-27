package com.triglav.clan.star;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.triglav.clan.net.ApiClient;
import com.triglav.clan.net.KeyStore;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.game.WorldService;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A shooting star the member sent from the clan site ("V igro"). The panel shows it; only when the member clicks
 * "Pojdi" does the plugin open the world switcher, hop to the star's world and ask the Shortest Path plugin
 * (if installed) for a path to the star. Nothing happens in the game without that click.
 */
@Slf4j
@Singleton
public class StarGuide
{
	public static final class Target
	{
		public final String id;
		public final int world;
		public final int x;
		public final int y;
		public final int plane;
		public final int size;
		public final String place;

		Target(String id, int world, int x, int y, int plane, int size, String place)
		{
			this.id = id;
			this.world = world;
			this.x = x;
			this.y = y;
			this.plane = plane;
			this.size = size;
			this.place = place;
		}

		public String label()
		{
			return "W" + world + " · " + place + " · velikost " + size;
		}
	}

	private static final long POLL_SECONDS = 20;
	private static final int HOP_ATTEMPTS = 5;

	private final Client client;
	private final ClientThread clientThread;
	private final OkHttpClient httpClient;
	private final KeyStore keyStore;
	private final EventBus eventBus;
	private final WorldService worldService;
	private final Gson gson;

	private volatile Target target;
	private String lastSeenId;
	private Consumer<Target> listener = t ->
	{
	};

	/** Game thread only. */
	private net.runelite.api.World hopTo;
	private int hopAttempts;
	private Target pathAfterHop;

	@Inject
	private StarGuide(Client client, ClientThread clientThread, OkHttpClient httpClient, KeyStore keyStore, EventBus eventBus,
		WorldService worldService, Gson gson, ScheduledExecutorService executor)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.httpClient = httpClient;
		this.keyStore = keyStore;
		this.eventBus = eventBus;
		this.worldService = worldService;
		this.gson = gson;
		executor.scheduleWithFixedDelay(this::poll, POLL_SECONDS, POLL_SECONDS, TimeUnit.SECONDS);
	}

	/** Called with the new star (or null when it expired); from a background thread. */
	public void setListener(Consumer<Target> listener)
	{
		this.listener = listener;
	}

	public Target target()
	{
		return target;
	}

	private void poll()
	{
		final String key = keyStore.ingestKey();
		if (key == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/star-target/" + key);
		if (url == null)
		{
			return;
		}

		try (Response response = httpClient.newCall(new Request.Builder().url(url).get().build()).execute())
		{
			if (!response.isSuccessful() || response.body() == null)
			{
				return;
			}
			final JsonObject body = gson.fromJson(response.body().charStream(), JsonObject.class);
			final Target next = parse(body);
			final String id = next == null ? null : next.id;
			if (id == null ? lastSeenId == null : id.equals(lastSeenId))
			{
				return;
			}
			lastSeenId = id;
			target = next;
			listener.accept(next);
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Star target poll failed: {}", e.getMessage());
		}
	}

	private static Target parse(JsonObject body)
	{
		if (body == null || !body.has("target") || !body.get("target").isJsonObject())
		{
			return null;
		}
		final JsonObject t = body.getAsJsonObject("target");
		return new Target(t.get("id").getAsString(), t.get("world").getAsInt(), t.get("x").getAsInt(), t.get("y").getAsInt(),
			t.has("plane") ? t.get("plane").getAsInt() : 0, t.has("size") ? t.get("size").getAsInt() : 0,
			t.has("place") ? t.get("place").getAsString() : "?");
	}

	/** The member clicked "Pojdi": hop when needed, then show the path. */
	public void go(Consumer<String> chat)
	{
		final Target t = target;
		if (t == null)
		{
			return;
		}
		clientThread.invoke(() ->
		{
			if (client.getWorld() == t.world)
			{
				showPath(t);
				chat.accept("TRIGLAV: pot do zvezde (" + t.place + ") je v Shortest Path.");
				return;
			}

			final WorldResult worlds = worldService.getWorlds();
			final World world = worlds == null ? null : worlds.findWorld(t.world);
			if (world == null)
			{
				chat.accept("TRIGLAV: world " + t.world + " ni na seznamu svetov.");
				return;
			}

			final net.runelite.api.World rsWorld = client.createWorld();
			rsWorld.setActivity(world.getActivity());
			rsWorld.setAddress(world.getAddress());
			rsWorld.setId(world.getId());
			rsWorld.setPlayerCount(world.getPlayers());
			rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

			pathAfterHop = t;
			if (client.getGameState() == GameState.LOGIN_SCREEN)
			{
				client.changeWorld(rsWorld);
				return;
			}
			hopTo = rsWorld;
			hopAttempts = 0;
			client.openWorldHopper();
			chat.accept("TRIGLAV: menjam na world " + t.world + " …");
		});
	}

	/** Remove the path shown for the star. */
	public void clearPath()
	{
		clientThread.invoke(() -> eventBus.post(new PluginMessage("shortestpath", "clear")));
	}

	/** Game thread: finishes the hop once the world switcher is open (as RuneLite's World Hopper does). */
	public void onGameTick()
	{
		if (hopTo == null)
		{
			return;
		}
		if (client.getWidget(InterfaceID.Worldswitcher.LIST) == null)
		{
			client.openWorldHopper();
			if (++hopAttempts >= HOP_ATTEMPTS)
			{
				hopTo = null;
				pathAfterHop = null;
			}
			return;
		}
		client.hopToWorld(hopTo);
		hopTo = null;
	}

	/** Game thread: after the hop the path is requested on the new world. */
	public void onLoggedIn()
	{
		final Target t = pathAfterHop;
		if (t != null && client.getWorld() == t.world)
		{
			pathAfterHop = null;
			showPath(t);
		}
	}

	private void showPath(Target t)
	{
		final Map<String, Object> data = new HashMap<>();
		data.put("target", new WorldPoint(t.x, t.y, t.plane));
		eventBus.post(new PluginMessage("shortestpath", "path", data));
	}
}
