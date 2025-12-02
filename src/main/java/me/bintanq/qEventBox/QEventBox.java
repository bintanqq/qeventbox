package me.bintanq.qEventBox;

import me.bintanq.qEventBox.commands.EventShopCommand;
import me.bintanq.qEventBox.commands.PointsCommand;
import me.bintanq.qEventBox.commands.QEventBoxCommand;
import me.bintanq.qEventBox.commands.RegionWandCommand;
import me.bintanq.qEventBox.gui.ShopGUI;
import me.bintanq.qEventBox.listeners.InteractListener;
import me.bintanq.qEventBox.listeners.PlayerListener;
import me.bintanq.qEventBox.listeners.RegionWandListener;
import me.bintanq.qEventBox.listeners.ShopListener;
import me.bintanq.qEventBox.managers.CrateManager;
import me.bintanq.qEventBox.managers.PointsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;


public class QEventBox extends JavaPlugin {

    private static QEventBox instance;

    private CrateManager crateManager;
    private PointsManager pointsManager;
    private ShopGUI shopGUI;
    private RegionWandListener regionWandListener;


    @Override
    public void onLoad() { instance = this; }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Setup plugin folders for configuration and data storage
        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists()) pluginFolder.mkdirs();

        File guiFolder = new File(pluginFolder, "gui");
        if (!guiFolder.exists()) guiFolder.mkdirs();

        File pointsFolder = new File(pluginFolder, "points");
        if (!pointsFolder.exists()) pointsFolder.mkdirs();

        // Copy default resource files to their respective folders
        copyResourceToFolder("gui.yml", new File(guiFolder, "gui.yml"));
        copyResourceToFolder("points.yml", new File(pointsFolder, "points.yml"));

        // Delete old config files from the plugin root directory (Cleanup)
        File oldGui = new File(pluginFolder, "gui.yml");
        if (oldGui.exists()) oldGui.delete();

        File oldPoints = new File(pluginFolder, "points.yml");
        if (oldPoints.exists()) oldPoints.delete();

        // Initialize managers and start auto spawn task
        this.pointsManager = new PointsManager(this);
        this.crateManager = new CrateManager(this);
        crateManager.startAutoSpawnTask();

        // Initialize ShopGUI instance (required before listeners and tasks)
        this.shopGUI = new ShopGUI(this);
        this.regionWandListener = new RegionWandListener(this);

        // Start repeating tasks (Auto-Save and Shop Update)
        startPointsAutoSaveTask();
        startShopUpdateTask(); // Task Updated Frequency

        // Register all command executors and tab completers
        getCommand("qeventbox").setExecutor(new QEventBoxCommand(this));
        getCommand("qeventbox").setTabCompleter(new QEventBoxCommand(this));
        getCommand("eventshop").setExecutor(new EventShopCommand(this));
        getCommand("eventshop").setTabCompleter(new EventShopCommand(this));
        getCommand("qpoints").setExecutor(new PointsCommand(this));
        getCommand("qpoints").setTabCompleter(new PointsCommand(this));
        RegionWandCommand wandExecutor = new RegionWandCommand(this, regionWandListener);
        getCommand("qwand").setExecutor(new RegionWandCommand(this, regionWandListener)); // Link ke listener
        getCommand("qwand").setTabCompleter(new RegionWandCommand(this, regionWandListener)); // Link ke listener


        // Register event listeners
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(regionWandListener, this);


        getLogger().info("[QEventBox] Enabled");
    }

    // Task for Auto-Saving points data periodically
    private void startPointsAutoSaveTask() {
        // Save points asynchronously every 5 minutes (6000 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pointsManager != null) {
                    pointsManager.saveAsync();
                    getLogger().info("[QEventBox] Points data auto-saved.");
                }
            }
        }.runTaskTimerAsynchronously(this, 6000L, 6000L);
    }

    // Task for updating the Shop GUI in real-time (Crate Status & Points)
    private void startShopUpdateTask() {
        // Run synchronous task every 100 ticks (5 seconds) to reduce load
        Bukkit.getScheduler().runTaskTimer(this, () -> {

            if (shopGUI == null) return;

            // Iterate through open inventories to refresh content
            for (UUID playerUUID : new ArrayList<>(shopGUI.getOpenInventories().keySet())) {
                Player p = Bukkit.getPlayer(playerUUID);

                // Check player status and inventory integrity before updating
                if (p != null && p.isOnline()) {
                    if (p.getOpenInventory().getTopInventory().equals(shopGUI.getOpenInventories().get(playerUUID))) {
                        // Refresh display of points and crate status placeholders
                        shopGUI.updatePointsDisplay(p);
                    } else {
                        // Clean up map if player closed inventory without triggering the CloseEvent
                        shopGUI.getOpenInventories().remove(playerUUID);
                    }
                } else {
                    // Clean up map if player logged out
                    shopGUI.getOpenInventories().remove(playerUUID);
                }
            }
        }, 0L, 100L); // **INTERVAL DIUBAH MENJADI 100 TICKS (5 DETIK)**
    }

    // Utility method to copy resource files from JAR to target folder
    private void copyResourceToFolder(String resourceName, File targetFile) {
        if (!targetFile.exists()) {
            saveResource(resourceName, false);
            File temp = new File(getDataFolder(), resourceName);
            temp.renameTo(targetFile);
        }
    }

    @Override
    public void onDisable() {
        // Cancel all running Bukkit tasks
        getServer().getScheduler().cancelTasks(this);

        // Perform cleanup and DATA SAVING (MUST BE SYNCHRONOUS AND SAVE ALL HERE)
        if (crateManager != null) crateManager.cleanupAll();
        if (pointsManager != null) pointsManager.saveAll();

        getLogger().info("[QEventBox] Disabled");
    }

    // Getters for Managers and Instance
    public static QEventBox getInstance() { return instance; }
    public CrateManager getCrateManager() { return crateManager; }
    public PointsManager getPointsManager() { return pointsManager; }
    public ShopGUI getShopGUI() { return shopGUI; }
}