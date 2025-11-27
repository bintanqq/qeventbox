package me.bintanq.qEventBox;

import me.bintanq.qEventBox.listeners.ShopListener;
import org.bukkit.plugin.java.JavaPlugin;
import me.bintanq.qEventBox.managers.CrateManager;
import me.bintanq.qEventBox.managers.PointsManager;
import me.bintanq.qEventBox.commands.QEventBoxCommand;
import me.bintanq.qEventBox.commands.EventShopCommand;
import me.bintanq.qEventBox.commands.PointsCommand;
import me.bintanq.qEventBox.listeners.InteractListener;

import java.io.File;

public class QEventBox extends JavaPlugin {

    private static QEventBox instance;

    private CrateManager crateManager;
    private PointsManager pointsManager;

    @Override
    public void onLoad() { instance = this; }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // pastikan gui.yml ada
        File guiFile = new File(getDataFolder(), "gui.yml");
        if (!guiFile.exists()) saveResource("gui.yml", false);

        this.pointsManager = new PointsManager(this);
        this.crateManager = new CrateManager(this);

        getCommand("qeventbox").setExecutor(new QEventBoxCommand(this));
        getCommand("eventshop").setExecutor(new EventShopCommand(this));
        getCommand("qpoints").setExecutor(new PointsCommand(this));

        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);

        getLogger().info("[QEventBox] Enabled");
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
}