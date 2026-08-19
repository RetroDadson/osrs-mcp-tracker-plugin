# OSRS MCP Tracker Plugin

Website: [osrsmcp.retrodadson.com](https://osrsmcp.retrodadson.com)
Server/backend source: [github.com/RetroDadson/osrs-mcp-server](https://github.com/RetroDadson/osrs-mcp-server)

This is a RuneLite plugin that tracks information about your OSRS character(s) and sends it to a companion server, which exposes that data through an [MCP](https://modelcontextprotocol.io/) server so an AI assistant (e.g. Claude) can query it directly. It works whether you're tracking yourself solo or sharing tracking with others, and for every OSRS account type (regular, Ironman, Hardcore Ironman, Ultimate Ironman, Group Ironman).

Originally forked from [group-ironmen-tracker](https://github.com/christoabrown/group-ironmen-tracker) — thanks to the original author for the excellent groundwork this project builds on.

Currently tracks:

* Inventory, equipment, bank, rune pouch, seed vault, and shared bank
* Skill XP
* World position
* HP, prayer, energy, and world
* Quest completion status
* Health and position of NPCs the player is interacting with
* Achievement Diaries
* Collection Log

More data categories (Slayer tasks, kill counts, clue scrolls, deaths, farming patches) are on the way — see the [server repo](https://github.com/RetroDadson/osrs-mcp-server) for the current roadmap.

## Setup

Sign up on the website with your OSRS name (or add others to share tracking), then paste the login name and token it gives you into the plugin's config panel. Each character you want to track needs the plugin installed with that same login token.
