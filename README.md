# 💎 QEventBox - Event Crate & Standalone Point System

**QEventBox** is a robust Spigot/Paper Minecraft plugin designed to enhance server events by introducing time-based, random event crates, a dedicated point currency, and a fully customizable in-game shop GUI. Perfect for survival, mini-game, or specialized event servers looking for a dynamic player engagement system.

---

## 🌟 Key Features

We focus on automation and engagement to deliver a seamless event experience.

* ✅ **Full Automation:** Event crates automatically spawn and vanish based on configurable schedules and amounts.
* 💰 **Standalone Point System:** Introduces an isolated currency (**Event Points**) that is completely separate from existing server economies.
* 🛍️ **In-Game Shop GUI:** Players can exchange their earned **Event Points** for configurable rewards via a beautiful and easy-to-use GUI.
* 🗺️ **Region Spawning Control:** Limit crate spawns to specific world regions defined by `min-x/z` and `max-x/z` coordinates.
* 📢 **Dynamic Notifications:** Automatic broadcast messages when crates appear, reach half-life, or expire, keeping players informed.
* 🛠️ **Flexible Rewards:** Rewards are highly configurable and can include **Event Points**, physical **Items**, or execution of server **Commands**.

---

## 🖼️ Visual Preview (Screenshots & GIFs)

<img src="https://media4.giphy.com/media/v1.Y2lkPTc5MGI3NjExdG83NnlrYno2NDhuaXV0aGR2dXk0NDg1czg2MTkybWM0a3J4Z3cxOSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/ciimv577LnhqNovfnJ/giphy.gif" width="250"> <img src="https://media4.giphy.com/media/v1.Y2lkPTc5MGI3NjExYXd2ZGRjaTQzcWU5dXBzZnN1OGY3aDlzNDB6enMxdW4wbDhkbTZmdSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/4yoLRbMFsuxZi3K1jh/giphy.gif" width="250">

---

## 🔨 Installation

Get QEventBox up and running in a few simple steps.

1.  Ensure your server is running **Spigot / Paper 1.21.8+** (or compatible versions).
2.  Download the `QEventBox.jar` file.
3.  Place the JAR file into your server's `plugins/` folder.
4.  Start the server to generate the configuration files (`config.yml` and the `gui/` folder).
5.  Configure the crates, spawn times, rewards, and the shop GUI to your liking.
6.  Use `/qeventbox reload` or restart your server to apply changes.

---

## ⚙️ Configuration Example

The plugin offers extensive configuration options to fit your server's needs.

### `config.yml` - Main Settings

```yaml
region:
  world: world # The world name where crates will spawn.
  min-x: -500 # Minimum X boundary for random crate spawning.
  max-x: 500  # Maximum X boundary for random crate spawning.
  min-z: -500 # Minimum Z boundary for random crate spawning.
  max-z: 500  # Maximum Z boundary for random crate spawning.
  min-y: 60   # Minimum Y level for crates to spawn (to avoid caves/void).

crate:
  lifetime-seconds: 60 # Duration (in seconds) before an unclaimed crate disappears.
  # Base64 texture string for the custom player head crate.
  # you can get texture in https://minecraft-heads.com/
  texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmYwMDk5MWFlNzljYWMzNGUzNTgzNTdjMTNkYjY5Y2E2ZmVkODJiOGE1OWE5MjE4OWYzZmU0ZmRiMmU0ZTU4OSJ9fX0="
  spawn-times:
    - "20:00" # List of 24-hour clock times when auto-spawn is triggered.
    - "23:15"
  amount: 5 # Number of crates to spawn simultaneously during auto-spawn.

rewards:
  - "eventpoints:20" # Grants 20 of the custom defined points (QCoin).
  - "item:DIAMOND:1" # Grants 1 Diamond item.
  # Other formats supported: command:[command]

messages:
  start: "§aBox has appeared in {x} {y} {z}!" # Broadcast message when a crate is manually spawned.
  half: "§6Box will disappear in {time} seconds!" # Broadcast warning when a crate reaches half its lifetime.
  expired: "§cBox vanished due to unclaimed!" # Broadcast when a crate disappears naturally.
  claimed: "§b{player} §7has claimed the box!" # Broadcast when a crate is successfully claimed.
  auto: "§e[QEventBox] §aEvent Box has spawned in %world%" # Broadcast message when the auto-spawn is triggered.

points: "QCoin" # The custom name used for the plugin's currency (replaces "Points" placeholders).
````

-----

## 💻 Commands & Permissions

| Command                              | Description | Permission        |
|:-------------------------------------| :--- |:------------------|
| `/qeventbox spawn`                   | Manually spawns 1 crate at a random location. | `qeventbox.admin` |
| `/qeventbox spawnauto`               | Triggers the auto-spawn function immediately. | `qeventbox.admin` |
| `/qeventbox removeall`               | Removes all currently active event crates. | `qeventbox.admin` |
| `/qeventbox status`                  | Displays the status of active crates. | `qeventbox.admin` |
| `/qeventbox reload`                  | Reloads all plugin configurations. | `qeventbox.admin` |
| `/qeventbox shop`                    | Opens the Event Point Shop GUI (Player command). | `qeventbox.shop`    |
| `/qeventbox points`                  | View/modify a player's points (Admin command). | `qeventbox.admin` |

-----

## 💖 Support & Contribution

Found a bug or have a suggestion? We welcome contributions\!

* **Report Bugs:** Please open an issue on the [GitHub Issues](https://github.com/bintanqq/qeventbox/issues) page.
* **Suggest Features:** Feel free to open a new discussion thread.

<!-- end list -->