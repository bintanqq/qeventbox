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
import org.bukkit.inventory.meta.SkullMeta; // Import diperlukan
import me.bintanq.qEventBox.utility.SkullUtility;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopGUI implements Listener {

    private final QEventBox plugin;
    private FileConfiguration guiCfg;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public void reloadConfig() {
        loadGUIConfig();
    }

    public String getPointName() {
        return plugin.getConfig().getString("points", "Points");
    }

    public ShopGUI(QEventBox plugin) {
        this.plugin = plugin;
        loadGUIConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public FileConfiguration getGuiConfig() {
        if (guiCfg == null) loadGUIConfig();
        return guiCfg;
    }

    public String getTitle() {
        return ChatColor.translateAlternateColorCodes('&',
                guiCfg.getString("title", "&aEvent Shop"));
    }

    private void loadGUIConfig() {
        File file = new File(plugin.getDataFolder(), "gui/gui.yml");

        if (!file.exists()) {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            plugin.saveResource("gui/gui.yml", false);
        }
        guiCfg = YamlConfiguration.loadConfiguration(file);
    }

    public void openShop(Player p) {
        if (guiCfg == null) loadGUIConfig();

        String title = getTitle();
        int size = guiCfg.getInt("size", 36);

        Inventory inv = openInventories.getOrDefault(p.getUniqueId(), Bukkit.createInventory(null, size, title));

        populateInventory(p, inv);

        openInventories.put(p.getUniqueId(), inv);

        p.openInventory(inv);
    }

    private String applyPlaceholders(Player p, String line, String pointsName, String fullPointsDisplay, String crateStatus) {
        String processedLine = line.replace("%qpoints%", fullPointsDisplay);
        processedLine = processedLine.replace("Points", pointsName);
        processedLine = processedLine.replace("%qcrate_status%", crateStatus);
        return ChatColor.translateAlternateColorCodes('&', processedLine);
    }

    public void populateInventory(Player p, Inventory inv) {
        if (guiCfg.getConfigurationSection("items") != null) {

            String pointsName = getPointName();
            String playerPointsAmount = String.valueOf(plugin.getPointsManager().getPoints(p.getUniqueId()));
            String crateStatus = plugin.getCrateManager().getActiveCratesStatus();
            String fullPointsDisplay = playerPointsAmount + " " + pointsName;

            for (String key : guiCfg.getConfigurationSection("items").getKeys(false)) {
                String path = "items." + key;
                Material mat = Material.valueOf(guiCfg.getString(path + ".block", "STONE").toUpperCase());

                String rawDisplay = guiCfg.getString(path + ".display_name", key);
                String display = applyPlaceholders(p, rawDisplay, pointsName, fullPointsDisplay, crateStatus);

                List<String> rawLore = guiCfg.getStringList(path + ".lore");
                List<String> finalLore = new ArrayList<>();

                for (String line : rawLore) {
                    finalLore.add(applyPlaceholders(p, line, pointsName, fullPointsDisplay, crateStatus));
                }

                ItemStack item = new ItemStack(mat);

                // --- START: LOGIC DUKUNGAN PLAYER HEAD ---
                if (mat == Material.PLAYER_HEAD) {

                    String owner = guiCfg.getString(path + ".skull-owner");
                    String texture = guiCfg.getString(path + ".skull-texture");

                    item = new ItemStack(mat, 1, (short) 3);
                    SkullMeta skullMeta = (SkullMeta) item.getItemMeta();

                    if (texture != null) {
                        // KASUS 2: Custom Base64 Texture (MEMANGGIL CRATEMANAGER)
                        // Mengganti pemanggilan ke SkullUtility.setSkullTexture
                        if (!applyCrateTextureToMeta(skullMeta, texture)) {
                            plugin.getLogger().severe("FAILED to set Base64 texture for item " + key + "! Falling back to error skull.");
                            skullMeta.setOwner("MHF_Exclamation"); // Fallback
                        }

                    } else if (owner != null && owner.equalsIgnoreCase("%player%")) {
                        // KASUS 1: Dynamic Viewer Head
                        skullMeta.setOwningPlayer(p);

                    } else if (owner != null) {
                        // KASUS 3: Static Username/UUID Head
                        skullMeta.setOwner(owner);
                    } else {
                        skullMeta.setOwner("MHF_Question"); // Default Fallback
                    }

                    skullMeta.setDisplayName(display);
                    skullMeta.setLore(finalLore);
                    item.setItemMeta(skullMeta);

                } else {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(display);
                        meta.setLore(finalLore);
                        item.setItemMeta(meta);
                    }
                }

                List<Integer> slots = guiCfg.getIntegerList(path + ".positions");
                for (int slot : slots) {
                    if (slot < inv.getSize()) inv.setItem(slot, item);
                }
            }
        }
    }

    private boolean applyCrateTextureToMeta(SkullMeta meta, String base64) {
        return plugin.getCrateManager().applyTextureToMeta(meta, base64);
    }

    public void updatePointsDisplay(Player p) {
        Inventory inv = openInventories.get(p.getUniqueId());

        if (inv == null) {
            if (p.getOpenInventory() != null &&
                    p.getOpenInventory().getTopInventory() != null &&
                    ChatColor.translateAlternateColorCodes('&', p.getOpenInventory().getTitle()).equals(getTitle())) {

                inv = p.getOpenInventory().getTopInventory();
                openInventories.put(p.getUniqueId(), inv);

            } else {
                return;
            }
        }

        if (inv != null) {
            populateInventory(p, inv);
            p.updateInventory();
        }
    }

    public Map<UUID, Inventory> getOpenInventories() {
        return openInventories;
    }

    public String getKeyBySlot(int slot) {
        if (guiCfg.getConfigurationSection("items") == null) return null;
        for (String key : guiCfg.getConfigurationSection("items").getKeys(false)) {
            List<Integer> positions = guiCfg.getIntegerList("items." + key + ".positions");
            if (positions.contains(slot)) return key;
        }
        return null;
    }

    public int getPrice(String key) {
        return guiCfg.getInt("items." + key + ".price", 0);
    }

    public List<String> getCommands(String key) {
        return guiCfg.getStringList("items." + key + ".command");
    }

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