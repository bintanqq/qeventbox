package me.bintanq.qEventBox;

import me.bintanq.qEventBox.gui.ShopGUI;
import me.bintanq.qEventBox.listeners.ShopListener;
import me.bintanq.qEventBox.listeners.InteractListener;
import me.bintanq.qEventBox.managers.CrateManager;
import me.bintanq.qEventBox.managers.PointsManager;
import me.bintanq.qEventBox.commands.QEventBoxCommand;
import me.bintanq.qEventBox.commands.EventShopCommand;
import me.bintanq.qEventBox.commands.PointsCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class QEventBox extends JavaPlugin {

    private static QEventBox instance;

    private CrateManager crateManager;
    private PointsManager pointsManager;
    private ShopGUI shopGUI;

    @Override
    public void onLoad() { instance = this; }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Setup folder gui dan points
        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists()) pluginFolder.mkdirs();

        File guiFolder = new File(pluginFolder, "gui");
        if (!guiFolder.exists()) guiFolder.mkdirs();

        File pointsFolder = new File(pluginFolder, "points");
        if (!pointsFolder.exists()) pointsFolder.mkdirs();

        // Copy gui.yml
        copyResourceToFolder("gui.yml", new File(guiFolder, "gui.yml"));

        // Copy points.yml
        copyResourceToFolder("points.yml", new File(pointsFolder, "points.yml"));

        // Hapus file lama di root plugin folder
        File oldGui = new File(pluginFolder, "gui.yml");
        if (oldGui.exists()) oldGui.delete();

        File oldPoints = new File(pluginFolder, "points.yml");
        if (oldPoints.exists()) oldPoints.delete();

        // Init managers
        this.pointsManager = new PointsManager(this);
        this.crateManager = new CrateManager(this);
        crateManager.startAutoSpawnTask(); // mulai auto spawn


        // Register commands & listeners
        getCommand("qeventbox").setExecutor(new QEventBoxCommand(this));
        getCommand("qeventbox").setTabCompleter(new QEventBoxCommand(this));
        getCommand("eventshop").setExecutor(new EventShopCommand(this));
        getCommand("eventshop").setTabCompleter(new EventShopCommand(this));
        getCommand("qpoints").setExecutor(new PointsCommand(this));
        getCommand("qpoints").setTabCompleter(new PointsCommand(this));


        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);

        getLogger().info("[QEventBox] Enabled");
    }

    // Method untuk copy resource dari jar ke folder target
    private void copyResourceToFolder(String resourceName, File targetFile) {
        if (!targetFile.exists()) {
            saveResource(resourceName, false); // simpan sementara di root
            File temp = new File(getDataFolder(), resourceName);
            temp.renameTo(targetFile); // pindahkan ke folder
        }
    }

    @Override
    public void onDisable() {
        if (crateManager != null) crateManager.cleanupAll();
        if (pointsManager != null) pointsManager.saveAll();
        getLogger().info("[QEventBox] Disabled");
    }

    public static QEventBox getInstance() { return instance; }
    public CrateManager getCrateManager() { return crateManager; }
    public PointsManager getPointsManager() { return pointsManager; }
    public ShopGUI getShopGUI() { return shopGUI; }
}
