package com.nexloottracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(NexLootTrackerConfig.CONFIG_GROUP)
public interface NexLootTrackerConfig extends Config
{
	String CONFIG_GROUP = "nexloottracker";

	@ConfigItem(
		keyName = "defaultFFA",
		name = "Default FFA",
		description = "Sets the default split to free-for-all rather than split"
	)
	default boolean defaultFFA()
	{
		return false;
	}

	@ConfigItem(
		keyName = "FFACutoff",
		name = "FFA cut off",
		description = "When the split value is below this amount, the drop is considered free-for-all"
	)
	default int FFACutoff()
	{
		return 1_000_000;
	}

	@ConfigItem(
		keyName = "lastXKills",
		name = "Last X Kills",
		description = "When the 'Last X Kills' filter is selected, this value is used as X"
	)
	default int lastXKills()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "showTitle",
		name = "Show Title",
		description = "Disable to hide the title in the side panel"
	)
	default boolean showTitle()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showKillsLogged",
		name = "Show Kills Logged",
		description = "Disable to hide the Kills Logged section"
	)
	default boolean showKillsLogged()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFilters",
		name = "Show Filters",
		description = "Disable to hide the filter panel"
	)
	default boolean showFilters()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showUniquesTable",
		name = "Show Uniques Table",
		description = "Disable to hide the uniques table"
	)
	default boolean showUniquesTable()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSplitGPEarned",
		name = "Show Split GP Earned",
		description = "Disable to hide the Split GP Earned section"
	)
	default boolean showSplitGPEarned()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showRegularDrops",
		name = "Show Regular Drops",
		description = "Disable to hide the Regular Drops section"
	)
	default boolean showRegularDrops()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSplitChanger",
		name = "Show Split Changer",
		description = "Disable to hide the split changer section"
	)
	default boolean showSplitChanger()
	{
		return true;
	}
}
