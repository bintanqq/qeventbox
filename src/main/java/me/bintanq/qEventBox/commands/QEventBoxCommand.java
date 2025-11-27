package me.bintanq.qEventBox.commands;

import me.bintanq.qEventBox.QEventBox;
import me.bintanq.qEventBox.managers.CrateManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class QEventBoxCommand implements CommandExecutor {

    private final QEventBox plugin;
    public QEventBoxCommand(QEventBox plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("qeventbox.admin")) { sendMessage(sender,"§cNo permission"); return true; }

        if (args.length == 0) { sendMessage(sender,"Usage: /qeventbox <spawn|spawnauto|remove|status|reload>"); return true; }

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
                cm.startAutoSpawnTask();
                sendMessage(sender,"§aAutospawn task started!");
                break;
            case "remove": cm.cleanupAll(); sendMessage(sender,"§aRemoved all crates"); break;
            case "status": sendMessage(sender,"Active crates: "+cm.getActiveCrates().size()); break;
            case "reload": plugin.reloadConfig(); sendMessage(sender,"§aConfig reloaded"); break;
            default: sendMessage(sender,"Unknown subcommand");
        }
        return true;
    }

    private void sendMessage(CommandSender sender,String msg) {
        String formatted = "\n\n§e§l[QEventBox] §r"+msg+"\n\n";
        sender.sendMessage(formatted);
        if(sender instanceof Player) ((Player)sender).playSound(((Player)sender).getLocation(),"minecraft:entity.experience_orb.pickup",1f,1f);
    }
}
