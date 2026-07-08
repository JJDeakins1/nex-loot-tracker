package com.nexloottracker;

import net.runelite.api.Client;
import net.runelite.api.WorldType;

public final class WorldUtils
{
	private WorldUtils()
	{
	}

	public static boolean playerOnBetaWorld(Client client)
	{
		return client.getWorldType().contains(WorldType.BETA_WORLD);
	}
}
