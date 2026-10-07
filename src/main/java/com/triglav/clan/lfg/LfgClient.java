package com.triglav.clan.lfg;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.triglav.clan.net.ApiClient;
import com.triglav.clan.net.KeyStore;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
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
 * Posts an LFG from the game: POST /api/plugin/lfg/:key. The start is sent as "in N minutes" so no
 * time zone ever enters the picture; the site posts it to Discord with the join buttons, same as a
 * post made on the website. The optional setup becomes the post's recommended gear.
 */
@Slf4j
@Singleton
public class LfgClient
{
	/** A meeting spot in OSRS tile coordinates. */
	public static final class Place
	{
		public final int x;
		public final int y;
		public final int plane;
		public final String name;

		public Place(int x, int y, int plane, String name)
		{
			this.x = x;
			this.y = y;
			this.plane = plane;
			this.name = name == null ? "" : name;
		}
	}

	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	private final OkHttpClient httpClient;
	private final KeyStore keyStore;
	private final ScheduledExecutorService executor;

	@Inject
	private LfgClient(OkHttpClient httpClient, KeyStore keyStore, ScheduledExecutorService executor)
	{
		this.httpClient = httpClient;
		this.keyStore = keyStore;
		this.executor = executor;
	}

	/** @param setup the current gear in Inventory Setups shape, or null to post without a recommended setup */
	public void create(String activity, String title, int inMinutes, int capacity, JsonObject setup, Consumer<String> onResult)
	{
		create(activity, title, inMinutes, capacity, setup, null, onResult);
	}

	/** @param place where the group meets (tile x, y, plane, optional name), or null for no location */
	public void create(String activity, String title, int inMinutes, int capacity, JsonObject setup, Place place, Consumer<String> onResult)
	{
		final String key = keyStore.ingestKey();
		if (key == null)
		{
			onResult.accept("najprej poveži račun.");
			return;
		}

		executor.execute(() ->
		{
			final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/lfg/" + key);
			if (url == null)
			{
				return;
			}

			final JsonObject body = new JsonObject();
			body.addProperty("activity", activity);
			body.addProperty("title", title);
			body.addProperty("inMinutes", inMinutes);
			body.addProperty("capacity", capacity);
			if (setup != null)
			{
				body.add("setup", setup);
			}
			if (place != null)
			{
				body.addProperty("locX", place.x);
				body.addProperty("locY", place.y);
				body.addProperty("locPlane", place.plane);
				if (!place.name.isEmpty())
				{
					body.addProperty("locName", place.name);
				}
			}

			try (Response response = httpClient.newCall(new Request.Builder().url(url).post(RequestBody.create(JSON, body.toString())).build()).execute())
			{
				final String raw = response.body() == null ? "" : response.body().string();
				final JsonObject json = raw.isEmpty() ? new JsonObject() : new JsonParser().parse(raw).getAsJsonObject();
				if (response.isSuccessful())
				{
					onResult.accept("LFG objavljen" + (json.has("url") ? ": " + json.get("url").getAsString() : "."));
				}
				else
				{
					onResult.accept(json.has("error") ? json.get("error").getAsString() : "LFG ni bil objavljen (HTTP " + response.code() + ").");
				}
			}
			catch (IOException | RuntimeException e)
			{
				log.warn("LFG create failed", e);
				onResult.accept("LFG ni bil objavljen — stran ni dosegljiva.");
			}
		});
	}
}
