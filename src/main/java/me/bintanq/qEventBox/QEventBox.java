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

        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists()) pluginFolder.mkdirs();

        File guiFolder = new File(pluginFolder, "gui");
        if (!guiFolder.exists()) guiFolder.mkdirs();

        File pointsFolder = new File(pluginFolder, "points");
        if (!pointsFolder.exists()) pointsFolder.mkdirs();

        copyResourceToFolder("gui.yml", new File(guiFolder, "gui.yml"));
        copyResourceToFolder("points.yml", new File(pointsFolder, "points.yml"));

        File oldGui = new File(pluginFolder, "gui.yml");
        if (oldGui.exists()) oldGui.delete();

        File oldPoints = new File(pluginFolder, "points.yml");
        if (oldPoints.exists()) oldPoints.delete();

        this.pointsManager = new PointsManager(this);
        this.crateManager = new CrateManager(this);
        crateManager.startAutoSpawnTask();

        this.shopGUI = new ShopGUI(this);
        this.regionWandListener = new RegionWandListener(this);

        startPointsAutoSaveTask();
        startShopUpdateTask();

        getCommand("qeventbox").setExecutor(new QEventBoxCommand(this));
        getCommand("qeventbox").setTabCompleter(new QEventBoxCommand(this));

        getCommand("eventshop").setExecutor(new EventShopCommand(this));
        getCommand("eventshop").setTabCompleter(new EventShopCommand(this));

        getCommand("qpoints").setExecutor(new PointsCommand(this));
        getCommand("qpoints").setTabCompleter(new PointsCommand(this));

        RegionWandCommand wandExecutor = new RegionWandCommand(this, regionWandListener);
        getCommand("qwand").setExecutor(wandExecutor);
        getCommand("qwand").setTabCompleter(wandExecutor);


        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(regionWandListener, this);


        getLogger().info("Enabled");
    }

    private void startPointsAutoSaveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pointsManager != null) {
                    pointsManager.saveAsync();
                    getLogger().info("Points data auto-saved.");
                }
            }
        }.runTaskTimerAsynchronously(this, 6000L, 6000L);
    }

    private void startShopUpdateTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {

            if (shopGUI == null) return;

            for (UUID playerUUID : new ArrayList<>(shopGUI.getOpenInventories().keySet())) {
                Player p = Bukkit.getPlayer(playerUUID);

                if (p != null && p.isOnline()) {
                    if (p.getOpenInventory().getTopInventory().equals(shopGUI.getOpenInventories().get(playerUUID))) {
                        shopGUI.updatePointsDisplay(p);
                    } else {
                        shopGUI.getOpenInventories().remove(playerUUID);
                    }
                } else {
                    shopGUI.getOpenInventories().remove(playerUUID);
                }
            }
        }, 0L, 100L);
    }

    private void copyResourceToFolder(String resourceName, File targetFile) {
        if (!targetFile.exists()) {
            saveResource(resourceName, false);
            File temp = new File(getDataFolder(), resourceName);
            temp.renameTo(targetFile);
        }
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);

        if (crateManager != null) crateManager.cleanupAll();
        if (pointsManager != null) pointsManager.saveAll();

        getLogger().info("Disabled");
    }

    public static QEventBox getInstance() { return instance; }
    public CrateManager getCrateManager() { return crateManager; }
    public PointsManager getPointsManager() { return pointsManager; }
    public ShopGUI getShopGUI() { return shopGUI; }
}