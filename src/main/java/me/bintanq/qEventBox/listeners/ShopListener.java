package me.bintanq.qEventBox.listeners;

import me.bintanq.qEventBox.QEventBox;
import me.bintanq.qEventBox.gui.ShopGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

        ShopGUI shopGUI = new ShopGUI(plugin); // ambil dummy untuk title
        String guiTitle = shopGUI.getTitle();

        if (!e.getView().getTitle().equals(guiTitle)) return;

        // mencegah player mengambil item
        e.setCancelled(true);

        // Ambil key item dari inventory slot
        int slot = e.getSlot();
        String clickedKey = shopGUI.getKeyBySlot(slot); // kita buat method di ShopGUI untuk mapping slot -> item key
        if (clickedKey == null) return;

        // Ambil harga item
        int price = shopGUI.getPrice(clickedKey);
        if (price <= 0) return; // hanya process item yang ada price

        int playerPoints = plugin.getPointsManager().getPoints(player.getUniqueId());
        if (playerPoints < price) {
            player.sendMessage("[QEventBox] §cYou have insufficient points!");
            return;
        }

        // Execute all commands
        List<String> commands = shopGUI.getCommands(clickedKey);
        if (commands != null) {
            for (String cmd : commands) {
                cmd = cmd.replace("%player%", player.getName());
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            }
        }

        // Deduct points sesuai price
        plugin.getPointsManager().removePoints(player.getUniqueId(), price);

        // Sound feedback
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }
}
