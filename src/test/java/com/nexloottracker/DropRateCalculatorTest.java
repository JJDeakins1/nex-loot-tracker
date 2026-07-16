package com.nexloottracker;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DropRateCalculatorTest
{
	@Test
	public void getKillsSinceLastSeenUniqueCountsFromNewestUntilSeenDrop()
	{
		ArrayList<NexLootTracker> kills = new ArrayList<>();
		kills.add(killWithDate(1, "", ""));
		kills.add(killWithDate(2, "Torva full helm (damaged)", "Someone"));
		kills.add(killWithDate(3, "", ""));
		kills.add(killWithDate(4, "", ""));

		assertEquals(2, DropRateCalculator.getKillsSinceLastSeenUnique(kills));
	}

	@Test
	public void getEffectiveKillContributionAppliesTenPercentMvpBoost()
	{
		NexLootTracker kill = killWithContribution(1, 25.0);
		kill.setMvpInOwnName(true);

		assertEquals(27.5, DropRateCalculator.getEffectiveKillContribution(kill), 0.001);
	}

	@Test
	public void getEffectiveKillContributionAddsMvpBonus()
	{
		NexLootTracker kill = killWithContribution(1, 20.0);
		kill.setMvpInOwnName(true);

		assertEquals(22.0, DropRateCalculator.getEffectiveKillContribution(kill), 0.001);
	}

	@Test
	public void getEffectiveKillContributionCapsAtOneHundred()
	{
		NexLootTracker kill = killWithContribution(1, 99.0);
		kill.setMvpInOwnName(true);

		assertEquals(100.0, DropRateCalculator.getEffectiveKillContribution(kill), 0.001);
	}

	@Test
	public void getDueMultiplierIncludesMvpBonus()
	{
		ArrayList<NexLootTracker> kills = new ArrayList<>();
		NexLootTracker kill = killWithContribution(1, 20.0);
		kill.setMvpInOwnName(true);
		kills.add(kill);

		Double due = DropRateCalculator.getDueMultiplier(
			kills,
			killEntry -> false,
			DropRateCalculator.ANY_UNIQUE_DENOMINATOR
		);

		// 22% on 1/43 -> 0.22/43
		assertEquals(0.22 / 43.0, due, 0.0001);
	}

	@Test
	public void getDueMultiplierUsesContributionScaledDropRate()
	{
		ArrayList<NexLootTracker> kills = new ArrayList<>();
		for (int i = 0; i < 269; i++)
		{
			kills.add(killWithContribution(i, 20.0));
		}

		Double due = DropRateCalculator.getDueMultiplier(
			kills,
			kill -> false,
			DropRateCalculator.ANY_UNIQUE_DENOMINATOR
		);
		assertEquals(1.25, due, 0.01);
	}

	@Test
	public void getDueMultiplierCarriesOverProgressAfterOwnDrop()
	{
		ArrayList<NexLootTracker> kills = new ArrayList<>();
		for (int i = 0; i < 1548; i++)
		{
			kills.add(killWithContribution(i, 20.0));
		}

		kills.add(ownUniqueKill(1548, NexUniques.TORVA_BODY, true));

		Double due = DropRateCalculator.getDueMultiplier(
			kills,
			kill -> DropRateCalculator.isOwnUnique(kill, NexUniques.TORVA_BODY),
			NexUniques.TORVA_BODY.getDropRateDenominator()
		);
		assertEquals(0.20, due, 0.01);
	}

	@Test
	public void getDueMultiplierClampsEarlyDropToZero()
	{
		ArrayList<NexLootTracker> kills = new ArrayList<>();
		for (int i = 0; i < 172; i++)
		{
			kills.add(killWithContribution(i, 20.0));
		}

		NexLootTracker dropKill = ownUniqueKill(172, NexUniques.ZARYTE_VAMBRACES, true);
		dropKill.setKillContribution(20.0);
		kills.add(dropKill);

		Double due = DropRateCalculator.getDueMultiplier(
			kills,
			kill -> DropRateCalculator.isOwnUnique(kill, NexUniques.ZARYTE_VAMBRACES),
			NexUniques.ZARYTE_VAMBRACES.getDropRateDenominator()
		);
		assertEquals(0.0, due, 0.01);
	}

	@Test
	public void getDueMultiplierReturnsNullWithoutContributionData()
	{
		ArrayList<NexLootTracker> kills = new ArrayList<>();
		kills.add(killWithContribution(1, null));

		assertNull(DropRateCalculator.getDueMultiplier(
			kills,
			kill -> false,
			172
		));
	}

	@Test
	public void getKillsSinceLastOwnDropStartsAfterMostRecentOwnDrop()
	{
		ArrayList<NexLootTracker> kills = new ArrayList<>();
		kills.add(ownUniqueKill(1, NexUniques.ZARYTE_VAMBRACES, false));
		kills.add(killWithDate(2, "", ""));
		kills.add(ownUniqueKill(3, NexUniques.ZARYTE_VAMBRACES, true));
		kills.add(killWithDate(4, "", ""));

		ArrayList<NexLootTracker> sinceLastOwn = DropRateCalculator.getKillsSinceLastOwnDrop(
			kills,
			kill -> DropRateCalculator.isOwnUnique(kill, NexUniques.ZARYTE_VAMBRACES)
		);

		assertEquals(1, sinceLastOwn.size());
		assertEquals(4, sinceLastOwn.get(0).getDate());
	}

	@Test
	public void getPersonalDryCountUsesAnyOwnUnique()
	{
		ArrayList<NexLootTracker> kills = new ArrayList<>();
		kills.add(ownUniqueKill(1, NexUniques.NIHIL_HORN, true));
		kills.add(killWithDate(2, "Torva full helm (damaged)", "Teammate"));
		kills.add(killWithDate(3, "", ""));
		kills.add(killWithDate(4, "", ""));

		assertEquals(3, DropRateCalculator.getPersonalDryCount(kills));
		assertEquals(2, DropRateCalculator.getKillsSinceLastSeenUnique(kills));
		assertEquals(NexUniques.NIHIL_HORN.getName(), DropRateCalculator.getLastOwnUniqueName(kills));
	}

	@Test
	public void getDryStreakStatsIncludesLastPersonalItem()
	{
		ArrayList<NexLootTracker> kills = new ArrayList<>();
		kills.add(ownUniqueKill(1, NexUniques.ANCIENT_HILT, true));
		kills.add(killWithDate(2, "", ""));

		DryStreakStats stats = DropRateCalculator.getDryStreakStats(kills);
		assertEquals(1, stats.getPersonalDry());
		assertEquals(1, stats.getTeamDry());
		assertEquals(NexUniques.ANCIENT_HILT.getName(), stats.getLastPersonalItem());
	}

	private static NexLootTracker killWithDate(long date, String specialLoot, String petReceiver)
	{
		NexLootTracker kill = new NexLootTracker();
		kill.setDate(date);
		kill.setKillCountID("kill-" + date);
		kill.setSpecialLoot(specialLoot);
		kill.setPetReceiver(petReceiver);
		return kill;
	}

	private static NexLootTracker killWithContribution(long date, Double contribution)
	{
		NexLootTracker kill = killWithDate(date, "", "");
		kill.setKillContribution(contribution);
		return kill;
	}

	private static NexLootTracker killWithDuration(long date, Long durationMs)
	{
		NexLootTracker kill = killWithDate(date, "", "");
		kill.setKillDurationMs(durationMs);
		return kill;
	}

	@Test
	public void getAverageKillDurationMsIgnoresMissingDurations()
	{
		ArrayList<NexLootTracker> kills = new ArrayList<>();
		kills.add(killWithDuration(1, 248_000L)); // 4:08
		kills.add(killWithDuration(2, null));
		kills.add(killWithDuration(3, 91_000L)); // 1:31

		assertEquals(Long.valueOf(169_500L), DropRateCalculator.getAverageKillDurationMs(kills));
	}

	@Test
	public void getAverageKillDurationMsReturnsNullWhenNoData()
	{
		ArrayList<NexLootTracker> kills = new ArrayList<>();
		kills.add(killWithDuration(1, null));

		assertNull(DropRateCalculator.getAverageKillDurationMs(kills));
	}

	@Test
	public void formatKillDurationMatchesChatStyle()
	{
		assertEquals("4:08", DropRateCalculator.formatKillDuration(248_000L));
		assertEquals("1:31", DropRateCalculator.formatKillDuration(91_000L));
		assertEquals("1:04:08", DropRateCalculator.formatKillDuration(3_848_000L));
	}

	private static NexLootTracker ownUniqueKill(long date, NexUniques unique, boolean inOwnName)
	{
		NexLootTracker kill = killWithDate(date, unique.getName(), "");
		kill.setSpecialLootInOwnName(inOwnName);
		return kill;
	}
}
