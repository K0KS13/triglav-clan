package com.triglav.clan.share;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.VarbitID;
import org.junit.Before;
import org.junit.Test;

public class LocationSharerTest
{
	private Client client;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS));
	}

	@Test
	public void anOrdinaryWorldMayShare()
	{
		assertFalse(LocationSharer.isSensitive(client, false));
	}

	@Test
	public void theWildernessNeverShares()
	{
		when(client.getVarbitValue(VarbitID.INSIDE_WILDERNESS)).thenReturn(1);
		assertTrue(LocationSharer.isSensitive(client, false));
	}

	@Test
	public void instancesNeverShareEvenWithTheExtraTick()
	{
		when(client.isInInstancedRegion()).thenReturn(true);
		assertTrue(LocationSharer.isSensitive(client, false));
		assertTrue(LocationSharer.isSensitive(client, true));
	}

	@Test
	public void theExtraTickAllowsTheWildernessAndPvpWorlds()
	{
		when(client.getVarbitValue(VarbitID.INSIDE_WILDERNESS)).thenReturn(1);
		assertTrue(LocationSharer.isSensitive(client, false));
		assertFalse(LocationSharer.isSensitive(client, true));

		when(client.getVarbitValue(VarbitID.INSIDE_WILDERNESS)).thenReturn(0);
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS, WorldType.DEADMAN));
		assertTrue(LocationSharer.isSensitive(client, false));
		assertFalse(LocationSharer.isSensitive(client, true));
	}

	@Test
	public void pvpStyleWorldsNeverShare()
	{
		for (WorldType type : new WorldType[]{WorldType.PVP, WorldType.BOUNTY, WorldType.HIGH_RISK, WorldType.DEADMAN, WorldType.LAST_MAN_STANDING, WorldType.PVP_ARENA, WorldType.TOURNAMENT_WORLD})
		{
			when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS, type));
			assertTrue(type + " should be sensitive", LocationSharer.isSensitive(client, false));
		}
	}
}
