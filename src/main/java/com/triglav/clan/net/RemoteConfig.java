package com.triglav.clan.net;

/** Snapshot of GET /api/plugin/config/:key (docs/plugin-brief.md §4). */
public final class RemoteConfig
{
	public static final RemoteConfig DEFAULT = new RemoteConfig(500_000, 60);

	public final int screenshotMinValue;
	public final int pollSeconds;

	public RemoteConfig(int screenshotMinValue, int pollSeconds)
	{
		this.screenshotMinValue = screenshotMinValue;
		this.pollSeconds = pollSeconds;
	}
}
