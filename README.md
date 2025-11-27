# QEventBox

**QEventBox** is a Spigot/Paper Minecraft plugin that adds event crates with auto-spawn, point-based rewards, and a shop GUI. Perfect for mini-game or survival event servers.

---

## Features

- Spawn crates manually or automatically.
- Auto-spawn crates based on configurable times and amount.
- Automatic broadcast messages when crates appear, near expiration, or disappear.
- Crates have a lifetime (in seconds) and notify players at half-life.
- Rewards can be **Event Points**, **commands**, or **items**.
- Admin commands for spawn, auto-spawn, remove, status, and reload.
- Shop GUI for exchanging points.
- Tab-completion for commands.

---

## Installation

1. Make sure your server is running **Spigot / Paper 1.20+**.
2. Place `QEventBox.jar` in the `plugins/` folder.
3. Start the server to generate `config.yml`.
4. Configure crates, spawn times, rewards, and messages in `config.yml`.
5. Restart or reload the server.

---

## Config.yml

Minimal example:

```yaml
region:
  world: world
  min-x: -500
  max-x: 500
  min-z: -500
  max-z: 500
  min-y: 60

crate:
  amount: 3
  lifetime-seconds: 300
  texture: "base64-texture-here"
  spawn-times:
    - "20:00"
    - "21:30"

messages:
  auto: "§e§l[QEventBox] AutoSpawn start in %world%"

rewards:
  - "eventpoints:50"
  - "command:give %player% minecraft:diamond 1"
  - "item:DIAMOND:3"