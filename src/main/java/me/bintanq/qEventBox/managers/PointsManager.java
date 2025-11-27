package me.bintanq.qEventBox.managers;

import me.bintanq.qEventBox.QEventBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

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

    public PointsManager(QEventBox plugin) {
        this.plugin = plugin;
        this.pointsFile = new File(plugin.getDataFolder(), "points/points.yml"); // <- folder points
        if (!pointsFile.exists()) plugin.saveResource("points.yml", false); // dari resources root
        this.pointsCfg = YamlConfiguration.loadConfiguration(pointsFile);
        loadAll();
    }

    public void loadAll() {
        for (String key : pointsCfg.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                points.put(id, pointsCfg.getInt(key + ".points", 0));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, Integer> e : points.entrySet()) {
            pointsCfg.set(e.getKey().toString() + ".points", e.getValue());
        }
        try { pointsCfg.save(pointsFile); } catch (IOException e) { plugin.getLogger().severe(e.getMessage()); }
    }

    public int getPoints(UUID uuid) { return points.getOrDefault(uuid, 0); }
    public void addPoints(UUID uuid, int amt) { points.put(uuid, getPoints(uuid) + amt); saveAll(); }
    public void removePoints(UUID uuid, int amt) { points.put(uuid, Math.max(0, getPoints(uuid) - amt)); saveAll(); }
    public void setPoints(UUID uuid, int amt) { points.put(uuid, Math.max(0, amt)); saveAll(); }
}
