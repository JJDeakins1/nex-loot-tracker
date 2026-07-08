package com.nexloottracker.filereadwriter;

import com.nexloottracker.NexLootTracker;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileReadWriterSyncTest
{
	private final FileReadWriter fileReadWriter = new FileReadWriter();

	@Test
	public void preservesOwnDropWhenMarkedFfaWithEmptyLootList()
	{
		NexLootTracker kill = new NexLootTracker();
		kill.setSpecialLoot("Torva full helm");
		kill.setSpecialLootInOwnName(true);
		kill.setSpecialLootValue(100_000_000);
		kill.setFreeForAll(true);
		kill.setLootSplitPaid(-1);
		kill.setLootSplitReceived(100_000_000);

		fileReadWriter.syncSpecialLootInOwnName(kill);

		assertTrue(kill.isSpecialLootInOwnName());
	}

	@Test
	public void doesNotMarkTeammateFfaDropAsOwn()
	{
		NexLootTracker kill = new NexLootTracker();
		kill.setSpecialLoot("Torva full helm");
		kill.setSpecialLootInOwnName(false);
		kill.setSpecialLootValue(100_000_000);
		kill.setFreeForAll(true);
		kill.setLootSplitPaid(-1);
		kill.setLootSplitReceived(-1);

		fileReadWriter.syncSpecialLootInOwnName(kill);

		assertFalse(kill.isSpecialLootInOwnName());
	}

	@Test
	public void infersOwnDropFromSplitPaid()
	{
		NexLootTracker kill = new NexLootTracker();
		kill.setSpecialLoot("Torva full helm");
		kill.setSpecialLootInOwnName(false);
		kill.setSpecialLootValue(100_000_000);
		kill.setLootSplitPaid(75_000_000);

		fileReadWriter.syncSpecialLootInOwnName(kill);

		assertTrue(kill.isSpecialLootInOwnName());
	}
}
