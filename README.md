# Nex Loot Tracker

A RuneLite plugin that tracks Nex kills, personal loot, team unique drops, and split/FFA GP. Modeled after the [Raid Data Tracker](https://github.com/Raitab/raid-data-tracker) plugin architecture.

## Features

- Logs every Nex kill to a local JSON-lines file
- Tracks personal loot from `NpcLootReceived` events
- Parses team unique broadcasts (`received a drop`) and MVP messages
- Side panel with kills logged, filters, uniques table, dry streak, split GP earned, regular drops, and split changer
- Configurable default FFA, FFA cutoff, and Last X kills filter

## Data storage

Data is stored at:

```
~/.runelite/nex-loot-tracker/<username>/nex_loot_data.log
```

Each line is one JSON object representing a kill or unique drop entry.

## Development

Requirements: Java 11+, Gradle wrapper included.

```bash
./gradlew build
./gradlew run
```

The `run` task launches RuneLite in developer mode with the plugin loaded.

## Plugin Hub submission

1. Push this repository to a public GitHub repo
2. Add a BSD 2-Clause `LICENSE` (included)
3. Fork [plugin-hub](https://github.com/runelite/plugin-hub)
4. Create `plugins/nex-loot-tracker` with:

```
repository=https://github.com/<user>/nex-loot-tracker.git
commit=<40-char-commit-hash>
```

5. Open a pull request and wait for CI/review

See the [plugin-hub README](https://github.com/runelite/plugin-hub/blob/master/README.md) for full details.

## Tracked uniques

- Ancient hilt
- Nihil horn
- Torva full helm (damaged)
- Torva platebody (damaged)
- Torva platelegs (damaged)
- Zaryte vambraces
- Nexling (pet)

## License

BSD 2-Clause License. See [LICENSE](LICENSE).
