# Nex Loot Tracker

A RuneLite plugin that tracks Nex kills, personal loot, team unique drops, and split/FFA GP. Modeled after the [Raid Data Tracker](https://github.com/Raitab/raid-data-tracker) plugin architecture.

## Screenshots

<p align="center">
  <img src="docs/side-panel.png" alt="Nex Loot Tracker side panel" width="300" />
</p>

<p align="center">
  <img src="docs/splits-and-drops.png" alt="Regular drops and split changer" width="300" />
</p>

## Features

- Logs every Nex kill to a local JSON-lines file
- Tracks personal loot from `NpcLootReceived` events
- Parses team unique broadcasts (`received a drop`) and MVP messages
- Side panel with kills logged, kills since last drop, average kill contribution, filters, uniques table, due rate, split GP earned, regular drops, and split changer
- Configurable default FFA and FFA cutoff
- Kill contribution per kill (optional) — requires the built-in **DPS Counter** plugin to be enabled

## Due rates

The **Due** column answers a simple question: *based on how much you have contributed at Nex, are you ahead or behind on uniques?*

Think of it like a progress bar toward your next personal drop:

| Due value | What it means |
|-----------|---------------|
| **0.50** | You are about halfway to where you would expect a drop |
| **1.00** | You are right on rate — you have done roughly enough for one drop |
| **1.36** | You are about 36% overdue (unlucky) |
| **0.00** | You either just received a drop, or got one early |

### How it is calculated

1. **Each kill adds progress** based on your damage share that kill (from **DPS Counter**).
2. **MVP kills add +2%** on top of your damage share, matching how Nex awards MVP.
3. **Your personal rate depends on team size and contribution.** Nex rolls uniques once per kill for the team (roughly **1/43** chance someone gets a unique in a full group). Your share of that roll is based on your contribution that kill — so fewer teammates means a larger share and faster Due progress. Equal damage examples (no MVP):
   - **5-man** (~20% each) → about **1/215** per kill for you
   - **4-man** (~25% each) → about **1/172**
   - **3-man** (~33% each) → about **1/129**
   - **Duo** (~50% each) → about **1/86**
4. **Each item has its own team per-kill rate** from the OSRS Wiki (e.g. Zaryte vambraces 1/172, Ancient hilt 1/516). Your Due for that item scales the same way — by your contribution each kill.
5. **When you receive a personal drop**, 1.00 is subtracted from that item's Due. If you were at **1.20** when it dropped, you carry **0.20** into the next cycle instead of resetting to zero.
6. **The Total row** tracks any unique, using the overall team roll rate (**1/43**) scaled by your contribution each kill.

If DPS Counter is disabled or missing data for your kills, Due shows **`-`**.

### Kills Since Last Drop

This is separate from Due. It counts how many kills have passed since **anyone** in your team last received a unique (including uniques you did not get).

### Kill contribution

Kill contribution is your damage percentage for that fight, read from **DPS Counter**. The average shown in the panel includes the **+2% MVP bonus** when you were MVP. Only kills with contribution data are included in Due and the average (respecting your active filters).

## Data storage

Data is stored at:

```
~/.runelite/nex-loot-tracker/<username>/nex_loot_data.log
```

Each line is one JSON object representing a kill or unique drop entry.

## Tracked uniques

- Ancient hilt
- Nihil horn
- Torva full helm (damaged)
- Torva platebody (damaged)
- Torva platelegs (damaged)
- Zaryte vambraces
- Nexling (pet)
