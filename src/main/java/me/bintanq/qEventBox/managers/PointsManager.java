package me.bintanq.qEventBox.managers;

import me.bintanq.qEventBox.QEventBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable; // Import baru

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PointsManager {
    private final QEventBox plugin;
    // Map to store player UUID and their points in memory
    private final Map<UUID, Integer> points = new HashMap<>();
    private final File pointsFile;
    private final FileConfiguration pointsCfg;

    // Flag to prevent concurrent writes to the file
    private boolean isSaving = false;

    public PointsManager(QEventBox plugin) {
        this.plugin = plugin;
        this.pointsFile = new File(plugin.getDataFolder(), "points/points.yml");

        if (!pointsFile.exists()) {
            // Pastikan folder dibuat sebelum mencoba saveResource (jika diperlukan)
            if (!pointsFile.getParentFile().exists()) pointsFile.getParentFile().mkdirs();
            // Asumsi file ini akan dibuat/disimpan oleh main class
        }

        this.pointsCfg = YamlConfiguration.loadConfiguration(pointsFile);
        loadAll();
    }

    // Loads all point data from points.yml into the in-memory map
    public void loadAll() {
        for (String key : pointsCfg.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                points.put(id, pointsCfg.getInt(key + ".points", 0));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    // Saves ALL point data from the in-memory map back to points.yml (Synchronous & Thread-Safe)
    public synchronized void saveAll() {
        if (isSaving) return;
        isSaving = true;

        for (Map.Entry<UUID, Integer> e : points.entrySet()) {
            pointsCfg.set(e.getKey().toString() + ".points", e.getValue());
        }
        try {
            pointsCfg.save(pointsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save points data: " + e.getMessage());
        } finally {
            isSaving = false;
        }
    }

    // Runs saveAll() in an asynchronous thread to avoid blocking the main thread (for Auto-Save)
    public void saveAsync() {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAll();
            }
        }.runTaskAsynchronously(plugin);
    }

    // Saves data for a single player (efficient for logout)
    public synchronized void savePlayer(UUID uuid) {
        if (points.containsKey(uuid)) {
            pointsCfg.set(uuid.toString() + ".points", points.get(uuid));
            try {
                pointsCfg.save(pointsFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save points for " + uuid + ": " + e.getMessage());
            }
        }
    }

    // Retrieves player's points, defaulting to 0
    public int getPoints(UUID uuid) { return points.getOrDefault(uuid, 0); }

    // Adds the specified amount of points (removed saveAll())
    public void addPoints(UUID uuid, int amt) {
        points.put(uuid, getPoints(uuid) + amt);
    }

    // Removes the specified amount of points (minimum 0) (removed saveAll())
    public void removePoints(UUID uuid, int amt) {
        points.put(uuid, Math.max(0, getPoints(uuid) - amt));
    }

    // Sets the player's points (minimum 0) (removed saveAll())
    public void setPoints(UUID uuid, int amt) {
        points.put(uuid, Math.max(0, amt));
    }
}