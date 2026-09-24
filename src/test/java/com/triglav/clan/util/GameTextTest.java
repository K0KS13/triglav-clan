package com.triglav.clan.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GameTextTest
{
	@Test
	public void foldsSlovenianLettersForTheGameFont()
	{
		assertEquals("Cez 15 min: Callisto mass - prijavi se na strani", GameText.ascii("Čez 15 min: Callisto mass — prijavi se na strani"));
		assertEquals("racun povezan, poslji, kozuh", GameText.ascii("račun povezan, pošlji, kožuh"));
		assertEquals("\"Zamorak\"", GameText.ascii("»Zamorak«"));
		assertEquals("", GameText.ascii(null));
	}
}
