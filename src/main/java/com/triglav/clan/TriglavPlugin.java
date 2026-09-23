package com.triglav.clan;

import com.google.gson.JsonObject;
import com.google.inject.Provides;
import com.triglav.clan.collect.ClanRankReporter;
import com.triglav.clan.collect.KillCountTracker;
import com.triglav.clan.collect.LootCollector;
import com.triglav.clan.collect.LootValue;
import com.triglav.clan.collect.Screenshot;
import com.triglav.clan.collect.SkillSnapshot;
import com.triglav.clan.feed.FeedClient;
import com.triglav.clan.net.ApiClient;
import com.triglav.clan.net.ConfigClient;
import com.triglav.clan.net.Envelope;
import com.triglav.clan.net.PairingClient;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "TRIGLAV Clan",
	description = "Ena klanska koda namesto Dinka: povezava racuna, klanski ranki in obvestila v igri",
	tags = {"triglav", "clan", "dink", "notification", "rank", "loot"}
)
public class TriglavPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private TriglavConfig config;

	@Inject
	private TriglavPanel panel;

	@Inject
	private ApiClient apiClient;

	@Inject
	private LootCollector lootCollector;

	@Inject
	private KillCountTracker killCountTracker;

	@Inject
	private PairingClient pairingClient;

	@Inject
	private ClanRankReporter clanRankReporter;

	@Inject
	private FeedClient feedClient;

	@Inject
	private ConfigClient configClient;

	@Inject
	private DrawManager drawManager;

	@Inject
	private ScheduledExecutorService executor;

	private static final int SCREENSHOT_TIMEOUT_SECONDS = 2;

	private NavigationButton navButton;
	private GameState lastGameState;

	@Override
	protected void startUp()
	{
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");

		navButton = NavigationButton.builder()
			.tooltip("TRIGLAV")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		panel.update(apiClient.isPaired());
		panel.setOnPair(this::startPairing);
		feedClient.setSink(this::showFeedMessage);
		lastGameState = null;
	}

	@Override
	protected void shutDown()
	{
		pairingClient.cancel();
		clientToolbar.removeNavigation(navButton);
	}

	@Provides
	TriglavConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TriglavConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		final GameState state = event.getGameState();

		if (state == GameState.LOGGED_IN && lastGameState != GameState.LOGGED_IN)
		{
			sendLogin();
			clanRankReporter.reportNow();
		}
		else if (lastGameState == GameState.LOGGED_IN
			&& (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING || state == GameState.CONNECTION_LOST))
		{
			sendLogout();
		}

		lastGameState = state;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		killCountTracker.onChatMessage(event);
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		final JsonObject extra = lootCollector.build(client, event);
		final JsonObject envelope = Envelope.create(client, "LOOT", extra);

		if (!config.sendScreenshots() || LootValue.total(extra.getAsJsonArray("items")) < configClient.current().screenshotMinValue)
		{
			apiClient.send(envelope, null);
			return;
		}

		captureScreenshot(png -> apiClient.send(envelope, png));
	}

	private void sendLogin()
	{
		final JsonObject extra = new JsonObject();
		extra.add("skills", SkillSnapshot.build(client));
		apiClient.send(Envelope.create(client, "LOGIN", extra), null);
		log.debug("TRIGLAV: sent LOGIN snapshot");
	}

	private void sendLogout()
	{
		apiClient.send(Envelope.create(client, "LOGOUT", new JsonObject()), null);
		log.debug("TRIGLAV: sent LOGOUT");
	}

	private void startPairing()
	{
		pairingClient.start(
			code -> panel.showPairingCode(code),
			ok ->
			{
				panel.update(ok && apiClient.isPaired());
				chat(ok ? "TRIGLAV: račun povezan." : "TRIGLAV: parjenje ni uspelo, poskusi znova.");
			});
	}

	private void showFeedMessage(String text)
	{
		chat("TRIGLAV: " + text);
	}

	/**
	 * The frame only exists once the next one is drawn; if that never happens (minimised window)
	 * the event still ships, just without a screenshot.
	 */
	private void captureScreenshot(Consumer<byte[]> onCaptured)
	{
		final AtomicBoolean done = new AtomicBoolean();

		drawManager.requestNextFrameListener(image ->
		{
			if (!done.compareAndSet(false, true))
			{
				return;
			}

			final BufferedImage buffered = ImageUtil.bufferedImageFromImage(image);
			executor.execute(() ->
			{
				try
				{
					onCaptured.accept(Screenshot.encode(buffered));
				}
				catch (IOException e)
				{
					log.warn("Could not encode screenshot", e);
					onCaptured.accept(null);
				}
			});
		});

		executor.schedule(() ->
		{
			if (done.compareAndSet(false, true))
			{
				onCaptured.accept(null);
			}
		}, SCREENSHOT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}

	private void chat(String message)
	{
		clientThread.invoke(() -> client.addChatMessage(ChatMessageType.CONSOLE, "", message, null));
	}
}
