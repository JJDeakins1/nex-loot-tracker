package com.nexloottracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class FileReadWriterParseTest
{
	@Test
	public void parsesDummyLogLine() throws Exception
	{
		String line = Files.readString(Paths.get(
			System.getProperty("user.home"),
			".runelite/nex-loot-tracker/deakinsjj07@gmail.com/nex_loot_data.log"
		)).trim();

		if (line.isEmpty())
		{
			return;
		}

		Gson gson = new GsonBuilder().create();
		NexLootTracker kill = gson.fromJson(new JsonParser().parse(line), NexLootTracker.class);

		assertNotNull(kill);
		assertFalse(kill.getLootList().isEmpty());
	}
}
