package me.bintanq.qEventBox.listeners;

import me.bintanq.qEventBox.QEventBox;
import me.bintanq.qEventBox.gui.ShopGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class ShopListener implements Listener {

    private final QEventBox plugin;

    public ShopListener(QEventBox plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        ShopGUI shopGUIInstance = plugin.getShopGUI();

        if (shopGUIInstance == null) return;

        String guiTitle = shopGUIInstance.getTitle();
        if (!e.getView().getTitle().equals(guiTitle)) return;

        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) {
            e.setCancelled(true);
            return;
        }

        ClickType click = e.getClick();
        if (click != ClickType.LEFT && click != ClickType.RIGHT) {
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);

        int slot = e.getSlot();
        String clickedKey = shopGUIInstance.getKeyBySlot(slot);
        if (clickedKey == null) return;

        int price = shopGUIInstance.getPrice(clickedKey);
        if (price <= 0) return;

        int playerPoints = plugin.getPointsManager().getPoints(player.getUniqueId());

        if (playerPoints < price) {
            String pointName = plugin.getConfig().getString("points", "Points");
            sendMessage(player, "&cYou have insufficient " + pointName + "!");
            return;
        }

        List<String> commands = shopGUIInstance.getCommands(clickedKey);
        if (commands != null) {
            for (String cmd : commands) {
                cmd = cmd.replace("%player%", player.getName());
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            }
        }

        plugin.getPointsManager().removePoints(player.getUniqueId(), price);

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        shopGUIInstance.updatePointsDisplay(player);
    }

    private void sendMessage(Player player, String msg) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&e[QEventBox] &r");
        String finalMessage = ChatColor.translateAlternateColorCodes('&', prefix + msg);

        player.sendMessage(finalMessage);
        player.playSound(player.getLocation(), "minecraft:entity.experience_orb.pickup", 1f, 1f);
    }
}