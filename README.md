# OSRS MCP Tracker Plugin

Website: [osrsmcp.retrodadson.com](https://osrsmcp.retrodadson.com)
Server/backend source: [github.com/RetroDadson/osrs-mcp-server](https://github.com/RetroDadson/osrs-mcp-server)

This is a RuneLite plugin that tracks information about your OSRS character and sends it to a companion server, which exposes that data through an [MCP](https://modelcontextprotocol.io/) server so an AI assistant (e.g. Claude) can query it directly. It works for every OSRS account type (regular, Ironman, Hardcore Ironman, Ultimate Ironman, Group Ironman).

Originally forked from [group-ironmen-tracker](https://github.com/christoabrown/group-ironmen-tracker) — thanks to the original author for the excellent groundwork this project builds on.

Currently tracks:

* Inventory, equipment, bank, rune pouch, quiver, seed vault, potion storage, and shared bank
* Skill XP
* World position
* HP, prayer, energy, and world
* Quest completion status
* Health and position of NPCs the player is interacting with
* Achievement Diaries
* Collection Log
* Combat Achievements
* Slayer task (current task and completed tasks)
* Clue scroll completions
* Deaths (location and likely killer)
* Farming patches
* Account type (regular, Ironman, Hardcore, Ultimate, Group)

The server also pulls boss/activity kill counts and clue scroll totals from the OSRS Hiscores, so the plugin doesn't need to track those itself.

## Setup

Sign up on the website with your OSRS name, then paste the token it gives you into the plugin's config panel.
