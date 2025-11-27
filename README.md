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
- Shop GUI for exchanging points to items or whatever.
- Tab-completion for commands.

---

## Installation

1. Make sure your server is running **Spigot / Paper 1.21.8+**.
2. Place `QEventBox.jar` in the `plugins/` folder.
3. Start the server to generate `config.yml`.
4. Configure crates, spawn times, rewards, and messages in `config.yml`.
5. Configure gui in `gui/gui.yml`

---

## Config.yml

Minimal example:

```yaml
region:
  world: world #set world here
  min-x: -500
  max-x: 500
  min-z: -500
  max-z: 500
  min-y: 60

crate:
  lifetime-seconds: 20 #box time till disappeared
  allow-multiple-worlds: false #allow multiple worlds
  texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjJjYTk3YzRjZjE5YjU0YjE3MDZhYzk1Zjc5ZmEyODQ5ZmZkZGIwZjYzMDllNzBmZDJkMjQ3MTgwMTYxYmI1NyJ9fX0="
  spawn-times:
    - "20:00" #activate auto box spawn when time is correct
    - "23:15"
  amount: 5 #amount of box will be spawned

rewards:
  - "eventpoints:20"
  - "item:DIAMOND:1"

messages:
  start: "§aBox has appeared in {x} {y} {z}!"
  half: "§6Box will disappear in {time} seconds!"
  expired: "§cBox vanished due to unclaimed!"
  claimed: "§b{player} §7has claimed the box!"
  auto: "§e§l[QEventBox] §aEvent Box has spawned in %world%"