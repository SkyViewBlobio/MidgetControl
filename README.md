# MidgetControl

MidgetControl is a lightweight, server-only Fabric mod for Minecraft Java 26.2. Vanilla clients do not need to install it.

## Features

- Configurable percentages for the regular biome-natural mob caps.
- Spawners, trial spawners, breeding, spawn eggs, commands, summoned mobs, raids, patrols, structures, golems, withers, dragons, and persistent mobs are never blocked or removed by MidgetControl.
- `/info <player>` shows first join, accumulated online time, player kills, and deaths in a private chat message.
- A `MidgetCraft` TPS/MSPT header plus player count and each viewer's own ping in the vanilla player list.
- Player locator-bar blips are private by default, with a persistent opt-in for each player.
- `/midgethelp` opens a native command-help window on vanilla clients.
- `/midgetcontrol reload` reloads the config without restarting the server.

## Requirements

- Minecraft Java 26.2
- Fabric Loader 0.19.3 or newer
- Java 25

The release jar embeds only the five small Fabric API modules it uses, so a separate Fabric API install is not required. It also works when the full Fabric API is already installed.

## Install

1. Stop the server.
2. Put `MidgetControl-1.1.0.jar` in the server's `mods` folder.
3. Start the server once.
4. Edit `config/midgetcontrol.properties` if wanted.
5. Run `/midgetcontrol reload` as the server owner, or restart the server.

Player history and locator-blip choices are stored separately per world in `world/data/midgetcontrol-players.json`.

## Natural spawn limits

The default for every category is 50% of the normal Vanilla cap. `100` restores Vanilla, and `0` stops regular runtime biome spawning for that category.

The hook is inside the regular `NaturalSpawner` cycle. It does not cancel other entity-add paths and never scans for or deletes existing entities. As in Vanilla, a non-persistent mob that came from another source can occupy the mob cap, but that source itself is still unrestricted. One-time creature population during new-chunk generation is separate from the runtime biome cycle and is left unchanged.

## Commands

- `/midgethelp` — opens the MidgetCraft command-help window.
- `/blipon` — lets other players see your locator-bar blip.
- `/blipoff` — hides your locator-bar blip from other players.
- `/info <player>` — available to everyone by default.
- `/midgetcontrol info <player>` — namespaced-style alias.
- `/midgetcontrol reload` — owner permission by default.

If another mod already owns `/info`, set `player-info.register-short-command=false` and restart. The `/midgetcontrol info` command remains available.

Join dates start when a player first connects after MidgetControl is installed. Online time is accumulated using real elapsed session time and saved every 60 seconds, on normal world saves, on disconnect, and during shutdown.

## Locator-bar privacy

New players start with their blip hidden. `/blipon` and `/blipoff` save the choice immediately, so it survives disconnects, server restarts, and player respawns. Enabling a blip restores normal Vanilla behavior: crouching, disguising headgear, dimensions, distance, and the global locator-bar gamerule can still hide it temporarily.

## Tab-list compatibility

Minecraft exposes one shared tab-list header and footer. If another mod also owns them, the most recent update wins. Disable `tab-tps.enabled` in one of the mods if they compete; all other MidgetControl features continue to work. Existing configs using the old default `tab-tps.title=MidgetControl` are displayed as `MidgetCraft`, while custom titles are preserved.
