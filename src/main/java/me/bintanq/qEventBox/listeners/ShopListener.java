package me.bintanq.qEventBox.listeners;

import me.bintanq.qEventBox.QEventBox;
import me.bintanq.qEventBox.gui.ShopGUI;
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

        // 1. Basic Check: Ensure an item was clicked and it's not air
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        ShopGUI shopGUIInstance = plugin.getShopGUI();

        // Safety Check: Ensure ShopGUI instance is available
        if (shopGUIInstance == null) return;

        // Check Inventory Title: Ensure the clicked inventory is the shop GUI
        String guiTitle = shopGUIInstance.getTitle();
        if (!e.getView().getTitle().equals(guiTitle)) return;

        // 2. Click Location Check: Only allow clicks in the top inventory (Shop GUI)
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) {
            e.setCancelled(true);
            return;
        }

        // 3. Click Type Validation: Only allow LEFT or RIGHT click for purchase
        ClickType click = e.getClick();
        if (click != ClickType.LEFT && click != ClickType.RIGHT) {
            e.setCancelled(true); // Cancel invalid clicks (Shift, Middle, etc.)
            return;
        }

        // Cancel the event for all valid clicks within the shop to prevent item movement
        e.setCancelled(true);

        // 4. Item Validation: Get item key based on slot
        int slot = e.getSlot();
        String clickedKey = shopGUIInstance.getKeyBySlot(slot);
        if (clickedKey == null) return; // Not a registered item

        // Price check
        int price = shopGUIInstance.getPrice(clickedKey);
        if (price <= 0) return; // Decoration or 0-priced item

        // 5. Point Balance Check
        int playerPoints = plugin.getPointsManager().getPoints(player.getUniqueId());
        if (playerPoints < price) {
            // Get custom point name and send insufficient message
            String pointName = plugin.getConfig().getString("points", "Points");
            player.sendMessage("§cYou have insufficient " + pointName + "!");
            return;
        }

        // 6. Process Purchase and Rewards
        // Execute all associated commands (rewards)
        List<String> commands = shopGUIInstance.getCommands(clickedKey);
        if (commands != null) {
            for (String cmd : commands) {
                cmd = cmd.replace("%player%", player.getName());
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            }
        }

        // Deduct points from the player's balance
        plugin.getPointsManager().removePoints(player.getUniqueId(), price);

        // Sound feedback for successful purchase
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        // 7. Real-Time GUI Update: Refresh the shop display immediately
        shopGUIInstance.updatePointsDisplay(player);
    }
}