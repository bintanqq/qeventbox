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

*Add your visual content here to showcase the plugin's features! This is crucial for attracting users.*

| Feature Showcase | Description |
| :--- | :--- |
| **** | The Event Crate spawning randomly within the defined region. |
| **** | Player claiming the crate and receiving points/items. |
| **** | The fully customizable Shop GUI for point exchange. |

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
````

-----

## 💻 Commands & Permissions

| Command | Description | Permission        |
| :--- | :--- |:------------------|
| `/qeventbox spawn` | Manually spawns 1 crate at a random location. | `qeventbox.admin` |
| `/qeventbox autospawn <amount>` | Triggers the auto-spawn function immediately. | `qeventbox.admin` |
| `/qeventbox removeall` | Removes all currently active event crates. | `qeventbox.admin` |
| `/qeventbox status` | Displays the status of active crates. | `qeventbox.admin` |
| `/qeventbox reload` | Reloads all plugin configurations. | `qeventbox.admin` |
| `/qeventbox shop` | Opens the Event Point Shop GUI (Player command). | `qeventbox.shop`    |
| `/qeventbox points <player> <amount>` | View/modify a player's points (Admin command). | `qeventbox.admin` |

-----

## 💖 Support & Contribution

Found a bug or have a suggestion? We welcome contributions\!

* **Report Bugs:** Please open an issue on the [GitHub Issues](https://github.com/bintanqq/qeventbox/issues) page.
* **Suggest Features:** Feel free to open a new discussion thread.

<!-- end list -->