package me.bintanq.qEventBox.commands;

import me.bintanq.qEventBox.QEventBox;
import me.bintanq.qEventBox.managers.CrateManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public class QEventBoxCommand implements CommandExecutor, TabCompleter {

    private final QEventBox plugin;
    private final List<String> subCommands = Arrays.asList("spawn", "spawnauto", "remove", "status", "reload", "tpworld");

    public QEventBoxCommand(QEventBox plugin) { this.plugin = plugin; }

    private void sendMessage(CommandSender sender, String msg) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&e[QEventBox] &r");
        String finalMessage = ChatColor.translateAlternateColorCodes('&', prefix + msg);

        sender.sendMessage(finalMessage);

        if (sender instanceof Player) {
            ((Player)sender).playSound(((Player)sender).getLocation(), "minecraft:entity.experience_orb.pickup", 1f, 1f);
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendMessage(sender, "&e&l--- QEventBox Commands ---");
            sendMessage(sender, "&b/qeventbox status &7- Check active crate status & next spawn time.");
            sendMessage(sender, "&b/qeventbox spawn &7- Spawn a crate at your location (if player) or a random location.");
            sendMessage(sender, "&b/qeventbox spawnauto &7- Manually trigger the auto-spawn task.");
            sendMessage(sender, "&b/qeventbox remove &7- Remove all currently active crates.");
            sendMessage(sender, "&b/qeventbox reload &7- Reload the plugin configuration.");

            sendMessage(sender, "&b/qwand &7- Manage the crate region wand.");
            return true;
        }

        if (!sender.hasPermission("qeventbox.admin")) {
            sendMessage(sender,"&cNo permission!");
            return true;
        }

        if (args.length == 0) {
            sendMessage(sender,"&cUsage: /qeventbox <spawn|spawnauto|remove|status|reload>");
            return true;
        }

        CrateManager cm = plugin.getCrateManager();
        String sub = args[0].toLowerCase();

        switch(sub) {
            case "spawn":
                if(sender instanceof Player) {
                    Player p = (Player)sender;
                    UUID id = cm.spawnCrateAt(p.getLocation());
                    if(id!=null) sendMessage(sender,"&aCrate spawned at your location (ID: "+id+")");
                    else sendMessage(sender,"&cFailed to spawn crate at your location.");
                } else {
                    UUID id = cm.spawnRandomCrate();
                    if(id!=null) sendMessage(sender,"&aCrate spawned at random location (ID: "+id+")");
                    else sendMessage(sender,"&cFailed to find location to spawn crate.");
                }
                break;
            case "spawnauto":
                cm.manualAutoSpawnOnce();
                sendMessage(sender,"&aAuto Box task started!");
                break;
            case "remove":
                cm.cleanupAll();
                sendMessage(sender,"&aAll active crates cleared.");
                break;
            case "status":
                sendMessage(sender,"&7Active crates: &a" + cm.getActiveCrates().size());

                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    for (CrateManager.CrateData data : cm.getActiveCrates().values()) {
                        Location loc = data.getLocation();
                        if (loc != null) {
                            sendClickableLocation(p, loc);
                        }
                    }
                } else {
                    for (CrateManager.CrateData data : cm.getActiveCrates().values()) {
                        Location loc = data.getLocation();
                        if (loc != null) {
                            sendMessage(sender," - " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + " (" + loc.getWorld().getName() + ")");
                        }
                    }
                }
                break;
            case "reload":
                plugin.reloadConfig();

                if (plugin.getShopGUI() != null) {
                    plugin.getShopGUI().reloadConfig();
                }
                sendMessage(sender,"&aAll configurations reloaded!");
                break;

            case "tpworld":
                if (!(sender instanceof Player) || args.length < 5) return true;

                Player p = (Player) sender;
                try {
                    String targetWorldName = args[1];
                    double tx = Double.parseDouble(args[2]) + 0.5;
                    double ty = Double.parseDouble(args[3]);
                    double tz = Double.parseDouble(args[4]) + 0.5;

                    World targetWorld = Bukkit.getWorld(targetWorldName);
                    if (targetWorld == null) {
                        sendMessage(p, "&cWorld " + targetWorldName + " not found!");
                        return true;
                    }

                    Location targetLoc = new Location(targetWorld, tx, ty, tz, p.getLocation().getYaw(), p.getLocation().getPitch());
                    p.teleport(targetLoc);
                    sendMessage(p, "&aTeleported to crate location in &b" + targetWorldName);

                } catch (NumberFormatException e) {
                }
                break;

            default:
                sendMessage(sender,"&cUnknown subcommand. Use /qeventbox help.");
        }
        return true;
    }

    private void sendClickableLocation(Player p, Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        String worldName = loc.getWorld().getName();

        String cmd = "/qeventbox tpworld " + worldName + " " + x + " " + y + " " + z;

        String prefixString = plugin.getConfig().getString("messages.prefix", "&e[QEventBox] &r");
        String formattedPrefix = ChatColor.translateAlternateColorCodes('&', prefixString);

        String locationText = " - §b[" + worldName + ": " + x + ", " + y + ", " + z + "]§r";
        String hoverText = "&eClick to Teleport to " + worldName;

        TextComponent locationComponent = new TextComponent(locationText);
        locationComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));

        String translatedHoverText = ChatColor.translateAlternateColorCodes('&', hoverText);
        locationComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(translatedHoverText).create()));

        TextComponent prefixComponent = new TextComponent(formattedPrefix);
        prefixComponent.addExtra(locationComponent);

        p.spigot().sendMessage(prefixComponent);

        p.playSound(p.getLocation(),"minecraft:entity.experience_orb.pickup",1f,1f);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("qeventbox.admin")) return Collections.emptyList();

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String current = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (!sub.equalsIgnoreCase("tpworld") && sub.startsWith(current)) {
                    completions.add(sub);
                }
            }
            return completions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }

        return Collections.emptyList();
    }
}