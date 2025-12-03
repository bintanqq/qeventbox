package me.bintanq.qEventBox.listeners;

import me.bintanq.qEventBox.QEventBox;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.UUID;

public class RegionWandListener implements Listener {

    private final QEventBox plugin;
    private final HashMap<UUID, Location> pos1Map = new HashMap<>();
    private final HashMap<UUID, Location> pos2Map = new HashMap<>();

    public RegionWandListener(QEventBox plugin) {
        this.plugin = plugin;
    }

    public HashMap<UUID, Location> getPos1Map() { return pos1Map; }
    public HashMap<UUID, Location> getPos2Map() { return pos2Map; }

    private void sendWandMessage(Player p, String message) {
        FileConfiguration config = plugin.getConfig();
        String prefix = config.getString("messages.prefix", "&e[QEventBox] &r");
        String finalMessage = ChatColor.translateAlternateColorCodes('&', prefix + message);
        p.sendMessage(finalMessage);
    }

    private boolean isRegionWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        FileConfiguration config = plugin.getConfig();

        Material requiredMaterial = Material.getMaterial(config.getString("wand-item.material", "GOLDEN_AXE"));
        if (item.getType() != requiredMaterial) return false;

        ItemMeta meta = item.getItemMeta();
        String requiredName = ChatColor.translateAlternateColorCodes('&',
                config.getString("wand-item.name", "&b&lEventBox Region Wand"));
        if (!meta.hasDisplayName() || !meta.getDisplayName().equals(requiredName)) return false;

        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        if (!isRegionWand(item) || !player.hasPermission("qeventbox.wand.use")) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR) {
            event.setCancelled(true);
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            Location loc = clickedBlock.getLocation();
            pos1Map.put(player.getUniqueId(), loc);

            sendWandMessage(player, "&7Position 1 set to: &a" + formatLocation(loc));

        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            if (player.isSneaking()) {
                sendWandMessage(player, "&7Use &b/qwand save &7to save the region!");
                return;
            }

            Location loc = clickedBlock.getLocation();
            pos2Map.put(player.getUniqueId(), loc);

            sendWandMessage(player, "&7Position 2 set to: &c" + formatLocation(loc));
        }
    }

    private String formatLocation(Location loc) {
        return loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}