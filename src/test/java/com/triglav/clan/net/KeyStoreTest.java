package com.triglav.clan.net;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeyStoreTest
{
	@Test
	public void recognisesClanCodesTypedLoosely()
	{
		assertTrue(KeyStore.isClanCode("TRG-2A3B"));
		assertTrue(KeyStore.isClanCode("trg-2a3b"));
		assertTrue(KeyStore.isClanCode("trg 2a3b"));
		assertTrue(KeyStore.isClanCode("TRG2A3B"));
	}

	@Test
	public void rawIngestKeysAreNotClanCodes()
	{
		assertFalse(KeyStore.isClanCode("cmuadfp5z0001h0epngkgs6ad"));
		assertFalse(KeyStore.isClanCode("550e8400-e29b-41d4-a716-446655440000"));
		assertFalse(KeyStore.isClanCode("TRG-2A3B4"));
	}
}
