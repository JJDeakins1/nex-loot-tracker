package com.nexloottracker;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class KillListFilter
{
	private KillListFilter()
	{
	}

	public static long getSinceTimestamp(String dateFilter, long now, ZoneId zone)
	{
		switch (dateFilter)
		{
			case "12 Hours":
				return now - 43_200_000L;
			case "Today":
				return LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli();
			case "3 Days":
				return now - 259_200_000L;
			case "Week":
				return now - 604_800_000L;
			case "Month":
				return now - 2_629_746_000L;
			case "3 Months":
				return now - 7_889_400_000L;
			case "Year":
				return now - 31_536_000_000L;
			default:
				return Long.MIN_VALUE;
		}
	}

	public static ArrayList<NexLootTracker> filterByDate(
		List<NexLootTracker> kills,
		String dateFilter,
		long now,
		ZoneId zone
	)
	{
		if ("All Time".equals(dateFilter))
		{
			return new ArrayList<>(kills);
		}

		final long since = getSinceTimestamp(dateFilter, now, zone);
		return kills.stream()
			.filter(kill -> kill.getDate() >= since)
			.collect(Collectors.toCollection(ArrayList::new));
	}
}
