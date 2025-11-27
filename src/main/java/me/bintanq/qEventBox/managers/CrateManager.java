package me.bintanq.qEventBox.managers;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.lang.reflect.Field;
import java.util.UUID;
import me.bintanq.qEventBox.QEventBox;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CrateManager {
    private final QEventBox plugin;
    private final Map<UUID, CrateData> activeCrates = new ConcurrentHashMap<>();

    public Map<UUID, CrateData> getActiveCrates() {
        return activeCrates;
    }

    public CrateManager(QEventBox plugin) { this.plugin = plugin; }

    private void applyTexture(SkullMeta meta, String base64) {
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", base64));

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Exception e) {
            plugin.getLogger().warning("[QEventBox] Failed to apply head texture: " + e.getMessage());
        }
    }

    public UUID spawnRandomCrate() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("region.world", "world"));
        if (world == null) return null;

        int minX = plugin.getConfig().getInt("region.min-x", -500);
        int maxX = plugin.getConfig().getInt("region.max-x", 500);
        int minZ = plugin.getConfig().getInt("region.min-z", -500);
        int maxZ = plugin.getConfig().getInt("region.max-z", 500);
        int minY = plugin.getConfig().getInt("region.min-y", 60);
        int attempts = 50;

        Random rand = new Random();
        for (int i = 0; i < attempts; i++) {
            int x = rand.nextInt(maxX - minX + 1) + minX;
            int z = rand.nextInt(maxZ - minZ + 1) + minZ;
            Location top = world.getHighestBlockAt(x, z).getLocation();
            if (top.getBlockY() < minY) continue;
            Location place = top.clone().add(0,1,0);
            if (!place.getBlock().getType().isAir()) continue;
            return spawnCrateAt(place);
        }
        return null;
    }

    public UUID spawnCrateAt(Location loc) {
        if (loc == null) return null;

        loc.getBlock().setType(Material.PLAYER_HEAD);
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if(meta != null) {
            meta.setDisplayName("§bEvent Crate");

            // Ambil texture dari config.yml
            String texture = plugin.getConfig().getString("crate.texture", "");
            if (!texture.isEmpty()) applyTexture(meta, texture);

            skull.setItemMeta(meta);
        }

        UUID crateId = UUID.randomUUID();
        int lifetime = plugin.getConfig().getInt("crate.lifetime-seconds", 300);
        CrateData data = new CrateData(crateId, loc, lifetime);
        activeCrates.put(crateId, data);
        broadcast("[QEventBox] §aCrate muncul di " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
        data.startTimer();
        return crateId;
    }

    public Optional<CrateData> getCrateByBlock(Block b) {
        return activeCrates.values().stream().filter(c -> c.getLocation().getBlock().equals(b)).findFirst();
    }

    public void claimCrate(UUID crateId, Player player) {
        CrateData data = activeCrates.get(crateId);
        if (data == null) return;
        handleRewards(player);
        broadcast("[QEventBox] §b" + player.getName() + " §7mengklaim crate!");
        data.cancel();
        activeCrates.remove(crateId);
        if (data.getLocation().getBlock().getType() == Material.PLAYER_HEAD) data.getLocation().getBlock().setType(Material.AIR);
    }

    private void handleRewards(Player player) {
        List<String> rewards = plugin.getConfig().getStringList("rewards");
        for (String r : rewards) {
            r = r.trim();
            if (r.startsWith("eventpoints:")) {
                int amt = Integer.parseInt(r.split(":",2)[1]);
                plugin.getPointsManager().addPoints(player.getUniqueId(), amt);
                player.sendMessage("[QEventBox] §a+" + amt + " Event Points");
            } else if (r.startsWith("command:")) {
                String cmd = r.split(":",2)[1].replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else if (r.startsWith("item:")) {
                String[] parts = r.split(":");
                Material mat = Material.getMaterial(parts[1]);
                int qty = Integer.parseInt(parts[2]);
                if (mat != null) player.getInventory().addItem(new ItemStack(mat, qty));
            }
        }
    }

    public void removeCrate(UUID crateId) {
        CrateData data = activeCrates.remove(crateId);
        if (data != null) {
            data.cancel();
            if (data.getLocation().getBlock().getType() == Material.PLAYER_HEAD) data.getLocation().getBlock().setType(Material.AIR);
        }
    }

    public void cleanupAll() {
        for (UUID id : new ArrayList<>(activeCrates.keySet())) removeCrate(id);
        activeCrates.clear();
    }

    public String getActiveCratesStatus() {
        if (activeCrates.isEmpty()) return "Belum spawn. Berikutnya: " + "??:??";
        return "Sudah spawn!";
    }

    private void broadcast(String msg) {
        Bukkit.broadcastMessage(msg);
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), "minecraft:entity.experience_orb.pickup",1f,1f));
    }

    public class CrateData {
        private final UUID id;
        private final Location location;
        private final int lifetimeSeconds;
        private int ticksLeft;
        private BukkitRunnable task;
        private boolean halfBroadcasted = false;

        public CrateData(UUID id, Location location, int lifetimeSeconds) {
            this.id = id; this.location = location; this.lifetimeSeconds = lifetimeSeconds; this.ticksLeft = lifetimeSeconds;
        }
        public UUID getId() { return id; }
        public Location getLocation() { return location; }

        public void startTimer() {
            final int half = lifetimeSeconds/2;
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    ticksLeft--;
                    if (!halfBroadcasted && ticksLeft <= half) {
                        halfBroadcasted = true;
                        broadcast("[QEventBox] §6Crate akan hilang dalam " + ticksLeft + " detik!");
                    }
                    if (ticksLeft <= 0) {
                        broadcast("[QEventBox] §cCrate hilang karena tidak diklaim!");
                        if (location.getBlock().getType() == Material.PLAYER_HEAD) location.getBlock().setType(Material.AIR);
                        cancel();
                        activeCrates.remove(id);
                    }
                }
            };
            task.runTaskTimer(plugin,20L,20L);
        }
        public void cancel() { if (task!=null) task.cancel(); }
    }
}