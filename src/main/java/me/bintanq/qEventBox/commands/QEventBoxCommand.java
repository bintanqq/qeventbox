package me.bintanq.qEventBox.commands;

import me.bintanq.qEventBox.QEventBox;
import me.bintanq.qEventBox.managers.CrateManager;
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
        if (!sender.hasPermission("qeventbox.admin")) {
            sendMessage(sender,"§cNo permission");
            return true;
        }

        if (args.length == 0) {
            sendMessage(sender,"Usage: /qeventbox <spawn|spawnauto|remove|status|reload>");
            return true;
        }

        CrateManager cm = plugin.getCrateManager();
        String sub = args[0].toLowerCase();

        switch(sub) {
            case "spawn":
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
                cm.manualAutoSpawnOnce();
                sendMessage(sender,"§aAuto Box task started!");
                break;
            case "remove":
                cm.cleanupAll();
                sendMessage(sender,"§aRemoved all crates");
                break;
            case "status":
                sendMessage(sender,"Active crates: " + cm.getActiveCrates().size());
                for (CrateManager.CrateData data : cm.getActiveCrates().values()) {
                    Location loc = data.getLocation();
                    if (loc != null) {
                        sendMessage(sender," - " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + " (" + loc.getWorld().getName() + ")");
                    }
                }
                break;
            case "reload":
                plugin.reloadConfig();
                sendMessage(sender,"§aConfig reloaded");
                break;
            default:
                sendMessage(sender,"Unknown subcommand");
        }
        return true;
    }

    private void sendMessage(CommandSender sender,String msg) {
        String formatted = "\n\n§e§l[QEventBox] §r"+msg+"\n\n";
        sender.sendMessage(formatted);
        if(sender instanceof Player) ((Player)sender).playSound(((Player)sender).getLocation(),"minecraft:entity.experience_orb.pickup",1f,1f);
    }

    // ---------------- TabCompleter ----------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("qeventbox.admin")) return Collections.emptyList();

        if (args.length == 1) {
            // autocomplete subcommands
            List<String> completions = new ArrayList<>();
            String current = args[0].toLowerCase();
            for (String sub : subCommands) if (sub.startsWith(current)) completions.add(sub);
            return completions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            // autocomplete nama player untuk spawn
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }

        return Collections.emptyList();
    }
}
