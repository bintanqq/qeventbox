package me.bintanq.qEventBox.commands;

import me.bintanq.qEventBox.QEventBox;
import me.bintanq.qEventBox.gui.ShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class EventShopCommand implements CommandExecutor, TabCompleter {

    private final QEventBox plugin;

    public EventShopCommand(QEventBox plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check 1: Command must be executed by a player
        if (!(sender instanceof Player)) {
            sendMessage(sender, "Only players can open the shop");
            return true;
        }

        Player p = (Player) sender;

        // Check 2: Permission check for using the shop command
        if (!p.hasPermission("qeventbox.shop")) {
            sendMessage(sender, "§cNo permission");
            return true;
        }

        // Initialize and open the Shop GUI
        // NOTE: It is recommended to use plugin.getShopGUI() instead of creating a new instance here.
        // Creating a new instance means the listener and configuration might be re-registered/reloaded.
        ShopGUI shopGUI = plugin.getShopGUI();
        if (shopGUI == null) {
            sendMessage(sender, "§cShop is not initialized.");
            return true;
        }

        shopGUI.openShop(p);

        return true;
    }

    // Utility method to send formatted plugin messages and sound feedback
    private void sendMessage(CommandSender sender, String msg) {
        String formatted = "\n\n§e[QEventBox] §r" + msg + "\n\n";
        sender.sendMessage(formatted);
        if (sender instanceof Player) {
            ((Player) sender).playSound(
                    ((Player) sender).getLocation(),
                    org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    1f,
                    1f
            );
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // No subcommands, so no tab completion needed
        return Collections.emptyList();
    }
}