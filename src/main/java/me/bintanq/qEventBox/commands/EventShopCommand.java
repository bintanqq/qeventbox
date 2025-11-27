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
        if (!(sender instanceof Player)) {
            sendMessage(sender, "Only players can open the shop");
            return true;
        }

        Player p = (Player) sender;

        if (!p.hasPermission("qeventbox.shop")) {
            sendMessage(sender, "§cNo permission");
            return true;
        }

        ShopGUI shopGUI = new ShopGUI(plugin);
        shopGUI.openShop(p);

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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // shop cuma player & tanpa subcommand
        return Collections.emptyList();
    }
}
