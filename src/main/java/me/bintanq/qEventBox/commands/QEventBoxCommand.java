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
                if (sender instanceof Player && args.length==1) {
                    Player p = (Player) sender;
                    UUID id = cm.spawnCrateAt(p.getLocation());
                    if (id != null) sendMessage(sender,"§aCrate spawned at your location (id: " + id + ")");
                    else sendMessage(sender,"§cFailed to spawn crate at your location");
                } else {
                    UUID id = cm.spawnRandomCrate();
                    if (id != null) sendMessage(sender,"§aCrate spawned at random location (id: " + id + ")");
                    else sendMessage(sender,"§cFailed to find location to spawn crate");
                }
                break;

            case "spawnauto":
                // trigger spawn otomatis sekarang sesuai jumlah config
                int amt = plugin.getConfig().getInt("crate.amount",1);
                for (int i=0;i<amt;i++) cm.spawnRandomCrate();
                sendMessage(sender,"§aSpawn otomatis dijalankan untuk "+amt+" crate!");
                break;

            case "remove":
                if (args.length<2) { sendMessage(sender,"Specify crate id (use status to see active ones)"); return true; }
                try {
                    UUID id = UUID.fromString(args[1]);
                    cm.cleanupAll(); // biar safe, remove all
                    sendMessage(sender,"§aRemoved crate "+id);
                } catch (IllegalArgumentException e) { sendMessage(sender,"Bad UUID"); }
                break;

            case "status":
                sendMessage(sender,"Active crates: "+cm.getActiveCrates().size());
                for(UUID id : cm.getActiveCrates().keySet()) sendMessage(sender," - "+id.toString());
                break;

            case "reload":
                plugin.reloadConfig();
                sendMessage(sender,"§aConfig reloaded");
                break;

            default: sendMessage(sender,"Unknown subcommand");
        }

        return true;
    }

    private void sendMessage(CommandSender sender, String msg) {
        String formatted = "\n\n§e§l[QEventBox] §r"+msg+"\n\n";
        sender.sendMessage(formatted);
        if(sender instanceof Player) {
            ((Player)sender).playSound(((Player)sender).getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f);
        }
    }
}