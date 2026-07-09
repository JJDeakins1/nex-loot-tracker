package com.nexloottracker.filereadwriter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.nexloottracker.NexLootTracker;
import com.nexloottracker.NexLootTrackerItem;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

@Slf4j
public class FileReadWriter
{
	private static final String DATA_FILE_NAME = "nex_loot_data.log";

	@Getter
	private String username;
	private String dataDir;

	@Inject
	@Setter
	private Gson gson;

	public void writeToFile(NexLootTracker kill)
	{
		if (dataDir == null || username == null || username.isEmpty())
		{
			log.error("Cannot write Nex kill: profile not initialized");
			return;
		}

		final String fileName = getDataFileName();

		try
		{
			log.info("Writing Nex kill to {}", fileName);
			JsonParser parser = new JsonParser();
			FileWriter fw = new FileWriter(fileName, true);
			gson.toJson(parser.parse(getJsonString(kill, gson, parser)), fw);
			fw.append("\n");
			fw.close();
		}
		catch (IOException ioe)
		{
			log.error("IOException in writeToFile: {}", ioe.getMessage());
		}
	}

	public String getJsonString(NexLootTracker kill, Gson gson, JsonParser parser)
	{
		JsonObject json = parser.parse(gson.toJson(kill)).getAsJsonObject();
		JsonArray lootListJson = new JsonArray();

		for (NexLootTrackerItem item : kill.getLootList())
		{
			lootListJson.add(parser.parse(gson.toJson(item, new TypeToken<NexLootTrackerItem>() {}.getType())));
		}

		json.addProperty("lootList", lootListJson.toString());
		return json.toString().replace("\\\"", "\"").replace("\"[", "[").replace("]\"", "]");
	}

	public ArrayList<NexLootTracker> readFromFile()
	{
		return readFromFile("");
	}

	public ArrayList<NexLootTracker> readFromFile(String alternateFile)
	{
		if (alternateFile.isEmpty() && (dataDir == null || username == null || username.isEmpty()))
		{
			log.warn("Cannot read Nex loot log before profile is set");
			return new ArrayList<>();
		}

		String fileName = alternateFile.isEmpty() ? getDataFileName() : alternateFile;
		boolean foundReplacementUnicode = false;

		try
		{
			log.info("Reading Nex loot log from {}", fileName);
			JsonParser parser = new JsonParser();
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String line;
			ArrayList<NexLootTracker> killList = new ArrayList<>();

			while ((line = reader.readLine()) != null && !line.isEmpty())
			{
				if (line.contains("\uFFFD"))
				{
					foundReplacementUnicode = true;
					line = line.replace("\uFFFD", " ");
				}

				try
				{
					killList.add(gson.fromJson(parser.parse(line), NexLootTracker.class));
				}
				catch (JsonSyntaxException e)
				{
					log.warn("Bad line in Nex loot log: {}", line);
				}
			}

			reader.close();

			boolean needsRewrite = foundReplacementUnicode;
			for (NexLootTracker kill : killList)
			{
				boolean before = kill.isSpecialLootInOwnName();
				syncSpecialLootInOwnName(kill);
				if (before != kill.isSpecialLootInOwnName())
				{
					needsRewrite = true;
				}
			}

			if (needsRewrite)
			{
				log.info("Rewriting Nex loot log after syncing special loot ownership");
				updateKillList(killList);
			}

			log.info("Loaded {} Nex kills from {}", killList.size(), fileName);
			return killList;
		}
		catch (IOException e)
		{
			log.error("Error reading Nex loot log", e);
			return new ArrayList<>();
		}
	}

	public void createFolders()
	{
		File dir = new File(RUNELITE_DIR, "nex-loot-tracker");
		ignoreResult(dir.mkdir());
		dir = new File(dir, username);
		ignoreResult(dir.mkdir());
		dataDir = dir.getAbsolutePath();

		try
		{
			ignoreResult(new File(getDataFileName()).createNewFile());
		}
		catch (IOException e)
		{
			log.error("Error creating Nex loot data file", e);
		}
	}

	public void updateUsername(final String username)
	{
		this.username = username;
		createFolders();
	}

	public boolean ensureProfile(String preferredUsername, long accountHash)
	{
		if (preferredUsername != null && !preferredUsername.isEmpty())
		{
			updateUsername(preferredUsername);
			return true;
		}

		final String discovered = discoverProfile(accountHash);
		if (discovered != null)
		{
			log.info("Using discovered Nex loot profile: {}", discovered);
			updateUsername(discovered);
			return true;
		}

		if (accountHash != 0)
		{
			final String hashProfile = "account-" + accountHash;
			log.info("Creating Nex loot profile for account hash: {}", accountHash);
			updateUsername(hashProfile);
			return true;
		}

		log.warn("No Nex loot profile available yet");
		return false;
	}

