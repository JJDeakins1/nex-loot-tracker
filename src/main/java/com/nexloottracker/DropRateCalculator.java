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
 * Nexling is tertiary (1/500) and is tracked separately — it does not reset unique dry streaks.
 */
public final class DropRateCalculator
{
	public static final double ANY_UNIQUE_DENOMINATOR = 43.0;
	public static final double MVP_CONTRIBUTION_MULTIPLIER = 1.10;

	private DropRateCalculator()
	{
	}

	/**
	 * Effective drop contribution applies a 10% MVP boost when the local player was MVP.
	 * e.g. 20% damage share becomes 22% (20 × 1.10).
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
			effective *= MVP_CONTRIBUTION_MULTIPLIER;
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

	/**
	 * Kills since the local player's last personal unique-table drop.
	 * Nexling is tertiary (1/500) and does not reset this streak.
	 * Computed from saved kill logs — same source of truth as the side panel.
	 */
	public static int getPersonalDryCount(List<NexLootTracker> kills)
	{
		return getKillsSinceLastOwnDrop(kills, DropRateCalculator::isAnyOwnUnique).size();
	}

	/**
	 * Most recent personal unique-table drop name from saved logs, or empty when none exist.
	 * Nexling is excluded (separate tertiary roll).
	 */
	public static String getLastOwnUniqueName(List<NexLootTracker> kills)
	{
		ArrayList<NexLootTracker> distinct = getDistinctKillsSortedByDate(kills);
		distinct.sort(Comparator.comparingLong(NexLootTracker::getDate).reversed());

		for (NexLootTracker kill : distinct)
		{
			if (isAnyOwnUnique(kill))
			{
				return kill.getSpecialLoot();
			}
		}

		return "";
	}

	public static DryStreakStats getDryStreakStats(List<NexLootTracker> kills)
	{
		return new DryStreakStats(
			getPersonalDryCount(kills),
			getKillsSinceLastSeenUnique(kills),
			getLastOwnUniqueName(kills)
		);
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
	 * Average fight duration in milliseconds across kills that have {@code killDurationMs} set.
	 * Uses the same distinct-kill list callers already apply for Average Kill Contribution.
	 */
	public static Long getAverageKillDurationMs(List<NexLootTracker> kills)
	{
		long sum = 0L;
		int count = 0;

		for (NexLootTracker kill : kills)
		{
			Long durationMs = kill.getKillDurationMs();
			if (durationMs == null || durationMs < 0)
			{
				continue;
			}
			sum += durationMs;
			count++;
		}

		if (count == 0)
		{
			return null;
		}

		return Math.round((double) sum / count);
	}

	/**
	 * Formats a duration for display like the in-game chat timer ({@code 4:08}, or {@code 1:04:08}).
	 */
	public static String formatKillDuration(long durationMs)
	{
		long totalSeconds = Math.max(0L, Math.round(durationMs / 1000.0));
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		if (hours > 0)
		{
			return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
		}

		return String.format(Locale.US, "%d:%02d", minutes, seconds);
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

	/**
	 * True when anyone on the team received a unique-table drop this kill.
	 * Nexling is tertiary and does not count.
	 */
	public static boolean hasSeenUnique(NexLootTracker kill)
	{
		return isUniqueTableDrop(kill.getSpecialLoot());
	}

	/**
	 * True when the local player received a unique-table drop this kill.
	 * Nexling is tertiary and does not count.
	 */
	public static boolean isAnyOwnUnique(NexLootTracker kill)
	{
		return kill.isSpecialLootInOwnName() && isUniqueTableDrop(kill.getSpecialLoot());
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

	/**
	 * Unique drop table items only (excludes Nexling tertiary pet).
	 */
	public static boolean isUniqueTableDrop(String itemName)
	{
		if (itemName == null || itemName.isEmpty())
		{
			return false;
		}

		final NexUniques unique = NexUniques.fromName(itemName);
		return unique != null && unique != NexUniques.NEXLING;
	}

	public static boolean isPetDrop(NexLootTracker kill)
	{
		return kill != null && !kill.getPetReceiver().isEmpty();
	}
}
