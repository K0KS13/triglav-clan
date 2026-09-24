package com.triglav.clan.util;

import java.text.Normalizer;

/**
 * The game's own fonts (chat box, overlays) have no č/š/ž — they render as wrong glyphs. Text shown
 * inside the game is therefore folded to plain ASCII ("Čez 15 min" -> "Cez 15 min"). The side panel
 * uses RuneLite's Swing font and keeps the real letters.
 */
public final class GameText
{
	private GameText()
	{
	}

	public static String ascii(String text)
	{
		if (text == null)
		{
			return "";
		}

		final String folded = Normalizer.normalize(text, Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")
			.replace('—', '-')
			.replace('–', '-')
			.replace('»', '"')
			.replace('«', '"')
			.replace('„', '"')
			.replace('“', '"')
			.replace('”', '"')
			.replace('’', '\'');

		final StringBuilder out = new StringBuilder(folded.length());
		for (char c : folded.toCharArray())
		{
			if (c >= 32 && c < 127)
			{
				out.append(c);
			}
		}
		return out.toString();
	}
}
