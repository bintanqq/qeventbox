package me.bintanq.qEventBox.managers;

import me.bintanq.qEventBox.QEventBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PointsManager {
    private final QEventBox plugin;
    private final Map<UUID, Integer> points = new HashMap<>();
    private final File pointsFile;
    private final FileConfiguration pointsCfg;
    private boolean isSaving = false;

    public PointsManager(QEventBox plugin) {
        this.plugin = plugin;
        this.pointsFile = new File(plugin.getDataFolder(), "points/points.yml");

        if (!pointsFile.exists()) {
            if (!pointsFile.getParentFile().exists()) pointsFile.getParentFile().mkdirs();
        }

        this.pointsCfg = YamlConfiguration.loadConfiguration(pointsFile);
        loadAll();
    }

    public void loadAll() {
        for (String key : pointsCfg.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                points.put(id, pointsCfg.getInt(key, 0));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Invalid UUID found in points.yml: " + key);
            }
        }
    }

    public synchronized void saveAll() {
        if (isSaving) return;
        isSaving = true;

        for (Map.Entry<UUID, Integer> e : points.entrySet()) {
            pointsCfg.set(e.getKey().toString(), e.getValue());
        }
        try {
            pointsCfg.save(pointsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save points data: " + e.getMessage());
        } finally {
            isSaving = false;
        }
    }

    public void saveAsync() {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAll();
            }
        }.runTaskAsynchronously(plugin);
    }

    public synchronized void savePlayer(UUID uuid) {
        if (points.containsKey(uuid)) {
            pointsCfg.set(uuid.toString(), points.get(uuid));
            try {
                pointsCfg.save(pointsFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save points for " + uuid + ": " + e.getMessage());
            }
        }
    }

    public int getPoints(UUID uuid) { return points.getOrDefault(uuid, 0); }

    public void addPoints(UUID uuid, int amt) {
        points.put(uuid, getPoints(uuid) + amt);
    }

    public void removePoints(UUID uuid, int amt) {
        points.put(uuid, Math.max(0, getPoints(uuid) - amt));
    }

    public void setPoints(UUID uuid, int amt) {
        points.put(uuid, Math.max(0, amt));
    }
}