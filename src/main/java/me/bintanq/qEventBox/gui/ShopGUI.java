package me.bintanq.qEventBox.gui;

import me.bintanq.qEventBox.QEventBox;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

import java.io.File;
import java.util.List;

public class ShopGUI implements Listener {



    private final QEventBox plugin;
    private FileConfiguration guiCfg;

    public ShopGUI(QEventBox plugin) {
        this.plugin = plugin;
        loadGUIConfig();
    }

    public FileConfiguration getGuiConfig() {
        if (guiCfg == null) loadGUIConfig();
        return guiCfg;
    }

    public String getTitle() {
        return guiCfg.getString("title", "Event Shop");
    }

    private void loadGUIConfig() {
        File file = new File(plugin.getDataFolder(), "gui/gui.yml"); // <- folder gui
        if (!file.exists()) plugin.saveResource("gui.yml", false); // ini tetap ambil dari resources root
        guiCfg = YamlConfiguration.loadConfiguration(file);
    }

    public void openShop(Player p) {
        if (guiCfg == null) loadGUIConfig();

        String title = guiCfg.getString("title", "Event Shop");
        int size = guiCfg.getInt("size", 36);
        Inventory inv = Bukkit.createInventory(null, size, title);

        if (guiCfg.getConfigurationSection("items") != null) {
            for (String key : guiCfg.getConfigurationSection("items").getKeys(false)) {
                String path = "items." + key;
                Material mat = Material.valueOf(guiCfg.getString(path + ".block", "STONE").toUpperCase());
                String display = guiCfg.getString(path + ".display_name", key);
                List<String> lore = guiCfg.getStringList(path + ".lore");

                // replace placeholder
                display = display.replace("%qpoints%", String.valueOf(plugin.getPointsManager().getPoints(p.getUniqueId())));
                lore.replaceAll(l -> l.replace("%qpoints%", String.valueOf(plugin.getPointsManager().getPoints(p.getUniqueId())))
                        .replace("%qcrate_status%", plugin.getCrateManager().getActiveCratesStatus()));

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(display);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }

                List<Integer> slots = guiCfg.getIntegerList(path + ".positions");
                for (int slot : slots) {
                    if (slot < size) inv.setItem(slot, item);
                }
            }
        }

        p.openInventory(inv);
    }

    // Mapping slot -> item key
    public String getKeyBySlot(int slot) {
        if (guiCfg.getConfigurationSection("items") == null) return null;
        for (String key : guiCfg.getConfigurationSection("items").getKeys(false)) {
            List<Integer> positions = guiCfg.getIntegerList("items." + key + ".positions");
            if (positions.contains(slot)) return key;
        }
        return null;
    }

    // Ambil price item
    public int getPrice(String key) {
        return guiCfg.getInt("items." + key + ".price", 0);
    }

    // Ambil commands item
    public List<String> getCommands(String key) {
        return guiCfg.getStringList("items." + key + ".command");
    }
}
