package com.triglav.clan;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import com.triglav.clan.bingo.BingoBoard;
import com.triglav.clan.bingo.BingoClient;
import com.triglav.clan.bingo.BingoOverlay;
import com.triglav.clan.collect.ClanRankReporter;
import com.triglav.clan.collect.ClueChat;
import com.triglav.clan.collect.CollectionLogChat;
import com.triglav.clan.collect.CombatTaskTracker;
import com.triglav.clan.collect.DeathTracker;
import com.triglav.clan.collect.DiarySnapshot;
import com.triglav.clan.collect.KillCountTracker;
import com.triglav.clan.collect.LevelTracker;
import com.triglav.clan.collect.LootCollector;
import com.triglav.clan.collect.LootValue;
import com.triglav.clan.collect.PetDetector;
import com.triglav.clan.collect.PlayerKillTracker;
import com.triglav.clan.collect.QuestTracker;
import com.triglav.clan.collect.SlayerChat;
import com.triglav.clan.collect.Screenshot;
import com.triglav.clan.collect.SkillSnapshot;
import com.triglav.clan.collect.SlayerSnapshot;
import com.triglav.clan.feed.FeedClient;
import com.triglav.clan.gear.GearClient;
import com.triglav.clan.net.ApiClient;
import com.triglav.clan.net.ConfigClient;
import com.triglav.clan.net.Envelope;
import com.triglav.clan.net.KeyStore;
import com.triglav.clan.util.GameText;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.clan.ClanID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import net.runelite.http.api.loottracker.LootRecordType;

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
	private KeyStore keyStore;

	@Inject
	private ConfigManager configManager;

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

	@Inject
	private PetDetector petDetector;

	@Inject
	private DeathTracker deathTracker;

	@Inject
	private LevelTracker levelTracker;

	@Inject
	private CombatTaskTracker combatTaskTracker;

	@Inject
	private SlayerChat slayerChat;

	@Inject
	private QuestTracker questTracker;

	@Inject
	private PlayerKillTracker playerKillTracker;

	@Inject
	private BingoClient bingoClient;

	@Inject
	private BingoOverlay bingoOverlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private GearClient gearClient;

	private static final int SCREENSHOT_TIMEOUT_SECONDS = 2;

	private NavigationButton navButton;
	private GameState lastGameState;
	private volatile boolean loginPending;
	private String lastPlayerName;

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

		panel.showLinked(keyStore.isLinked(), null);
		panel.setOnLink(this::linkCode);
		panel.setOnSendGear(title -> gearClient.sendCurrentSetup(title, result -> chat("TRIGLAV: " + result)));
		feedClient.setSink(this::showFeedMessage);
		overlayManager.add(bingoOverlay);
		// Enabled while already in game: send the login snapshot on the next tick.
		lastGameState = client.getGameState();
		loginPending = lastGameState == GameState.LOGGED_IN;

		// Re-check the stored code on every start: picks up a code changed on the site ("zamenjaj kodo").
		keyStore.refresh(result -> onLinkResult(result, false));
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(bingoOverlay);
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
			// The local player (and its name) isn't loaded yet on this event; onGameTick sends it.
			loginPending = true;
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
		// Only the game's own messages: a player could otherwise type "Your Zulrah kill count is:
		// 1000" in public chat and have it reported as theirs.
		final ChatMessageType type = event.getType();
		if (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM
			&& type != ChatMessageType.FRIENDSCHATNOTIFICATION)
		{
			return;
		}

		killCountTracker.onChatMessage(event);
		petDetector.onChatMessage(event);

		final String clogItem = CollectionLogChat.newItemName(event);
		if (clogItem != null)
		{
			final JsonObject extra = new JsonObject();
			extra.addProperty("itemName", clogItem);
			apiClient.send(Envelope.create(client, "COLLECTION", extra), null);
		}

		final JsonObject clue = ClueChat.parse(event);
		if (clue != null)
		{
			apiClient.send(Envelope.create(client, "CLUE", clue), null);
		}

		final JsonObject slayer = slayerChat.parse(event);
		if (slayer != null)
		{
			apiClient.send(Envelope.create(client, "SLAYER", slayer), null);
		}

		// The combat achievement point varbits are only updated a tick after the message.
		final String message = Text.removeTags(event.getMessage());
		if (message.startsWith("Congratulations, you've completed a"))
		{
			clientThread.invokeLater(() ->
			{
				final JsonObject task = combatTaskTracker.parse(client, message);
				if (task != null)
				{
					apiClient.send(Envelope.create(client, "COMBAT_ACHIEVEMENT", task), null);
				}
			});
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		levelTracker.onStatChanged(event);
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		playerKillTracker.onHitsplat(client, event);
	}

	/** The quest completion scroll; its counts land one tick later, hence the invokeLater. */
	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() != QuestTracker.WIDGET_GROUP)
		{
			return;
		}

		final String title = questTracker.title(client);
		clientThread.invokeLater(() ->
			apiClient.send(Envelope.create(client, "QUEST", questTracker.build(client, title)), null));
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		sendLoot(lootCollector.build(event));
	}

	/**
	 * Loot that is not an NPC drop: raid chests, clue caskets, implings, pickpocketing. NPC loot
	 * also fires this event, but it is already handled above (with the kill count attached), and a
	 * killed player is reported as a PK rather than as loot.
	 */
	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		if (event.getType() == LootRecordType.NPC || event.getType() == LootRecordType.PLAYER)
		{
			return;
		}

		sendLoot(lootCollector.build(event.getName(), event.getType().name(), event.getItems()));
	}

	private void sendLoot(JsonObject extra)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		final JsonObject envelope = Envelope.create(client, "LOOT", extra);

		final BingoBoard board = bingoClient.board();
		if (board != null)
		{
			for (JsonElement item : extra.getAsJsonArray("items"))
			{
				if (board.hasOpenTileFor(item.getAsJsonObject().get("id").getAsInt()))
				{
					bingoClient.refreshSoon();
					break;
				}
			}
		}

		if (!config.sendScreenshots() || LootValue.total(extra.getAsJsonArray("items")) < configClient.current().screenshotMinValue)
		{
			apiClient.send(envelope, null);
			return;
		}

		captureScreenshot(png -> apiClient.send(envelope, png));
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		final String petName = petDetector.onNpcSpawned(client, event);
		if (petName != null)
		{
			final JsonObject extra = new JsonObject();
			extra.addProperty("petName", petName);
			apiClient.send(Envelope.create(client, "PET", extra), null);
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() == client.getLocalPlayer())
		{
			deathTracker.onLocalPlayerDied(client);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		final String name = Envelope.playerName(client);
		if (name != null)
		{
			lastPlayerName = name;
			if (loginPending)
			{
				loginPending = false;
				sendLogin();
				clanRankReporter.reportNow();
			}
		}

		petDetector.onGameTick();

		final JsonObject deathExtra = deathTracker.onGameTick(client);
		if (deathExtra != null && config.sendDeaths())
		{
			apiClient.send(Envelope.create(client, "DEATH", deathExtra), null);
		}

		final JsonObject levelExtra = levelTracker.onGameTick(client);
		if (levelExtra != null)
		{
			apiClient.send(Envelope.create(client, "LEVEL", levelExtra), null);
		}

		final JsonObject killCountExtra = killCountTracker.onGameTick();
		if (killCountExtra != null)
		{
			apiClient.send(Envelope.create(client, "KILL_COUNT", killCountExtra), null);
		}

		final List<JsonObject> kills = playerKillTracker.onGameTick(client);
		if (kills != null)
		{
			for (JsonObject kill : kills)
			{
				apiClient.send(Envelope.create(client, "PLAYER_KILL", kill), null);
			}
		}
	}

	private void sendLogin()
	{
		// The level-up tracker needs a baseline: StatChanged fires for every skill right after login.
		levelTracker.prime(client);

		final JsonObject extra = new JsonObject();
		extra.add("skills", SkillSnapshot.build(client));
		extra.add("slayer", SlayerSnapshot.build(client));
		extra.add("achievementDiary", DiarySnapshot.build(client));
		apiClient.send(Envelope.create(client, "LOGIN", extra), null);
		log.debug("TRIGLAV: sent LOGIN snapshot");
	}

	private void sendLogout()
	{
		// The player is already gone at this point, so use the name seen while logged in.
		apiClient.send(Envelope.create(client, "LOGOUT", new JsonObject(), lastPlayerName), null);
		log.debug("TRIGLAV: sent LOGOUT");

		// Half-finished state must not carry over to the next character or world.
		levelTracker.reset();
		killCountTracker.reset();
		slayerChat.reset();
		playerKillTracker.reset();
	}

	/** The clan channel loads a few seconds after login; report the ranks once it's actually there. */
	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		if (!event.isGuest() && event.getClanId() == ClanID.CLAN && event.getClanChannel() != null)
		{
			clanRankReporter.reportNow();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (TriglavConfig.GROUP.equals(event.getGroup()) && "clanCode".equals(event.getKey()))
		{
			keyStore.refresh(result -> onLinkResult(result, true));
		}
	}

	/** Panel "Poveži": storing the code fires ConfigChanged, which does the actual linking. */
	private void linkCode(String code)
	{
		if (code.isEmpty())
		{
			chat("TRIGLAV: vpiši svojo kodo s profila na clan.kokalj.dev (TRG-XXXX).");
			return;
		}

		if (code.equals(config.clanCode()))
		{
			keyStore.refresh(result -> onLinkResult(result, true));
		}
		else
		{
			configManager.setConfiguration(TriglavConfig.GROUP, "clanCode", code);
		}
	}

	/** @param announce true when the member just typed a code, false for the quiet check at startup */
	private void onLinkResult(KeyStore.Result result, boolean announce)
	{
		panel.showLinked(keyStore.isLinked(), keyStore.linkedName());

		switch (result)
		{
			case LINKED:
				configClient.refreshNow();
				if (announce && client.getGameState() == GameState.LOGGED_IN)
				{
					// Linked mid-session: the login snapshot went nowhere without a key, so send it now.
					loginPending = true;
				}
				if (announce)
				{
					chat("TRIGLAV: povezano" + (keyStore.linkedName() == null ? "." : " kot " + keyStore.linkedName() + "."));
				}
				break;
			case WRONG_CODE:
				chat("TRIGLAV: koda ne velja. Preveri jo na clan.kokalj.dev/profil (Pokaži mojo kodo).");
				break;
			case UNREACHABLE:
				if (announce)
				{
					chat("TRIGLAV: stran trenutno ni dosegljiva, poskusi čez nekaj minut.");
				}
				break;
			default:
				break;
		}
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
		final String text = GameText.ascii(message);
		clientThread.invoke(() -> client.addChatMessage(ChatMessageType.CONSOLE, "", text, null));
	}
}
