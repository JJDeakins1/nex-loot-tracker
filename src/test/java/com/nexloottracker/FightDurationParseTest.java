package com.nexloottracker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FightDurationParseTest
{
	@Test
	public void parsesMinutesAndSeconds()
	{
		assertEquals(Long.valueOf(248_000L), NexLootTrackerPlugin.parseFightDurationMs("4:08"));
		assertEquals(Long.valueOf(91_000L), NexLootTrackerPlugin.parseFightDurationMs("1:31"));
	}

	@Test
	public void parsesHoursMinutesAndSeconds()
	{
		assertEquals(Long.valueOf(3_848_000L), NexLootTrackerPlugin.parseFightDurationMs("1:04:08"));
	}

	@Test
	public void rejectsInvalidDurations()
	{
		assertNull(NexLootTrackerPlugin.parseFightDurationMs(null));
		assertNull(NexLootTrackerPlugin.parseFightDurationMs(""));
		assertNull(NexLootTrackerPlugin.parseFightDurationMs("4"));
		assertNull(NexLootTrackerPlugin.parseFightDurationMs("abc:def"));
	}
}
