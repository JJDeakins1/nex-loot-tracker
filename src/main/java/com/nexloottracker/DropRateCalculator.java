package com.nexloottracker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Nex unique drop rates are rolled per kill (1/43 for any unique on the table).
 * A player's personal rate scales linearly with kill contribution, e.g. 20% contribution
 * yields a 1/215 chance for any unique (43 / 0.20 = 215).
 */
public final class DropRateCalculator
{
	public static final double ANY_UNIQUE_DENOMINATOR = 43.0;
	public static final double MVP_CONTRIBUTION_BONUS = 2.0;

	private DropRateCalculator()
	{
	}

	/**
	 * Effective drop contribution includes the +2% MVP bonus when the local player was MVP.
	 */
	public static Double getEffectiveKillContribution(NexLootTracker kill)
	{
		Double contribution = kill.getKillContribution();
		if (contribution == null)
		{
			return null;
		}

		double effective = contribution;
		if (kill.isMvpInOwnName())
		{
			effective += MVP_CONTRIBUTION_BONUS;
		}

		return Math.max(0.0, Math.min(100.0, effective));
	}

	public static ArrayList<NexLootTracker> getDistinctKillsSortedByDate(List<NexLootTracker> kills)
	{
		LinkedHashMap<String, NexLootTracker> distinct = new LinkedHashMap<>();
		for (NexLootTracker kill : kills)
		{
			distinct.put(kill.getKillCountID(), kill);
		}

		ArrayList<NexLootTracker> sorted = new ArrayList<>(distinct.values());
		sorted.sort(Comparator.comparingLong(NexLootTracker::getDate));
		return sorted;
	}

	public static int getKillsSinceLastSeenUnique(List<NexLootTracker> kills)
	{
		ArrayList<NexLootTracker> distinct = getDistinctKillsSortedByDate(kills);
		distinct.sort(Comparator.comparingLong(NexLootTracker::getDate).reversed());

		int streak = 0;
		for (NexLootTracker kill : distinct)
		{
			if (hasSeenUnique(kill))
			{
				break;
			}
			streak++;
		}
		return streak;
	}

	public static ArrayList<NexLootTracker> getKillsSinceLastOwnDrop(
		List<NexLootTracker> kills,
		Predicate<NexLootTracker> isOwnDrop
	)
	{
		ArrayList<NexLootTracker> distinct = getDistinctKillsSortedByDate(kills);

		int lastOwnDropIndex = -1;
		for (int i = 0; i < distinct.size(); i++)
		{
			if (isOwnDrop.test(distinct.get(i)))
			{
				lastOwnDropIndex = i;
			}
		}

		if (lastOwnDropIndex < 0)
		{
			return distinct;
		}

		if (lastOwnDropIndex == distinct.size() - 1)
		{
			return new ArrayList<>();
		}

		return new ArrayList<>(distinct.subList(lastOwnDropIndex + 1, distinct.size()));
	}

	public static Double getAverageKillContribution(List<NexLootTracker> kills)
	{
		double sum = 0.0;
		int count = 0;

		for (NexLootTracker kill : kills)
		{
			Double contribution = getEffectiveKillContribution(kill);
			if (contribution == null)
			{
				continue;
			}
			sum += contribution;
			count++;
		}

		if (count == 0)
		{
			return null;
		}

		return sum / count;
	}

	/**
	 * Returns how "due" a drop is relative to expected rate, walking kills in chronological order.
	 * Each kill with contribution adds {@code contribution% / dropRateDenominator} toward 1.0x.
	 * When the player receives a matching drop, 1.0 is subtracted so overdue progress carries over
	 * (e.g. 1.20x at drop becomes 0.20x). Early drops clamp at 0.00x.
	 */
	public static Double getDueMultiplier(
		List<NexLootTracker> kills,
		Predicate<NexLootTracker> isOwnDrop,
		double dropRateDenominator
	)
	{
		ArrayList<NexLootTracker> distinct = getDistinctKillsSortedByDate(kills);
		double due = 0.0;
		boolean hasContributionData = false;

		for (NexLootTracker kill : distinct)
		{
			Double contribution = getEffectiveKillContribution(kill);
			if (contribution != null && contribution > 0.0)
			{
				due += (contribution / 100.0) / dropRateDenominator;
				hasContributionData = true;
			}

			if (isOwnDrop.test(kill))
			{
				due = Math.max(0.0, due - 1.0);
			}
		}

		if (!hasContributionData)
		{
			return null;
		}

		return due;
	}

	public static String formatDueMultiplier(Double multiplier)
	{
		if (multiplier == null)
		{
			return "-";
		}

		return String.format(Locale.US, "%.2f", multiplier);
	}

	public static boolean hasSeenUnique(NexLootTracker kill)
	{
		return !kill.getSpecialLoot().isEmpty() || !kill.getPetReceiver().isEmpty();
	}

	public static boolean isAnyOwnUnique(NexLootTracker kill)
	{
		if (kill.isPetInMyName())
		{
			return true;
		}

		if (!kill.isSpecialLootInOwnName())
		{
			return false;
		}

		return NexUniques.fromName(kill.getSpecialLoot()) != null;
	}

	public static boolean isOwnUnique(NexLootTracker kill, NexUniques unique)
	{
		if (unique == NexUniques.NEXLING)
		{
			return kill.isPetInMyName();
		}

		return kill.isSpecialLootInOwnName()
			&& unique.getName().equalsIgnoreCase(kill.getSpecialLoot());
	}
}
