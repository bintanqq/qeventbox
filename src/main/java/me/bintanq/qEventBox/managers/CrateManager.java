package me.bintanq.qEventBox.managers;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.bintanq.qEventBox.QEventBox;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CrateManager {

    private final QEventBox plugin;
    private final Map<UUID, CrateData> activeCrates = new ConcurrentHashMap<>();

    // Flags untuk autospawn broadcast
    private boolean autoStartBroadcasted = false;
    private boolean autoHalfBroadcasted = false;
    private boolean autoExpiredBroadcasted = false;

    public CrateManager(QEventBox plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, CrateData> getActiveCrates() {
        return activeCrates;
    }

    /* -------------------------
       Reflection / Texture Helpers
       ------------------------- */
    private boolean injectProfileReflectively(Object target, GameProfile profile) {
        if (target == null || profile == null) return false;
        try {
            Class<?> clazz = target.getClass();
            Field field = null;
            try { field = clazz.getDeclaredField("profile"); } catch (NoSuchFieldException ignored) {}
            if (field == null) {
                for (Field f : clazz.getDeclaredFields()) {
                    String name = f.getName().toLowerCase(Locale.ROOT);
                    if (name.contains("profile") || name.contains("gameprofile") || name.contains("owner")) {
                        field = f;
                        break;
                    }
                }
            }
            if (field == null) {
                plugin.getLogger().fine("[CrateManager] No profile-like field found in " + clazz.getName());
                return false;
            }
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            if (fieldType.isAssignableFrom(GameProfile.class)) {
                field.set(target, profile);
                return true;
            }
            try {
                Constructor<?> ctor = fieldType.getDeclaredConstructor(GameProfile.class);
                ctor.setAccessible(true);
                Object wrapperInstance = ctor.newInstance(profile);
                field.set(target, wrapperInstance);
                return true;
            } catch (NoSuchMethodException ignored) {}
            for (Constructor<?> c : fieldType.getDeclaredConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 1 && params[0].isAssignableFrom(GameProfile.class)) {
                    c.setAccessible(true);
                    Object instance = c.newInstance(profile);
                    field.set(target, instance);
                    return true;
                }
            }
            plugin.getLogger().fine("[CrateManager] Field type " + fieldType.getName() +
                    " not constructible from GameProfile for " + clazz.getName());
            return false;
        } catch (Throwable t) {
            plugin.getLogger().warning("[CrateManager] injectProfileReflectively failed: " + t.getMessage());
            return false;
        }
    }

    private GameProfile makeProfileFromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "QEventBoxHead");
            profile.getProperties().put("textures", new Property("textures", base64));
            return profile;
        } catch (Throwable t) {
            plugin.getLogger().warning("[CrateManager] makeProfileFromBase64 failed: " + t.getMessage());
            return null;
        }
    }

    private boolean applyTextureToMeta(SkullMeta meta, String base64) {
        if (meta == null || base64 == null || base64.isEmpty()) return false;
        try {
            GameProfile profile = makeProfileFromBase64(base64);
            if (profile == null) return false;
            return injectProfileReflectively(meta, profile);
        } catch (Throwable t) {
            plugin.getLogger().warning("[CrateManager] applyTextureToMeta error: " + t.getMessage());
            return false;
        }
    }

    private boolean applyTextureToBlock(Block block, String base64) {
        if (block == null || base64 == null || base64.isEmpty()) return false;
        BlockState state = block.getState();
        if (state == null) return false;
        try {
            GameProfile profile = makeProfileFromBase64(base64);
            if (profile == null) return false;
            boolean ok = injectProfileReflectively(state, profile);
            if (ok) {
                try { state.update(true, false); } catch (Throwable ignored) {
                    try { state.update(); } catch (Throwable ex) {}
                }
            }
            return ok;
        } catch (Throwable t) {
            plugin.getLogger().warning("[CrateManager] applyTextureToBlock error: " + t.getMessage());
            return false;
        }
    }

    /* -------------------------
       AutoSpawn Task
       ------------------------- */
    public void manualAutoSpawnOnce() {
        int amount = plugin.getConfig().getInt("crate.amount", 1);

        // 1x broadcast seperti auto spawn
        String msg = plugin.getConfig().getString("messages.auto",
                        "§e§l[QEventBox] AutoSpawn start in %world%")
                .replace("%world%", plugin.getConfig().getString("region.world", "world"));
        broadcast(msg);

        // Spawn crates sesuai config
        for (int i = 0; i < amount; i++) {
            spawnCrateAtAuto();
        }
    }

    public void startAutoSpawnTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                LocalTime now = LocalTime.now();
                List<String> spawnTimes = plugin.getConfig().getStringList("crate.spawn-times");
                int amount = plugin.getConfig().getInt("crate.amount", 1);

                for (String t : spawnTimes) {
                    try {
                        String[] split = t.split(":");
                        LocalTime spawnTime = LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));

                        if (now.getHour() == spawnTime.getHour() && now.getMinute() == spawnTime.getMinute()) {

                            // 1x broadcast auto start
                            if (!autoStartBroadcasted) {
                                String msg = plugin.getConfig().getString("messages.auto",
                                                "§e§l[QEventBox] AutoSpawn start in %world%")
                                        .replace("%world%", plugin.getConfig().getString("region.world", "world"));
                                broadcast(msg);
                                autoStartBroadcasted = true;
                            }

                            // Spawn crate sesuai amount
                            for (int i = 0; i < amount; i++) spawnCrateAtAuto();

                            // Reset flags setelah 1 menit supaya bisa spawn lagi next time
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                autoStartBroadcasted = false;
                                autoHalfBroadcasted = false;
                                autoExpiredBroadcasted = false;
                            }, 20L * 60);

                            break; // sudah spawn untuk waktu ini, stop loop
                        }
                    } catch (Exception ignored) {}
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 60); // cek tiap 1 menit
    }

    // Spawn crate khusus auto tanpa broadcast manual per crate
    public void spawnCrateAtAuto() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("region.world", "world"));
        if (world == null) return;

        int minX = plugin.getConfig().getInt("region.min-x", -500);
        int maxX = plugin.getConfig().getInt("region.max-x", 500);
        int minZ = plugin.getConfig().getInt("region.min-z", -500);
        int maxZ = plugin.getConfig().getInt("region.max-z", 500);
        int minY = plugin.getConfig().getInt("region.min-y", 60);

        Random rand = new Random();
        for (int i = 0; i < 50; i++) {
            int x = rand.nextInt(maxX - minX + 1) + minX;
            int z = rand.nextInt(maxZ - minZ + 1) + minZ;
            Location top = world.getHighestBlockAt(x, z).getLocation();
            if (top.getBlockY() < minY) continue;

            Location place = top.clone().add(0, 1, 0);
            if (!place.getBlock().getType().isAir()) continue;

            // SET BLOCK & TEXTURE
            Block block = place.getBlock();
            block.setType(Material.PLAYER_HEAD);

            ItemStack skullItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skullItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§bEvent Crate");
                String texture = plugin.getConfig().getString("crate.texture", "");
                if (texture != null && !texture.isEmpty()) applyTextureToMeta(meta, texture);
                skullItem.setItemMeta(meta);
            }

            String texture = plugin.getConfig().getString("crate.texture", "");
            if (texture != null && !texture.isEmpty()) applyTextureToBlock(block, texture);

            // Simpan CrateData
            UUID crateId = UUID.randomUUID();
            int lifetime = plugin.getConfig().getInt("crate.lifetime-seconds", 300);
            CrateData data = new CrateData(crateId, place, lifetime, true); // true = auto spawn
            activeCrates.put(crateId, data);
            data.startTimer();
            break; // spawn 1 crate per loop
        }
    }

    /* -------------------------
       Crate Spawn Manual
       ------------------------- */
    public UUID spawnRandomCrate() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("region.world", "world"));
        if (world == null) return null;

        int minX = plugin.getConfig().getInt("region.min-x", -500);
        int maxX = plugin.getConfig().getInt("region.max-x", 500);
        int minZ = plugin.getConfig().getInt("region.min-z", -500);
        int maxZ = plugin.getConfig().getInt("region.max-z", 500);
        int minY = plugin.getConfig().getInt("region.min-y", 60);

        Random rand = new Random();
        for (int i = 0; i < 50; i++) {
            int x = rand.nextInt(maxX - minX + 1) + minX;
            int z = rand.nextInt(maxZ - minZ + 1) + minZ;
            Location top = world.getHighestBlockAt(x, z).getLocation();
            if (top.getBlockY() < minY) continue;

            Location place = top.clone().add(0, 1, 0);
            if (!place.getBlock().getType().isAir()) continue;

            return spawnCrateAt(place);
        }

        return null;
    }

    public UUID spawnCrateAt(Location loc) {
        if (loc == null) return null;

        Block block = loc.getBlock();
        block.setType(Material.PLAYER_HEAD);

        ItemStack skullItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skullItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§bEvent Crate");
            String texture = plugin.getConfig().getString("crate.texture", "");
            if (texture != null && !texture.isEmpty()) applyTextureToMeta(meta, texture);
            skullItem.setItemMeta(meta);
        }

        String texture = plugin.getConfig().getString("crate.texture", "");
        if (texture != null && !texture.isEmpty()) applyTextureToBlock(block, texture);

        UUID crateId = UUID.randomUUID();
        int lifetime = plugin.getConfig().getInt("crate.lifetime-seconds", 300);
        CrateData data = new CrateData(crateId, loc, lifetime, false); // false = manual spawn
        activeCrates.put(crateId, data);

        broadcast("§e§l[QEventBox] §aCrate spawned in " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
        data.startTimer();
        return crateId;
    }

    /* -------------------------
       Crate Management
       ------------------------- */
    public Optional<CrateData> getCrateByBlock(Block b) {
        if (b == null) return Optional.empty();
        return activeCrates.values().stream()
                .filter(c -> c.getLocation() != null && c.getLocation().getBlock().equals(b))
                .findFirst();
    }

    public void claimCrate(UUID crateId, Player player) {
        CrateData data = activeCrates.get(crateId);
        if (data == null) return;
        handleRewards(player);

        if (!data.isAutoSpawn()) broadcast("§e§l[QEventBox] §b" + player.getName() + " §7claimed the box!");

        data.cancel();
        activeCrates.remove(crateId);
        if (data.getLocation().getBlock().getType() == Material.PLAYER_HEAD)
            data.getLocation().getBlock().setType(Material.AIR);
    }

    private void handleRewards(Player player) {
        List<String> rewards = plugin.getConfig().getStringList("rewards");
        for (String r : rewards) {
            r = r.trim();
            if (r.startsWith("eventpoints:")) {
                try {
                    int amt = Integer.parseInt(r.split(":", 2)[1]);
                    plugin.getPointsManager().addPoints(player.getUniqueId(), amt);
                    player.sendMessage("§e§l[QEventBox] §a+" + amt + " Event Points");
                } catch (Exception ignored) {}
            } else if (r.startsWith("command:")) {
                String cmd = r.split(":", 2)[1].replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else if (r.startsWith("item:")) {
                try {
                    String[] parts = r.split(":");
                    Material mat = Material.getMaterial(parts[1]);
                    int qty = Integer.parseInt(parts[2]);
                    if (mat != null) player.getInventory().addItem(new ItemStack(mat, qty));
                } catch (Exception ignored) {}
            }
        }
    }

    public void removeCrate(UUID crateId) {
        CrateData data = activeCrates.remove(crateId);
        if (data != null) {
            data.cancel();
            if (data.getLocation().getBlock().getType() == Material.PLAYER_HEAD)
                data.getLocation().getBlock().setType(Material.AIR);
        }
    }

    public void cleanupAll() {
        for (UUID id : new ArrayList<>(activeCrates.keySet()))
            removeCrate(id);
        activeCrates.clear();
    }

    public String getActiveCratesStatus() {
        if (!activeCrates.isEmpty()) return "Has spawned!";
        List<String> spawnTimes = plugin.getConfig().getStringList("crate.spawn-times");
        if (spawnTimes.isEmpty()) return "Not spawned.";

        LocalTime now = LocalTime.now();
        LocalTime nextSpawn = null;

        for (String t : spawnTimes) {
            String[] split = t.split(":");
            LocalTime spawnTime = LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
            if (spawnTime.isAfter(now)) {
                nextSpawn = spawnTime;
                break;
            }
        }

        if (nextSpawn == null) {
            String first = spawnTimes.get(0);
            String[] split = first.split(":");
            nextSpawn = LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        }

        Duration duration = Duration.between(now, nextSpawn);
        if (duration.isNegative()) duration = duration.plusDays(1);

        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;

        return "Next Box: " + hours + " hours " + minutes + " minute";
    }

    private void broadcast(String msg) {
        Bukkit.broadcastMessage(msg);
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(),
                "minecraft:entity.experience_orb.pickup", 1f, 1f));
    }

    /* -------------------------
       Inner CrateData
       ------------------------- */
    public class CrateData {
        private final UUID id;
        private final Location location;
        private final int lifetimeSeconds;
        private int ticksLeft;
        private BukkitRunnable task;
        private boolean halfBroadcasted = false;
        private final boolean autoSpawn;

        public CrateData(UUID id, Location location, int lifetimeSeconds, boolean autoSpawn) {
            this.id = id;
            this.location = location;
            this.lifetimeSeconds = lifetimeSeconds;
            this.ticksLeft = lifetimeSeconds;
            this.autoSpawn = autoSpawn;
        }

        public UUID getId() { return id; }
        public Location getLocation() { return location; }
        public boolean isAutoSpawn() { return autoSpawn; }

        public void startTimer() {
            final int half = lifetimeSeconds / 2;
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    ticksLeft--;

                    // Half broadcast
                    if (autoSpawn && !autoHalfBroadcasted && ticksLeft <= half) {
                        autoHalfBroadcasted = true;
                        broadcast("§e§l[QEventBox] §6Auto Box will dissapeared in " + ticksLeft + " seconds!");
                    } else if (!autoSpawn && !halfBroadcasted && ticksLeft <= half) {
                        halfBroadcasted = true;
                        broadcast("§e§l[QEventBox] §6Box will be dissapeared in " + ticksLeft + " seconds!");
                    }

                    // Expired broadcast
                    if (autoSpawn && !autoExpiredBroadcasted && ticksLeft <= 0) {
                        autoExpiredBroadcasted = true;
                        broadcast("§e§l[QEventBox] §cAuto Box dissapeared!");
                    } else if (!autoSpawn && ticksLeft <= 0) {
                        broadcast("§e§l[QEventBox] §cBox dissapeared due to unclaimed!");
                    }

                    if (ticksLeft <= 0) {
                        if (location.getBlock().getType() == Material.PLAYER_HEAD)
                            location.getBlock().setType(Material.AIR);
                        cancel();
                        activeCrates.remove(id);
                    }
                }
            };
            task.runTaskTimer(plugin, 20L, 20L);
        }

        public void cancel() {
            if (task != null) task.cancel();
        }
    }
}
