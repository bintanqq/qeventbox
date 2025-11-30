package me.bintanq.qEventBox.commands;

import me.bintanq.qEventBox.QEventBox;
import me.bintanq.qEventBox.managers.CrateManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

public class QEventBoxCommand implements CommandExecutor, TabCompleter {

    private final QEventBox plugin;
    private final List<String> subCommands = Arrays.asList("spawn", "spawnauto", "remove", "status", "reload");

    public QEventBoxCommand(QEventBox plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission check for all subcommands
        if (!sender.hasPermission("qeventbox.admin")) {
            sendMessage(sender,"§cNo permission");
            return true;
        }

        // Display usage if no arguments provided
        if (args.length == 0) {
            sendMessage(sender,"Usage: /qeventbox <spawn|spawnauto|remove|status|reload>");
            return true;
        }

        CrateManager cm = plugin.getCrateManager();
        String sub = args[0].toLowerCase();

        switch(sub) {
            case "spawn":
                // Spawns a crate: at player's location (if player) or random location (if console)
                if(sender instanceof Player) {
                    Player p = (Player)sender;
                    UUID id = cm.spawnCrateAt(p.getLocation());
                    if(id!=null) sendMessage(sender,"§aCrate spawned at your location (id: "+id+")");
                    else sendMessage(sender,"§cFailed to spawn crate at your location");
                } else {
                    UUID id = cm.spawnRandomCrate();
                    if(id!=null) sendMessage(sender,"§aCrate spawned at random location (id: "+id+")");
                    else sendMessage(sender,"§cFailed to find location to spawn crate");
                }
                break;
            case "spawnauto":
                // Manually triggers the auto-spawn logic once
                cm.manualAutoSpawnOnce();
                sendMessage(sender,"§aAuto Box task started!");
                break;
            case "remove":
                // Removes all active crates
                cm.cleanupAll();
                sendMessage(sender,"§aRemoved all crates");
                break;
            case "status":
                // Displays the status and location of all active crates
                sendMessage(sender,"Active crates: " + cm.getActiveCrates().size());

                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    // Send clickable location components for each active crate
                    for (CrateManager.CrateData data : cm.getActiveCrates().values()) {
                        Location loc = data.getLocation();
                        if (loc != null) {
                            sendClickableLocation(p, loc);
                        }
                    }
                } else {
                    // Default console output
                    for (CrateManager.CrateData data : cm.getActiveCrates().values()) {
                        Location loc = data.getLocation();
                        if (loc != null) {
                            sendMessage(sender," - " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + " (" + loc.getWorld().getName() + ")");
                        }
                    }
                }
                break;
            case "reload":
                // Reloads main config and GUI config
                plugin.reloadConfig();

                if (plugin.getShopGUI() != null) {
                    plugin.getShopGUI().reloadConfig();
                }
                sendMessage(sender,"§aAll configurations reloaded!");
                break;
            default:
                sendMessage(sender,"Unknown subcommand");
        }
        return true;
    }

    // Sends a Spigot/JSON message allowing the player to teleport by clicking the coordinates
    private void sendClickableLocation(Player p, Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        String world = loc.getWorld().getName();

        // Command to execute on click (teleports the player)
        String cmd = "/teleport " + p.getName() + " " + x + " " + y + " " + z;

        // Formatted location text
        String locationText = " - §b[" + x + ", " + y + ", " + z + " (" + world + ")]§r";
        String hoverText = "§eClick to Teleport.";

        // Create the clickable component
        TextComponent locationComponent = new TextComponent(locationText);
        locationComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));

        // Add hover text component
        locationComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));

        // Combine with plugin header
        TextComponent header = new TextComponent("§e[QEventBox] §r");
        header.addExtra(locationComponent);

        p.spigot().sendMessage(header); // Send JSON message

        // Sound feedback
        p.playSound(p.getLocation(),"minecraft:entity.experience_orb.pickup",1f,1f);
    }

    // Utility method to send standard (non-JSON) plugin messages
    private void sendMessage(CommandSender sender,String msg) {
        String formatted = "\n\n§e[QEventBox] §r"+msg+"\n\n";
        sender.sendMessage(formatted);
        if(sender instanceof Player) ((Player)sender).playSound(((Player)sender).getLocation(),"minecraft:entity.experience_orb.pickup",1f,1f);
    }

    // ---------------- TabCompleter ----------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("qeventbox.admin")) return Collections.emptyList();

        if (args.length == 1) {
            // Autocomplete subcommands
            List<String> completions = new ArrayList<>();
            String current = args[0].toLowerCase();
            for (String sub : subCommands) if (sub.startsWith(current)) completions.add(sub);
            return completions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            // Autocomplete player names for /qeventbox spawn [player] (though not implemented in executor)
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }

        return Collections.emptyList();
    }
}