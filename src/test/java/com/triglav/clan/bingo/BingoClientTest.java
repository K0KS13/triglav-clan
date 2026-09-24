package com.triglav.clan.bingo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import org.junit.Test;

public class BingoClientTest
{
	private static JsonObject json(String s)
	{
		return new JsonParser().parse(s).getAsJsonObject();
	}

	@Test
	public void noActiveBingo()
	{
		assertNull(BingoClient.parse(json("{\"bingo\":null}")));
		assertNull(BingoClient.parse(json("{}")));
	}

	@Test
	public void parsesBoard()
	{
		final BingoBoard board = BingoClient.parse(json("{"
			+ "\"bingo\":{\"id\":\"b1\",\"title\":\"Jesenski bingo\",\"rows\":2,\"cols\":2,\"endsAt\":null},"
			+ "\"team\":{\"name\":\"Ekipa 1\",\"color\":\"#3366ff\"},"
			+ "\"tiles\":["
			+ "{\"index\":0,\"title\":\"Bandos page\",\"points\":1,\"iconItemId\":11832,\"itemIds\":[11832,11834],\"status\":\"done\"},"
			+ "{\"index\":1,\"title\":\"Pet\",\"points\":3,\"iconItemId\":null,\"itemIds\":[],\"status\":\"pending\"},"
			+ "{\"index\":2,\"title\":\"Dragon warhammer\",\"points\":2,\"iconItemId\":13576,\"itemIds\":[13576],\"status\":\"open\"}"
			+ "]}"));

		assertEquals("Jesenski bingo", board.title);
		assertEquals(2, board.rows);
		assertEquals(2, board.cols);
		assertEquals("Ekipa 1", board.teamName);
		assertEquals(new Color(0x3366ff), board.teamColor);
		assertEquals(3, board.tiles.size());
		assertEquals(BingoBoard.Status.DONE, board.tiles.get(0).status);
		assertEquals(BingoBoard.Status.PENDING, board.tiles.get(1).status);
		assertEquals(-1, board.tiles.get(1).iconItemId);
		assertEquals(BingoBoard.Status.OPEN, board.tiles.get(2).status);
	}

	@Test
	public void openTileMatchIgnoresFinishedTiles()
	{
		final BingoBoard board = BingoClient.parse(json("{"
			+ "\"bingo\":{\"title\":\"B\",\"rows\":1,\"cols\":2},"
			+ "\"tiles\":["
			+ "{\"index\":0,\"title\":\"a\",\"itemIds\":[11832],\"status\":\"done\"},"
			+ "{\"index\":1,\"title\":\"b\",\"itemIds\":[13576],\"status\":\"open\"}"
			+ "]}"));

		assertFalse(board.hasOpenTileFor(11832));
		assertTrue(board.hasOpenTileFor(13576));
		assertFalse(board.hasOpenTileFor(4151));
	}
}
