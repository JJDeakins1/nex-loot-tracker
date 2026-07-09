package com.nexloottracker.filereadwriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.nexloottracker.NexLootTracker;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class FileReadWriterJsonTest
{
	private final FileReadWriter fileReadWriter = new FileReadWriter();

	@Test
	public void getJsonStringIncludesNullKillContribution()
	{
		Gson gson = new GsonBuilder().create();
		JsonParser parser = new JsonParser();
		NexLootTracker kill = new NexLootTracker();

		String json = fileReadWriter.getJsonString(kill, gson, parser);

		assertTrue(json.contains("\"killContribution\":null"));
	}

	@Test
	public void gsonRoundTripStripsNullKillContribution()
	{
		Gson gson = new GsonBuilder().create();
		JsonParser parser = new JsonParser();
		NexLootTracker kill = new NexLootTracker();

		String jsonString = fileReadWriter.getJsonString(kill, gson, parser);
		String written = gson.toJson(parser.parse(jsonString));

		assertTrue(jsonString.contains("\"killContribution\":null"));
		assertTrue(!written.contains("killContribution"));
	}
}
