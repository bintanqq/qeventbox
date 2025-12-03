package me.bintanq.qEventBox.commands;

import me.bintanq.qEventBox.QEventBox;
import me.bintanq.qEventBox.gui.ShopGUI;
import org.bukkit.ChatColor;
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
        if (!(sender instanceof Player)) {
            sendMessage(sender, "&cOnly players can open the shop.");
            return true;
        }

        Player p = (Player) sender;

        if (!p.hasPermission("qeventbox.shop")) {
            sendMessage(sender, "&cNo permission.");
            return true;
        }

        ShopGUI shopGUI = plugin.getShopGUI();
        if (shopGUI == null) {
            sendMessage(sender, "&cShop is not initialized.");
            return true;
        }

        shopGUI.openShop(p);

        return true;
    }

    private void sendMessage(CommandSender sender, String msg) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&e[QEventBox] &r");
        String finalMessage = ChatColor.translateAlternateColorCodes('&', prefix + msg);

        sender.sendMessage(finalMessage);

        if (sender instanceof Player) {
            ((Player)sender).playSound(((Player)sender).getLocation(), "minecraft:entity.experience_orb.pickup", 1f, 1f);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}