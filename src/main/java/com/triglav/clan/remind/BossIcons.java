package com.triglav.clan.remind;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Picks a boss from an LFG or event title and returns the item id of its pet, whose icon the
 * countdown box shows so a glance at the minimap says which boss is next. The ids were checked
 * against the OSRS wiki's item lookup; anything that does not match falls back to the plugin icon.
 * Short aliases ("kq", "kbd", "sire") are matched as whole words so "kqueen" or "firesire" do not hit.
 */
public final class BossIcons
{
	private static final class Boss
	{
		final Pattern pattern;
		final int itemId;

		Boss(String regex, int itemId)
		{
			this.pattern = Pattern.compile("\\b(?:" + regex + ")\\b", Pattern.CASE_INSENSITIVE);
			this.itemId = itemId;
		}
	}

	private static final List<Boss> BOSSES = new ArrayList<>();

	static
	{
		// Longer names before the short ones they contain (sarachnis before sara).
		add("zulrah|zul", 12921);
		add("vorkath|vorki", 21992);
		add("cerberus|cerb", 13247);
		add("kraken", 12655);
		add("callisto", 13178);
		add("venenatis", 13177);
		add("vet'?ion", 13179);
		add("scorpia", 13181);
		add("chaos elemental", 11995);
		add("dagannoth prime|prime", 12644);
		add("dagannoth rex|rex", 12645);
		add("dagannoth supreme|supreme|daggs?|dks", 12643);
		add("giant mole|mole", 12646);
		add("king black dragon|kbd", 12653);
		add("kalphite queen|kq", 12654);
		add("sarachnis", 23495);
		add("zilyana|saradomin|sara", 12651);
		add("graardor|bandos", 12650);
		add("kree'?arra|armadyl|arma", 12649);
		add("k'?ril|zamorak|zammy", 12652);
		add("corporeal beast|corp", 12816);
		add("nex", 26348);
		add("jad|fight caves?|tzhaar", 13225);
		add("inferno|zuk", 21291);
		add("alchemical hydra|hydra", 22746);
		add("gauntlet|cg|corrupted gauntlet", 23757);
		add("nightmare|nm", 24491);
		add("theatre of blood|tob", 22473);
		add("chambers of xeric|cox|olm", 20851);
		add("tombs of amascut|toa", 25348);
		add("thermonuclear|thermy|smoke devil", 12648);
		add("abyssal sire|sire", 13262);
		add("grotesque guardians|grotesque|gg", 21748);
		add("skotizo", 21273);
		add("wintertodt", 20693);
		add("tempoross", 25602);
		add("zalcano", 23760);
	}

	private BossIcons()
	{
	}

	private static void add(String regex, int itemId)
	{
		BOSSES.add(new Boss(regex, itemId));
	}

	/** @return the boss pet's item id for the first title that names a boss, or -1 */
	public static int itemIdFor(String... texts)
	{
		for (String text : texts)
		{
			if (text == null)
			{
				continue;
			}

			for (Boss boss : BOSSES)
			{
				if (boss.pattern.matcher(text).find())
				{
					return boss.itemId;
				}
			}
		}
		return -1;
	}
}
