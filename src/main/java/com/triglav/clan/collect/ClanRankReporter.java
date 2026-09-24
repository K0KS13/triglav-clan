package com.triglav.clan.collect;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.triglav.clan.net.ApiClient;
import com.triglav.clan.net.KeyStore;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanID;
import net.runelite.api.clan.ClanSettings;
import net.runelite.client.callback.ClientThread;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Reads the in-game clan tab and reports ranks to the site (docs/plugin-brief.md §5.1), so
 * leadership stops retyping ranks by hand. Runs on login and every 30 minutes; reading the
 * channel itself must happen on the client thread, sending it must not.
 */
@Slf4j
@Singleton
public class ClanRankReporter
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final long INTERVAL_MINUTES = 30;

	private final Client client;
	private final ClientThread clientThread;
	private final OkHttpClient httpClient;
	private final KeyStore keyStore;

	@Inject
	private ClanRankReporter(Client client, ClientThread clientThread, OkHttpClient httpClient, KeyStore keyStore, ScheduledExecutorService executor)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.httpClient = httpClient;
		this.keyStore = keyStore;
		executor.scheduleWithFixedDelay(this::reportNow, INTERVAL_MINUTES, INTERVAL_MINUTES, TimeUnit.MINUTES);
	}

	public void reportNow()
	{
		clientThread.invokeLater(() ->
		{
			final JsonArray members = readMembers();
			if (members != null && members.size() > 0)
			{
				send(members);
			}
		});
	}

	private JsonArray readMembers()
	{
		final ClanChannel channel = client.getClanChannel(ClanID.CLAN);
		final ClanSettings settings = client.getClanSettings(ClanID.CLAN);
		if (channel == null || settings == null)
		{
			return null;
		}

		final JsonArray members = new JsonArray();
		for (ClanChannelMember member : channel.getMembers())
		{
			final JsonObject json = new JsonObject();
			json.addProperty("rsn", member.getName());
			json.addProperty("rank", settings.titleForRank(member.getRank()).getName());
			json.addProperty("rankIndex", member.getRank().getRank());
			members.add(json);
		}
		return members;
	}

	private void send(JsonArray members)
	{
		final String clanCode = keyStore.ingestKey();
		if (clanCode == null)
		{
			return;
		}

		final JsonObject payload = new JsonObject();
		payload.addProperty("ingestKey", clanCode);
		payload.add("members", members);

		final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/clan");
		if (url == null)
		{
			return;
		}

		final Request request = new Request.Builder().url(url).post(RequestBody.create(JSON, payload.toString())).build();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Clan rank report failed: {}", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
			}
		});
	}
}
