package me.bintanq.qEventBox.gui;

import me.bintanq.qEventBox.QEventBox;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopGUI implements Listener {

    private final QEventBox plugin;
    private FileConfiguration guiCfg;

    // Map to track all currently open Shop Inventory instances
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    // Reloads the GUI configuration from file
    public void reloadConfig() {
        loadGUIConfig();
    }

    // Retrieves the custom point name from the main config
    public String getPointName() {
        return plugin.getConfig().getString("points", "Points");
    }

    public ShopGUI(QEventBox plugin) {
        this.plugin = plugin;
        loadGUIConfig();
        // Register this class as a listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public FileConfiguration getGuiConfig() {
        if (guiCfg == null) loadGUIConfig();
        return guiCfg;
    }

    // Retrieves and formats the GUI title
    public String getTitle() {
        return ChatColor.translateAlternateColorCodes('&',
                guiCfg.getString("title", "&aEvent Shop"));
    }

    // Loads the gui.yml file configuration
    private void loadGUIConfig() {
        File file = new File(plugin.getDataFolder(), "gui/gui.yml");

        if (!file.exists()) {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            // Saves the resource to the correct nested path
            plugin.saveResource("gui/gui.yml", false);
        }
        guiCfg = YamlConfiguration.loadConfiguration(file);
    }

    // Opens the shop inventory for the player
    public void openShop(Player p) {
        if (guiCfg == null) loadGUIConfig();

        String title = getTitle();
        int size = guiCfg.getInt("size", 36);

        // Get existing inventory instance or create a new one
        Inventory inv = openInventories.getOrDefault(p.getUniqueId(), Bukkit.createInventory(null, size, title));

        // Populate/refresh items
        populateInventory(p, inv);

        // Track the inventory instance
        openInventories.put(p.getUniqueId(), inv);

        p.openInventory(inv);
    }

    // Renders all items in the inventory, handling placeholders
    public void populateInventory(Player p, Inventory inv) {
        if (guiCfg.getConfigurationSection("items") != null) {

            // Prepare placeholder values once per rendering cycle
            String pointsName = getPointName();
            String playerPointsAmount = String.valueOf(plugin.getPointsManager().getPoints(p.getUniqueId()));
            String crateStatus = plugin.getCrateManager().getActiveCratesStatus();

            // Combined point display (e.g., "150 Token") for %qpoints%
            String fullPointsDisplay = playerPointsAmount + " " + pointsName;

            for (String key : guiCfg.getConfigurationSection("items").getKeys(false)) {
                String path = "items." + key;
                Material mat = Material.valueOf(guiCfg.getString(path + ".block", "STONE").toUpperCase());

                // 1. Process Display Name
                String rawDisplay = guiCfg.getString(path + ".display_name", key);
                String display = ChatColor.translateAlternateColorCodes('&', rawDisplay);

                // Replace generic "Points" in display name with custom name
                if (display.contains("Points")) {
                    display = display.replace("Points", pointsName);
                }

                // 2. Process Lore and Placeholders
                List<String> rawLore = guiCfg.getStringList(path + ".lore");
                List<String> finalLore = new ArrayList<>();

                for (String line : rawLore) {
                    // A. Replace combined points placeholder
                    line = line.replace("%qpoints%", fullPointsDisplay);

                    // B. Replace generic "Points" keyword in lore (e.g., in price description)
                    line = line.replace("Points", pointsName);

                    // C. Replace Crate Status placeholder
                    line = line.replace("%qcrate_status%", crateStatus);

                    finalLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }

                // Build the ItemStack
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(display);
                    meta.setLore(finalLore);
                    item.setItemMeta(meta);
                }

                // Place the item in all configured slots
                List<Integer> slots = guiCfg.getIntegerList(path + ".positions");
                for (int slot : slots) {
                    if (slot < inv.getSize()) inv.setItem(slot, item);
                }
            }
        }
    }

    // Updates the display of dynamic items (Points, Status) for an open GUI
    public void updatePointsDisplay(Player p) {
        // Attempt to get the tracked inventory instance
        Inventory inv = openInventories.get(p.getUniqueId());

        // Fallback check: If map failed, check the currently opened inventory
        if (inv == null) {
            if (p.getOpenInventory() != null &&
                    p.getOpenInventory().getTopInventory() != null &&
                    ChatColor.translateAlternateColorCodes('&', p.getOpenInventory().getTitle()).equals(getTitle())) {

                inv = p.getOpenInventory().getTopInventory();
                openInventories.put(p.getUniqueId(), inv); // Re-track the instance

            } else {
                return;
            }
        }

        if (inv != null) {
            // Re-render only dynamic items
            populateInventory(p, inv);
            // Force client update
            p.updateInventory();
        }
    }

    // Getter for the map of open inventories (used by the repeating task)
    public Map<UUID, Inventory> getOpenInventories() {
        return openInventories;
    }

    // Finds the item key based on the clicked slot
    public String getKeyBySlot(int slot) {
        if (guiCfg.getConfigurationSection("items") == null) return null;
        for (String key : guiCfg.getConfigurationSection("items").getKeys(false)) {
            List<Integer> positions = guiCfg.getIntegerList("items." + key + ".positions");
            if (positions.contains(slot)) return key;
        }
        return null;
    }

    // Retrieves the price of an item key
    public int getPrice(String key) {
        return guiCfg.getInt("items." + key + ".price", 0);
    }

    // Retrieves the command list for an item key
    public List<String> getCommands(String key) {
        return guiCfg.getStringList("items." + key + ".command");
    }

    // Listener to remove inventory instance from the map when the GUI is closed
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        if (openInventories.containsKey(playerUUID)) {
            if (event.getInventory().equals(openInventories.get(playerUUID))) {
                openInventories.remove(playerUUID);
            }
        }
    }
}