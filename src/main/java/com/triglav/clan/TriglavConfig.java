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
		description = "Nastavi se sama, ko v pluginu klikneš Poveži račun in kodo vneseš na strani (/profil). "
			+ "Lahko jo tudi ročno prilepiš s strani, če povezovanje s klikom ne deluje.",
		position = 0
	)
	default String clanCode()
	{
		return "";
	}

	@ConfigItem(
		keyName = "sendScreenshots",
		name = "Pošiljaj screenshote",
		description = "Priloži sliko dropom nad pragom in drugim dogodkom (smrt, pet, clog, level), enako kot Dink.",
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
}
