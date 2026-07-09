# Nex Loot Tracker

A RuneLite plugin that tracks Nex kills, personal loot, team unique drops, and split/FFA GP. Modeled after the [Raid Data Tracker](https://github.com/Raitab/raid-data-tracker) plugin architecture.

## Features

- Logs every Nex kill to a local JSON-lines file
- Tracks personal loot from `NpcLootReceived` events
- Parses team unique broadcasts (`received a drop`) and MVP messages
- Side panel with kills logged, average kill contribution, filters, uniques table, dry streak, split GP earned, regular drops, and split changer
- Configurable default FFA, FFA cutoff, and Last X kills filter
- Kill contribution per kill (optional) — requires the built-in **DPS Counter** plugin to be enabled

## Data storage

Data is stored at:

```
~/.runelite/nex-loot-tracker/<username>/nex_loot_data.log
```

Each line is one JSON object representing a kill or unique drop entry.

### Kill contribution

Kill contribution is logged as a percentage of total fight damage from RuneLite's **DPS Counter** plugin. **DPS Counter must be enabled** for this value to be recorded; if it is disabled or has no data for a kill, `killContribution` is stored as `null`.

The side panel's average kill contribution only includes kills with non-null contribution data (filtered by your selected team size and time range).

## Tracked uniques

- Ancient hilt
- Nihil horn
- Torva full helm (damaged)
- Torva platebody (damaged)
- Torva platelegs (damaged)
- Zaryte vambraces
- Nexling (pet)


