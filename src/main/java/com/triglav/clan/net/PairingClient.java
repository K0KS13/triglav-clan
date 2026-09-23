package com.triglav.clan.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.triglav.clan.TriglavConfig;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.config.ConfigManager;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Drives the pairing flow from docs/plugin-brief.md §3: ask the site for a short code, show it,
 * poll until a member has entered it on /profil, then store the resulting ingestKey.
 */
@Slf4j
@Singleton
public class PairingClient
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final int POLL_SECONDS = 5;

	private final Client client;
	private final OkHttpClient httpClient;
	private final ConfigManager configManager;
	private final ScheduledExecutorService executor;

	private ScheduledFuture<?> pollTask;

	@Inject
	private PairingClient(Client client, OkHttpClient httpClient, ConfigManager configManager, ScheduledExecutorService executor)
	{
		this.client = client;
		this.httpClient = httpClient;
		this.configManager = configManager;
		this.executor = executor;
	}

	/**
	 * @param onCode   called with the 6-character code as soon as it is issued (for the panel/chat)
	 * @param onResult called with true once paired, false if the request failed or the code expired
	 */
	public void start(Consumer<String> onCode, Consumer<Boolean> onResult)
	{
		cancel();
		executor.execute(() ->
		{
			final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/pair/start");
			if (url == null)
			{
				onResult.accept(false);
				return;
			}

			// `playerName` lets the site show the member which account is asking to pair, so they can
			// spot a code that isn't theirs before confirming (docs/plugin-handover.md §5).
			final Player local = client.getLocalPlayer();
			final JsonObject payload = new JsonObject();
			if (local != null && local.getName() != null)
			{
				payload.addProperty("playerName", local.getName());
			}

			final Request request = new Request.Builder().url(url).post(RequestBody.create(JSON, payload.toString())).build();
			try (Response response = httpClient.newCall(request).execute())
			{
				if (!response.isSuccessful() || response.body() == null)
				{
					onResult.accept(false);
					return;
				}

				final JsonObject body = new JsonParser().parse(response.body().string()).getAsJsonObject();
				final String pairId = body.get("pairId").getAsString();
				final String code = body.get("code").getAsString();
				onCode.accept(code);
				pollTask = executor.scheduleWithFixedDelay(() -> poll(pairId, onResult), POLL_SECONDS, POLL_SECONDS, TimeUnit.SECONDS);
			}
			catch (IOException | RuntimeException e)
			{
				log.warn("Could not start pairing", e);
				onResult.accept(false);
			}
		});
	}

	public void cancel()
	{
		if (pollTask != null)
		{
			pollTask.cancel(false);
			pollTask = null;
		}
	}

	private void poll(String pairId, Consumer<Boolean> onResult)
	{
		final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/pair/" + pairId);
		if (url == null)
		{
			cancel();
			onResult.accept(false);
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
			final String status = body.get("status").getAsString();

			if ("ok".equals(status))
			{
				final String ingestKey = body.get("ingestKey").getAsString();
				configManager.setConfiguration(TriglavConfig.GROUP, "clanCode", ingestKey);
				cancel();
				onResult.accept(true);
			}
			else if ("expired".equals(status))
			{
				cancel();
				onResult.accept(false);
			}
			// "pending" - keep polling
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Pairing poll failed: {}", e.getMessage());
		}
	}
}
