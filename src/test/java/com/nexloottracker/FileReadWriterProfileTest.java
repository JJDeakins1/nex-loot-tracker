package com.nexloottracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nexloottracker.filereadwriter.FileReadWriter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileReadWriterProfileTest
{
	@Test
	public void discoversSingleExistingProfileWhenUsernameMissing()
	{
		FileReadWriter fileReadWriter = new FileReadWriter();
		fileReadWriter.setGson(new GsonBuilder().create());

		assertTrue(fileReadWriter.ensureProfile(null, 0));
		assertEquals("deakinsjj07@gmail.com", fileReadWriter.getUsername());
	}
}
