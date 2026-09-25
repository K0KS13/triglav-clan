package com.triglav.clan.bingo;

import java.awt.Color;
import java.util.List;

/** Immutable snapshot of GET /api/plugin/bingo/:key for the member's team. */
public final class BingoBoard
{
	public enum Status
	{
		OPEN, PENDING, DONE
	}

	public static final class Tile
	{
		public final int index;
		public final String title;
		public final int points;
		public final int iconItemId;
		public final int[] itemIds;
		public final Status status;

		public Tile(int index, String title, int points, int iconItemId, int[] itemIds, Status status)
		{
			this.index = index;
			this.title = title;
			this.points = points;
			this.iconItemId = iconItemId;
			this.itemIds = itemIds;
			this.status = status;
		}

		/** @return the tile's icon, else its first item, else -1 (the overlay then shows initials) */
		public int displayItemId()
		{
			if (iconItemId > 0)
			{
				return iconItemId;
			}
			return itemIds.length > 0 ? itemIds[0] : -1;
		}
	}

	public final String title;
	public final int rows;
	public final int cols;
	public final String teamName;
	public final Color teamColor;
	public final List<Tile> tiles;

	public BingoBoard(String title, int rows, int cols, String teamName, Color teamColor, List<Tile> tiles)
	{
		this.title = title;
		this.rows = rows;
		this.cols = cols;
		this.teamName = teamName;
		this.teamColor = teamColor;
		this.tiles = tiles;
	}

	/** @return true if some tile this team hasn't finished yet would be completed by this item */
	public boolean hasOpenTileFor(int itemId)
	{
		for (Tile tile : tiles)
		{
			if (tile.status == Status.DONE)
			{
				continue;
			}
			for (int id : tile.itemIds)
			{
				if (id == itemId)
				{
					return true;
				}
			}
		}
		return false;
	}
}
