package com.triglav.clan.gear;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.triglav.clan.TriglavConfig;
import com.triglav.clan.net.ApiClient;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Varbits;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * "Pošlji trenutni setup" (docs/plugin-brief.md §5.3): sends worn equipment and inventory to
 * POST /api/plugin/gear in the RuneLite Inventory Setups shape the site already imports
 * (src/lib/gear.ts `fromInventorySetups`: inv[28], eq[14] by EquipmentInventorySlot index,
 * items {id, q, name}). Containers are read on the client thread, the request goes out off it.
 */
@Slf4j
@Singleton
public class GearClient
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final int INVENTORY_SIZE = 28;
	private static final int EQUIPMENT_SIZE = 14;

	private final Client client;
	private final ClientThread clientThread;
	private final ItemManager itemManager;
	private final OkHttpClient httpClient;
	private final TriglavConfig config;
	private final ScheduledExecutorService executor;

	@Inject
	private GearClient(Client client, ClientThread clientThread, ItemManager itemManager, OkHttpClient httpClient,
		TriglavConfig config, ScheduledExecutorService executor)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.itemManager = itemManager;
		this.httpClient = httpClient;
		this.config = config;
		this.executor = executor;
	}

	/**
	 * @param onResult receives a human-readable result line for the game chat
	 */
	public void sendCurrentSetup(String title, Consumer<String> onResult)
	{
		final String clanCode = config.clanCode() == null ? "" : config.clanCode().trim();
		if (clanCode.isEmpty())
		{
			onResult.accept("najprej poveži račun.");
			return;
		}

		clientThread.invoke(() ->
		{
			final ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
			final ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
			if (equipment == null && inventory == null)
			{
				onResult.accept("setupa ni mogoče prebrati — prijavi se v igro.");
				return;
			}

			final JsonObject setup = new JsonObject();
			setup.add("inv", items(inventory, INVENTORY_SIZE));
			setup.add("eq", items(equipment, EQUIPMENT_SIZE));
			setup.addProperty("sb", client.getVarbitValue(Varbits.SPELLBOOK));
			setup.addProperty("name", title);

			final JsonObject payload = new JsonObject();
			payload.addProperty("ingestKey", clanCode);
			payload.addProperty("title", title);
			payload.add("setup", setup);

			executor.execute(() -> post(payload, onResult));
		});
	}

	private JsonArray items(ItemContainer container, int size)
	{
		final JsonArray out = new JsonArray();
		final Item[] items = container == null ? new Item[0] : container.getItems();
		for (int i = 0; i < size; i++)
		{
			final Item item = i < items.length ? items[i] : null;
			if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
			{
				out.add(JsonNull.INSTANCE);
				continue;
			}

			final JsonObject json = new JsonObject();
			json.addProperty("id", item.getId());
			if (item.getQuantity() > 1)
			{
				json.addProperty("q", item.getQuantity());
			}
			json.addProperty("name", itemManager.getItemComposition(item.getId()).getName());
			out.add(json);
		}
		return out;
	}

	private void post(JsonObject payload, Consumer<String> onResult)
	{
		final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/gear");
		if (url == null)
		{
			return;
		}

		final Request request = new Request.Builder().url(url).post(RequestBody.create(JSON, payload.toString())).build();
		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful() || response.body() == null)
			{
				onResult.accept("setupa ni bilo mogoče poslati (HTTP " + response.code() + ").");
				return;
			}

			final JsonObject body = new JsonParser().parse(response.body().string()).getAsJsonObject();
			onResult.accept(body.has("url") ? "setup shranjen: " + body.get("url").getAsString() : "setup shranjen.");
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Could not send gear setup", e);
			onResult.accept("setupa ni bilo mogoče poslati — stran ni dosegljiva.");
		}
	}
}
