package com.triglav.clan;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(TriglavConfig.GROUP)
public interface TriglavConfig extends Config
{
	String GROUP = "triglav";

	@ConfigItem(
		keyName = "clanCode",
		name = "Klanska koda",
		description = "Tvoja koda TRG-XXXX s strani (clan.kokalj.dev/profil → Pokaži mojo kodo). Ista koda velja na "
			+ "vseh tvojih računalnikih. Koda je kot geslo — kdor jo ima, lahko v tvojem imenu pošilja drope na stran. "
			+ "Ne deli je in je ne kaži na streamu; če jo je kdo videl, jo na profilu zamenjaj.",
		position = 0,
		// Entered only in the TRIGLAV side panel (naročnik: "naj se vpisuje samo v panelu"); stored here.
		hidden = true
	)
	default String clanCode()
	{
		return "";
	}

	@ConfigItem(
		keyName = "sendScreenshots",
		name = "Pošiljaj screenshote",
		description = "Priloži sliko dropom nad pragom in drugim dogodkom (smrt, pet, clog, level), enako kot Dink. "
			+ "Slika pokaže tvoje uporabniško ime, chat in inventar/opremo v tistem trenutku.",
		position = 1
	)
	default boolean sendScreenshots()
	{
		return true;
	}

	@ConfigItem(
		keyName = "inGameMessages",
		name = "Prikaži obvestila v igri",
		description = "Izpiši v chat kratka obvestila s strani (dogodki, bingo, LFG).",
		position = 2
	)
	default boolean inGameMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sendDeaths",
		name = "Pošiljaj smrti",
		description = "Pošlji dogodek, ko tvoj lik umre (za lestvico smrti na strani).",
		position = 3
	)
	default boolean sendDeaths()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bingoOverlay",
		name = "Bingo tabla v igri",
		description = "Med aktivnim klanskim bingom pokaži tablo tvoje ekipe (zeleno potrjeno, rumeno čaka na pregled). Premakneš jo z Alt+vleci.",
		position = 4
	)
	default boolean bingoOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lfgReminders",
		name = "Opomniki za LFG",
		description = "Ko se pridružiš LFG-ju, ti 10 minut pred začetkom izpiše opomnik v chat (in preveri opremo po priporočenem setupu), "
			+ "ob strani minimape pa pokaže odštevanje.",
		position = 5
	)
	default boolean lfgReminders()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoSendGear",
		name = "Samodejno pošlji setup",
		description = "Ko zamenjaš opremo, jo čez pol minute sam pošlje na stran kot en zapis »Samodejno: tvoj RSN« (ne ustvarja novih). "
			+ "Pošlje opremo, inventar in spellbook. Privzeto izklopljeno.",
		position = 6
	)
	default boolean autoSendGear()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareLocation",
		name = "Deli mojo lokacijo s klanom",
		description = "Zemljevid v živo na clan.kokalj.dev/zemljevid/zivo pokaže tvoj RSN in kje stojiš, vidijo ga samo prijavljeni člani klana. "
			+ "Pošlje se na ~30 s, brez zgodovine poti, in izgine 2 minuti po zadnjem pošiljanju. Ne pošilja se v Wildernessu, na PvP/Deadman svetovih "
			+ "in v instancah (raidi). Ob izklopu, odjavi in vstopu v nevarno območje se takoj izbriše. Privzeto izklopljeno.",
		position = 7
	)
	default boolean shareLocation()
	{
		return false;
	}
}
