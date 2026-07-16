package com.nexloottracker;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.HitsplatApplied;

import java.util.Set;

/**
 * Tracks local and total damage dealt to Nex during the current kill.
 * Mirrors DPS Counter boss damage accounting without reading that plugin directly.
 */
public class NexKillContributionTracker
{
	private int localDamage;
	private int totalDamage;
	private Long fightStartMs;
	private Long fightEndMs;

	public void reset()
	{
		localDamage = 0;
		totalDamage = 0;
		fightStartMs = null;
		fightEndMs = null;
	}

	public void onHitsplatApplied(HitsplatApplied event, Client client, Set<Integer> nexNpcIds)
	{
		final Actor actor = event.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}

		if (!nexNpcIds.contains(((NPC) actor).getId()))
		{
			return;
		}

		final Hitsplat hitsplat = event.getHitsplat();
		if (hitsplat == null)
		{
			return;
		}

		final Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}

		final int amount = hitsplat.getAmount();
		if (amount <= 0)
		{
			return;
		}

		if (hitsplat.isMine())
		{
			localDamage += amount;
			totalDamage += amount;
			markFightStarted();
		}
		else if (hitsplat.isOthers())
		{
			totalDamage += amount;
			markFightStarted();
		}
	}

	public void markFightEnded()
	{
		if (fightStartMs != null && fightEndMs == null)
		{
			fightEndMs = System.currentTimeMillis();
		}
	}

	public Long getKillDurationMs()
	{
		if (fightStartMs == null || fightEndMs == null)
		{
			return null;
		}

		return Math.max(0L, fightEndMs - fightStartMs);
	}

	private void markFightStarted()
	{
		if (fightStartMs == null)
		{
			fightStartMs = System.currentTimeMillis();
		}
	}

	public Double getContributionPercent()
	{
		if (totalDamage <= 0)
		{
			return null;
		}

		final double percent = (localDamage * 100.0) / totalDamage;
		if (Double.isNaN(percent) || Double.isInfinite(percent))
		{
			return null;
		}

		return Math.max(0.0, Math.min(100.0, percent));
	}

	int getLocalDamage()
	{
		return localDamage;
	}

	int getTotalDamage()
	{
		return totalDamage;
	}
}
