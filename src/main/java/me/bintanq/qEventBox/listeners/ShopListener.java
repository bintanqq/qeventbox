package me.bintanq.qEventBox.listeners;

import me.bintanq.qEventBox.QEventBox;
import me.bintanq.qEventBox.gui.ShopGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ShopListener implements Listener {

    private final QEventBox plugin;
    public ShopListener(QEventBox plugin){ this.plugin=plugin; }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        ShopGUI shopGUI = new ShopGUI(plugin); // dummy instance untuk ambil title
        String guiTitle = shopGUI.getTitle(); // tambahkan getter getTitle() di ShopGUI

        if (!e.getView().getTitle().equals(guiTitle)) return;

        // mencegah player mengambil item
        e.setCancelled(true);

        // logic klik item
        player.sendMessage("You clicked: " + e.getCurrentItem().getItemMeta().getDisplayName());
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        // TODO: tambahkan logic beli item / deduct points sesuai keinginan
    }
}