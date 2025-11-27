package me.bintanq.qEventBox.commands;

import me.bintanq.qEventBox.QEventBox;
import me.bintanq.qEventBox.gui.ShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EventShopCommand implements CommandExecutor {

    private final QEventBox plugin;

    public EventShopCommand(QEventBox plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "Only players can open the shop");
            return true;
        }

        Player p = (Player) sender;

        if (!p.hasPermission("qeventbox.shop")) {
            sendMessage(sender, "§cNo permission");
            return true;
        }

        // Buat instance ShopGUI dengan plugin
        ShopGUI shopGUI = new ShopGUI(plugin);
        shopGUI.openShop(p); // openShop hanya butuh Player

        return true;
    }

    private void sendMessage(CommandSender sender, String msg) {
        String formatted = "\n\n§e§l[QEventBox] §r" + msg + "\n\n";
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
}