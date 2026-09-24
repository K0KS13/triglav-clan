package com.triglav.clan.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.triglav.clan.TriglavConfig;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Turns the member's permanent clan code (TRG-XXXX from clan.kokalj.dev/profil) into the ingest key
 * every endpoint uses, via POST /api/plugin/link. The last key is cached in the config so the plugin
 * keeps working when the site is unreachable at startup. A value that isn't a TRG code is taken as
 * the raw ingest key itself (the key from the Dink URL still works).
 */
@Slf4j
@Singleton
public class KeyStore
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final String CACHED_KEY = "ingestKey";

	public enum Result
	{
		LINKED, WRONG_CODE, UNREACHABLE, EMPTY
	}

	private final OkHttpClient httpClient;
	private final TriglavConfig config;
	private final ConfigManager configManager;
	private final ScheduledExecutorService executor;

	private volatile String ingestKey;
	private volatile String linkedName;

	@Inject
	private KeyStore(OkHttpClient httpClient, TriglavConfig config, ConfigManager configManager, ScheduledExecutorService executor)
	{
		this.httpClient = httpClient;
		this.config = config;
		this.configManager = configManager;
		this.executor = executor;
		this.ingestKey = configManager.getConfiguration(TriglavConfig.GROUP, CACHED_KEY);
	}

	/** @return the ingest key, or null if not linked */
	public String ingestKey()
	{
		final String key = ingestKey;
		return key == null || key.isEmpty() ? null : key;
	}

	public boolean isLinked()
	{
		return ingestKey() != null;
	}

	/** @return the Discord name the code belongs to, once confirmed by the site this session */
	public String linkedName()
	{
		return linkedName;
	}

	/** Resolves the configured clan code off the client thread and reports the outcome. */
	public void refresh(Consumer<Result> onDone)
	{
		executor.execute(() -> onDone.accept(resolve()));
	}

	private Result resolve()
	{
		final String raw = config.clanCode() == null ? "" : config.clanCode().trim();
		if (raw.isEmpty())
		{
			store(null, null);
			return Result.EMPTY;
		}

		if (!isClanCode(raw))
		{
			store(raw, null);
			return Result.LINKED;
		}

		final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/link");
		if (url == null)
		{
			return Result.UNREACHABLE;
		}

		final JsonObject body = new JsonObject();
		body.addProperty("code", raw);
		final Request request = new Request.Builder().url(url).post(RequestBody.create(JSON, body.toString())).build();

		try (Response response = httpClient.newCall(request).execute())
		{
			if (response.code() == 404)
			{
				store(null, null);
				return Result.WRONG_CODE;
			}
			if (!response.isSuccessful() || response.body() == null)
			{
				return Result.UNREACHABLE;
			}

			final JsonObject json = new JsonParser().parse(response.body().string()).getAsJsonObject();
			store(json.get("ingestKey").getAsString(), json.has("name") ? json.get("name").getAsString() : null);
			return Result.LINKED;
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Could not link clan code: {}", e.getMessage());
			return Result.UNREACHABLE;
		}
	}

	private void store(String key, String name)
	{
		ingestKey = key;
		linkedName = name;
		if (key == null)
		{
			configManager.unsetConfiguration(TriglavConfig.GROUP, CACHED_KEY);
		}
		else
		{
			configManager.setConfiguration(TriglavConfig.GROUP, CACHED_KEY, key);
		}
	}

	/** "TRG-2A3B", "trg 2a3b", "TRG2A3B" */
	static boolean isClanCode(String raw)
	{
		return raw.toUpperCase().replaceAll("[^A-Z0-9]", "").matches("TRG[A-Z0-9]{4}");
	}
}
