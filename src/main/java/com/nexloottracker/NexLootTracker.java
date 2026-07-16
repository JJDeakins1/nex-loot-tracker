package com.nexloottracker;

import lombok.Data;

import java.util.ArrayList;
import java.util.UUID;

@Data
public class NexLootTracker
{
	long accountHash = -1;
	String profileType = "";
	boolean loggedIn = false;
	boolean killComplete = false;
	boolean freeForAll = false;

	int teamSize = -1;
	int completionCount = -1;

	String mvp = "";
	boolean mvpInOwnName = false;

	/**
	 * Kill contribution percent (0-100) derived from DPS Counter, if available.
	 * Null when DPS Counter is not enabled/installed or does not have data.
	 */
	Double killContribution = null;

	String specialLoot = "";
	String specialLootReceiver = "";
	boolean specialLootInOwnName = false;
	int specialLootValue = -1;

	String petReceiver = "";
	boolean petInMyName = false;

	int lootSplitReceived = -1;
	int lootSplitPaid = -1;

	ArrayList<NexLootTrackerItem> lootList = new ArrayList<>();

	String uniqueID = UUID.randomUUID().toString();
	String killCountID = UUID.randomUUID().toString();
	long date = System.currentTimeMillis();

	/**
	 * Fight duration in milliseconds, parsed from the chat "Fight duration: m:ss" message.
	 * Null when the duration message was not seen for this kill.
	 */
	Long killDurationMs = null;
}
