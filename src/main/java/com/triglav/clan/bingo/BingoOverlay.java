package com.triglav.clan.bingo;

import com.triglav.clan.TriglavConfig;
import com.triglav.clan.util.GameText;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/**
 * The team's bingo board in game (docs/plugin-brief.md §5.3): one square per tile with the
 * tile's item icon, green once the site has approved it, yellow while a proof waits for staff.
 * A tile without any item shows the initials of its title. Hovering a tile shows its title,
 * points and state. Movable like any overlay (Alt+drag).
 */
public class BingoOverlay extends Overlay
{
	private static final int TILE = 30;
	private static final int GAP = 2;
	private static final int HEADER = 16;
	private static final Color OPEN = new Color(30, 34, 45, 200);
	private static final Color PENDING = new Color(216, 169, 63, 170);
	private static final Color DONE = new Color(60, 160, 90, 190);

	private final BingoClient bingoClient;
	private final ItemManager itemManager;
	private final TriglavConfig config;
	private final Client client;
	private final TooltipManager tooltipManager;

	@Inject
	private BingoOverlay(BingoClient bingoClient, ItemManager itemManager, TriglavConfig config, Client client, TooltipManager tooltipManager)
	{
		this.bingoClient = bingoClient;
		this.itemManager = itemManager;
		this.config = config;
		this.client = client;
		this.tooltipManager = tooltipManager;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		final BingoBoard board = bingoClient.board();
		if (!config.bingoOverlay() || board == null || board.rows <= 0 || board.cols <= 0)
		{
			return null;
		}

		g.setFont(FontManager.getRunescapeSmallFont());
		final FontMetrics metrics = g.getFontMetrics();
		final String header = GameText.ascii(board.teamName.isEmpty() ? board.title : board.title + " - " + board.teamName);
		final int width = Math.max(board.cols * (TILE + GAP) - GAP, metrics.stringWidth(header));
		final int height = HEADER + board.rows * (TILE + GAP) - GAP;

		g.setColor(board.teamColor);
		g.drawString(header, 0, metrics.getAscent());

		final Point mouse = client.getMouseCanvasPosition();
		final Rectangle bounds = getBounds();
		BingoBoard.Tile hovered = null;

		for (BingoBoard.Tile tile : board.tiles)
		{
			final int row = tile.index / board.cols;
			final int col = tile.index % board.cols;
			if (row >= board.rows)
			{
				continue;
			}

			final int x = col * (TILE + GAP);
			final int y = HEADER + row * (TILE + GAP);

			g.setColor(tile.status == BingoBoard.Status.DONE ? DONE : tile.status == BingoBoard.Status.PENDING ? PENDING : OPEN);
			g.fillRect(x, y, TILE, TILE);

			final int itemId = tile.displayItemId();
			final BufferedImage icon = itemId > 0 ? itemManager.getImage(itemId) : null;
			if (icon != null)
			{
				g.drawImage(icon, x + (TILE - 24) / 2, y + (TILE - 22) / 2, 24, 22, null);
			}
			else
			{
				final String initials = initials(tile.title);
				g.setColor(Color.WHITE);
				g.drawString(initials, x + (TILE - metrics.stringWidth(initials)) / 2, y + (TILE + metrics.getAscent()) / 2 - 1);
			}

			if (mouse != null && bounds != null && new Rectangle(bounds.x + x, bounds.y + y, TILE, TILE).contains(mouse.getX(), mouse.getY()))
			{
				hovered = tile;
			}
		}

		if (hovered != null)
		{
			tooltipManager.add(new Tooltip(GameText.ascii(hovered.title) + "</br>" + hovered.points + " t - " + statusLabel(hovered.status)));
		}

		return new Dimension(width, height);
	}

	/** "Boss pet" -> "BP", "Hard clue unique" -> "HC", "Pet" -> "PE" */
	static String initials(String title)
	{
		final String[] words = GameText.ascii(title).trim().split("\\s+");
		if (words.length >= 2)
		{
			return (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase();
		}
		final String word = words[0];
		return word.isEmpty() ? "?" : word.substring(0, Math.min(2, word.length())).toUpperCase();
	}

	private static String statusLabel(BingoBoard.Status status)
	{
		switch (status)
		{
			case DONE:
				return "potrjeno";
			case PENDING:
				return "caka na pregled";
			default:
				return "odprto";
		}
	}
}
