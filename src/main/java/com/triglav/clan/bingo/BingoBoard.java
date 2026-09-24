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
		public final int iconItemId;
		public final int[] itemIds;
		public final Status status;

		public Tile(int index, String title, int iconItemId, int[] itemIds, Status status)
		{
			this.index = index;
			this.title = title;
			this.iconItemId = iconItemId;
			this.itemIds = itemIds;
			this.status = status;
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
