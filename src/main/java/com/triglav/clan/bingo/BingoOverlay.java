package com.triglav.clan.bingo;

import com.triglav.clan.TriglavConfig;
import com.triglav.clan.util.GameText;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The team's bingo board in game (docs/plugin-brief.md §5.3): one square per tile with the
 * tile's item icon, green once the site has approved it, yellow while a proof waits for staff.
 * Movable like any overlay (Alt+drag).
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

	@Inject
	private BingoOverlay(BingoClient bingoClient, ItemManager itemManager, TriglavConfig config)
	{
		this.bingoClient = bingoClient;
		this.itemManager = itemManager;
		this.config = config;
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

			if (tile.iconItemId > 0)
			{
				final BufferedImage icon = itemManager.getImage(tile.iconItemId);
				if (icon != null)
				{
					g.drawImage(icon, x + (TILE - 24) / 2, y + (TILE - 22) / 2, 24, 22, null);
				}
			}
		}

		return new Dimension(width, height);
	}
}
