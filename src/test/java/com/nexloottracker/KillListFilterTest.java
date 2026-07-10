package com.nexloottracker;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KillListFilterTest
{
	private static final ZoneId ZONE = ZoneId.of("Australia/Sydney");

	@Test
	public void filterByDateTodayIncludesKillsFromStartOfLocalDay()
	{
		final ZonedDateTime now = ZonedDateTime.of(2026, 7, 10, 11, 0, 0, 0, ZONE);
		final long todayStart = LocalDate.of(2026, 7, 10).atStartOfDay(ZONE).toInstant().toEpochMilli();
		final long yesterday = todayStart - 1;

		List<NexLootTracker> kills = new ArrayList<>();
		kills.add(killWithDate(yesterday));
		kills.add(killWithDate(todayStart));
		kills.add(killWithDate(todayStart + 60_000));

		ArrayList<NexLootTracker> filtered = KillListFilter.filterByDate(
			kills,
			"Today",
			now.toInstant().toEpochMilli(),
			ZONE
		);

		assertEquals(2, filtered.size());
	}

	@Test
	public void filterByDateTwelveHoursUsesRollingWindow()
	{
		final long now = 1_000_000_000_000L;
		final long since = KillListFilter.getSinceTimestamp("12 Hours", now, ZONE);

		List<NexLootTracker> kills = new ArrayList<>();
		kills.add(killWithDate(since - 1));
		kills.add(killWithDate(since));
		kills.add(killWithDate(now));

		ArrayList<NexLootTracker> filtered = KillListFilter.filterByDate(kills, "12 Hours", now, ZONE);

		assertEquals(2, filtered.size());
	}

	@Test
	public void filterByDateAllTimeReturnsEveryKill()
	{
		List<NexLootTracker> kills = new ArrayList<>();
		kills.add(killWithDate(1));
		kills.add(killWithDate(2));

		ArrayList<NexLootTracker> filtered = KillListFilter.filterByDate(kills, "All Time", 3, ZONE);

		assertEquals(2, filtered.size());
	}

	@Test
	public void getSinceTimestampTodayIsBeforeRollingTwentyFourHoursWhenMorning()
	{
		final ZonedDateTime morning = ZonedDateTime.of(2026, 7, 10, 11, 0, 0, 0, ZONE);
		final long now = morning.toInstant().toEpochMilli();
		final long todaySince = KillListFilter.getSinceTimestamp("Today", now, ZONE);
		final long rollingSince = now - 86_400_000L;

		assertTrue(todaySince > rollingSince);
	}

	private static NexLootTracker killWithDate(long date)
	{
		NexLootTracker kill = new NexLootTracker();
		kill.setDate(date);
		kill.setKillCountID("kill-" + date);
		return kill;
	}
}