	private String discoverProfile(long accountHash)
	{
		final File baseDir = new File(RUNELITE_DIR, "nex-loot-tracker");
		final File[] profileDirs = baseDir.listFiles(File::isDirectory);
		if (profileDirs == null || profileDirs.length == 0)
		{
			return null;
		}

		if (accountHash != 0)
		{
			for (File profileDir : profileDirs)
			{
				if (logContainsAccountHash(profileDir, accountHash))
				{
					return profileDir.getName();
				}
			}
		}

		String profileWithData = null;
		int profilesWithData = 0;

		for (File profileDir : profileDirs)
		{
			final File logFile = new File(profileDir, DATA_FILE_NAME);
			if (logFile.isFile() && logFile.length() > 0)
			{
				profilesWithData++;
				profileWithData = profileDir.getName();
			}
		}

		return profilesWithData == 1 ? profileWithData : discoverSoleProfileDirectory();
	}

	private String discoverSoleProfileDirectory()
	{
		final File baseDir = new File(RUNELITE_DIR, "nex-loot-tracker");
		final File[] profileDirs = baseDir.listFiles(File::isDirectory);
		if (profileDirs == null)
		{
			return null;
		}

		String soleProfile = null;
		for (File profileDir : profileDirs)
		{
			soleProfile = profileDir.getName();
		}

		return profileDirs.length == 1 ? soleProfile : null;
	}

	private boolean logContainsAccountHash(File profileDir, long accountHash)
	{
		final File logFile = new File(profileDir, DATA_FILE_NAME);
		if (!logFile.isFile())
		{
			return false;
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(logFile)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isEmpty())
				{
					continue;
				}

				try
				{
					final NexLootTracker kill = gson.fromJson(new JsonParser().parse(line), NexLootTracker.class);
					if (kill.getAccountHash() == accountHash)
					{
						return true;
					}
				}
				catch (JsonSyntaxException ignored)
				{
				}
			}
		}
		catch (IOException e)
		{
			log.debug("Unable to scan Nex loot log in {}", profileDir.getName(), e);
		}

		return false;
	}

	public void updateKillList(ArrayList<NexLootTracker> killList)
	{
		try
		{
			JsonParser parser = new JsonParser();
			String fileName = getDataFileName();
			FileWriter fw = new FileWriter(fileName, false);

			for (NexLootTracker kill : killList)
			{
				syncSpecialLootInOwnName(kill);

				gson.toJson(parser.parse(getJsonString(kill, gson, parser)), fw);
				fw.append("\n");
			}

			fw.close();
		}
		catch (IOException e)
		{
			log.error("Error updating Nex loot log list", e);
		}
	}

	/**
	 * Infer whether a purple was received by the local player when persisting kills.
	 * Only ever upgrades {@code specialLootInOwnName} to {@code true}; never clears an
	 * existing own drop (e.g. after marking a received split as FFA).
	 */
	void syncSpecialLootInOwnName(NexLootTracker kill)
	{
		if (kill.getSpecialLoot().isEmpty())
		{
			return;
		}

		if (kill.getLootSplitPaid() > 0)
		{
			kill.setSpecialLootInOwnName(true);
			return;
		}

		if (kill.isFreeForAll()
			&& kill.getLootSplitReceived() > 0
			&& kill.getLootSplitReceived() == kill.getSpecialLootValue())
		{
			kill.setSpecialLootInOwnName(true);
			return;
		}

		if (!kill.getLootList().isEmpty()
			&& kill.getLootList().stream().anyMatch(item -> item.getName().equalsIgnoreCase(kill.getSpecialLoot())))
		{
			kill.setSpecialLootInOwnName(true);
		}
	}

	public void updateKill(NexLootTracker updatedKill)
	{
		try
		{
			JsonParser parser = new JsonParser();
			String fileName = getDataFileName();
			ArrayList<NexLootTracker> killList = readFromFile();
			FileWriter fw = new FileWriter(fileName, false);

			for (NexLootTracker kill : killList)
			{
				if (kill.getUniqueID().equals(updatedKill.getUniqueID()))
				{
					kill = updatedKill;
				}

				gson.toJson(parser.parse(getJsonString(kill, gson, parser)), fw);
				fw.append("\n");
			}

			fw.close();
		}
		catch (IOException e)
		{
			log.error("Error updating Nex kill entry", e);
		}
	}

	public boolean delete()
	{
		File file = new File(getDataFileName());
		boolean deleted = file.delete();

		try
		{
			ignoreResult(file.createNewFile());
		}
		catch (IOException e)
		{
			log.error("Error recreating Nex loot data file", e);
		}

		return deleted;
	}

	public String getDataFileName()
	{
		return dataDir + File.separator + DATA_FILE_NAME;
	}

	private void ignoreResult(boolean ignored)
	{
	}
}
