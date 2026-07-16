package com.nexloottracker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

@AllArgsConstructor
@Getter
public enum NexUniques
{
	ANCIENT_HILT("Ancient hilt", ItemID.GODWARS_GODSWORD_HILT_ANCIENT, 516),
	NIHIL_HORN("Nihil horn", ItemID.NIHIL_HORN, 258),
	TORVA_HELM("Torva full helm (damaged)", ItemID.BROKEN_TORVA_HELM, 258),
	TORVA_BODY("Torva platebody (damaged)", ItemID.BROKEN_TORVA_CHEST, 258),
	TORVA_LEGS("Torva platelegs (damaged)", ItemID.BROKEN_TORVA_LEGS, 258),
	ZARYTE_VAMBRACES("Zaryte vambraces", ItemID.ZARYTE_VAMBRACES, 172),
	NEXLING("Nexling", ItemID.NEXPET, 500);

	private final String name;
	private final int itemId;
	private final int dropRateDenominator;

	public static String normalizeDropItemName(String itemName)
	{
		if (itemName == null || itemName.isEmpty())
		{
			return "";
		}

		String normalized = itemName.trim()
			.replaceAll("\\s*\\(Nex\\)\\s*$", "")
			.replaceAll("\\s*from Nex\\.?\\s*$", "")
			.replaceAll("\\s*\\([\\d,]+ coins\\)\\s*$", "");

		if (normalized.matches("^\\d+\\s*x\\s*.+"))
		{
			normalized = normalized.replaceFirst("^\\d+\\s*x\\s*", "");
		}

		return normalized.trim();
	}

	public static NexUniques fromName(String itemName)
	{
		final String normalized = normalizeDropItemName(itemName);
		if (normalized.isEmpty())
		{
			return null;
		}

		final String lower = normalized.toLowerCase();

		for (NexUniques unique : values())
		{
			if (unique.getName().equalsIgnoreCase(normalized)
				|| unique.getName().toLowerCase().equals(lower))
			{
				return unique;
			}
		}

		if (lower.contains("torva full helm"))
		{
			return TORVA_HELM;
		}
		if (lower.contains("torva platebody"))
		{
			return TORVA_BODY;
		}
		if (lower.contains("torva platelegs"))
		{
			return TORVA_LEGS;
		}
		if (lower.contains("nexling"))
		{
			return NEXLING;
		}

		return null;
	}

	public static boolean isUniqueItemId(int itemId)
	{
		return fromItemId(itemId) != null;
	}

	public static NexUniques fromItemId(int itemId)
	{
		for (NexUniques unique : values())
		{
			if (unique.getItemId() == itemId)
			{
				return unique;
			}
		}
		return null;
	}
}
