package com.triglav.clan;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Starts a RuneLite client with this plugin already loaded. Used by the {@code run} Gradle task
 * and as the main class of the standalone shadow jar.
 */
public class TriglavLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TriglavPlugin.class);
		RuneLite.main(args);
	}
}
