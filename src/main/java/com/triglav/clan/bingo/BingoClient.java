package com.triglav.clan.bingo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.triglav.clan.TriglavConfig;
import com.triglav.clan.net.ApiClient;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Polls GET /api/plugin/bingo/:key (docs/plugin-brief.md §5.3) for the member's team board. The
 * site completes tiles itself from LOOT events, so after a drop that matches an open tile the
 * plugin just asks again a few seconds later instead of guessing the tile state locally.
 */
@Slf4j
@Singleton
public class BingoClient
{
	private static final int POLL_SECONDS = 60;
	private static final int REFRESH_AFTER_DROP_SECONDS = 10;
	private static final Color DEFAULT_TEAM_COLOR = new Color(0xd8a93f);

	private final OkHttpClient httpClient;
	private final TriglavConfig config;
	private final ScheduledExecutorService executor;
	private final AtomicReference<BingoBoard> board = new AtomicReference<>();

	@Inject
	private BingoClient(OkHttpClient httpClient, TriglavConfig config, ScheduledExecutorService executor)
	{
		this.httpClient = httpClient;
		this.config = config;
		this.executor = executor;
		executor.scheduleWithFixedDelay(this::refresh, 5, POLL_SECONDS, TimeUnit.SECONDS);
	}

	/** @return the current board, or null if there is no active bingo for this member */
	public BingoBoard board()
	{
		return board.get();
	}

	public void refreshSoon()
	{
		executor.schedule(this::refresh, REFRESH_AFTER_DROP_SECONDS, TimeUnit.SECONDS);
	}

	private void refresh()
	{
		final String clanCode = config.clanCode() == null ? "" : config.clanCode().trim();
		if (clanCode.isEmpty() || !config.bingoOverlay())
		{
			board.set(null);
			return;
		}

		final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/bingo/" + clanCode);
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
			board.set(parse(new JsonParser().parse(response.body().string()).getAsJsonObject()));
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Bingo poll failed: {}", e.getMessage());
		}
	}

	static BingoBoard parse(JsonObject body)
	{
		if (!body.has("bingo") || body.get("bingo").isJsonNull())
		{
			return null;
		}

		final JsonObject bingo = body.getAsJsonObject("bingo");
		final JsonObject team = body.has("team") && !body.get("team").isJsonNull() ? body.getAsJsonObject("team") : null;

		final List<BingoBoard.Tile> tiles = new ArrayList<>();
		final JsonArray rawTiles = body.has("tiles") ? body.getAsJsonArray("tiles") : new JsonArray();
		for (JsonElement element : rawTiles)
		{
			final JsonObject t = element.getAsJsonObject();
			final JsonArray rawIds = t.has("itemIds") && t.get("itemIds").isJsonArray() ? t.getAsJsonArray("itemIds") : new JsonArray();
			final int[] itemIds = new int[rawIds.size()];
			for (int i = 0; i < itemIds.length; i++)
			{
				itemIds[i] = rawIds.get(i).getAsInt();
			}

			tiles.add(new BingoBoard.Tile(
				t.get("index").getAsInt(),
				t.has("title") ? t.get("title").getAsString() : "",
				t.has("iconItemId") && !t.get("iconItemId").isJsonNull() ? t.get("iconItemId").getAsInt() : -1,
				itemIds,
				status(t.has("status") ? t.get("status").getAsString() : "open")));
		}

		return new BingoBoard(
			bingo.has("title") ? bingo.get("title").getAsString() : "Bingo",
			bingo.get("rows").getAsInt(),
			bingo.get("cols").getAsInt(),
			team != null && team.has("name") ? team.get("name").getAsString() : "",
			team != null && team.has("color") ? color(team.get("color").getAsString()) : DEFAULT_TEAM_COLOR,
			tiles);
	}

	private static BingoBoard.Status status(String raw)
	{
		switch (raw)
		{
			case "done":
				return BingoBoard.Status.DONE;
			case "pending":
				return BingoBoard.Status.PENDING;
			default:
				return BingoBoard.Status.OPEN;
		}
	}

	private static Color color(String hex)
	{
		try
		{
			return Color.decode(hex);
		}
		catch (NumberFormatException e)
		{
			return DEFAULT_TEAM_COLOR;
		}
	}
}
