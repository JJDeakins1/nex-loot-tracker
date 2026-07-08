package com.nexloottracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FileReadWriterParseTest
{
	@Test
	public void parsesDummyLogLine() throws Exception
	{
		String line = Files.readString(Paths.get(
			System.getProperty("user.home"),
			".runelite/nex-loot-tracker/deakinsjj07@gmail.com/nex_loot_data.log"
		)).trim();

		Gson gson = new GsonBuilder().create();
		NexLootTracker kill = gson.fromJson(new JsonParser().parse(line), NexLootTracker.class);

		assertEquals(140, kill.getCompletionCount());
		assertFalse(kill.getLootList().isEmpty());
		assertEquals(4, kill.getLootList().size());
	}
}
