package com.triglav.clan.bingo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class BingoAnnounceTest
{
	private static BingoBoard board(String title, BingoBoard.Status first, BingoBoard.Status second)
	{
		return new BingoBoard(title, 1, 2, "Ekipa", new Color(0xd8a93f), Arrays.asList(
			new BingoBoard.Tile(0, "Dragon pickaxe", 5, -1, new int[0], first),
			new BingoBoard.Tile(1, "Pet", 3, -1, new int[0], second)));
	}

	@Test
	public void announcesOnlyTilesThatJustTurnedDone()
	{
		final List<String> messages = BingoClient.newlyDone(
			board("Bingo", BingoBoard.Status.OPEN, BingoBoard.Status.DONE),
			board("Bingo", BingoBoard.Status.DONE, BingoBoard.Status.DONE));
		assertEquals(1, messages.size());
		assertTrue(messages.get(0).contains("Dragon pickaxe"));
		assertTrue(messages.get(0).contains("+5"));
	}

	@Test
	public void staysQuietOnTheFirstLoadAndAcrossDifferentBingos()
	{
		assertTrue(BingoClient.newlyDone(null, board("Bingo", BingoBoard.Status.DONE, BingoBoard.Status.DONE)).isEmpty());
		assertTrue(BingoClient.newlyDone(board("Star", BingoBoard.Status.OPEN, BingoBoard.Status.OPEN),
			board("Nov", BingoBoard.Status.DONE, BingoBoard.Status.DONE)).isEmpty());
	}

	@Test
	public void pendingIsNotAnnouncedAsDone()
	{
		assertTrue(BingoClient.newlyDone(board("Bingo", BingoBoard.Status.OPEN, BingoBoard.Status.OPEN),
			board("Bingo", BingoBoard.Status.PENDING, BingoBoard.Status.OPEN)).isEmpty());
	}
}
