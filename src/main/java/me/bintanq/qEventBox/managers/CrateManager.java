package me.bintanq.qEventBox.managers;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.bintanq.qEventBox.QEventBox;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
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
import java.util.concurrent.ThreadLocalRandom;

public class CrateManager {

    private final QEventBox plugin;
    private final Map<UUID, CrateData> activeCrates = new ConcurrentHashMap<>();
    private final Set<String> worldsSpawnedIn = ConcurrentHashMap.newKeySet();

    private boolean autoStartBroadcasted = false;
    private boolean autoHalfBroadcasted = false;
    private boolean autoExpiredBroadcasted = false;

    public CrateManager(QEventBox plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, CrateData> getActiveCrates() {
        return activeCrates;
    }

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
            return false;
        } catch (Throwable t) {
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
            return null;
        }
    }

    public boolean applyTextureToMeta(SkullMeta meta, String base64) {
        if (meta == null || base64 == null || base64.isEmpty()) return false;
        try {
            GameProfile profile = makeProfileFromBase64(base64);
            if (profile == null) return false;
            return injectProfileReflectively(meta, profile);
        } catch (Throwable t) {
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
            return false;
        }
    }

    public void manualAutoSpawnOnce() {
        FileConfiguration config = plugin.getConfig();
        int amount = config.getInt("crate.amount", 1);

        int successfulSpawns = 0;
        worldsSpawnedIn.clear();

        for (int i = 0; i < amount; i++) {
            UUID id = spawnRandomCrate();
            if (id != null) {
                successfulSpawns++;
                CrateData data = activeCrates.get(id);
                if (data != null && data.getLocation() != null) {
                    worldsSpawnedIn.add(data.getLocation().getWorld().getName());
                }
            }
        }

        if (successfulSpawns > 0) {
            String worldList = String.join(", ", worldsSpawnedIn);
            String msg = config.getString("messages.auto-spawn-broadcast", "&aEvent Box has spawned in %world%");
            broadcast(msg.replace("%world%", worldList));
        }

        if (successfulSpawns == 0) {
            plugin.getLogger().warning("[QEventBox] Failed to find safe spawn locations for any crate!");
        }
    }

    public void startAutoSpawnTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                LocalTime now = LocalTime.now();
                List<String> spawnTimes = plugin.getConfig().getStringList("crate.spawn-times");

                for (String t : spawnTimes) {
                    try {
                        String[] split = t.split(":");
                        LocalTime spawnTime = LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));

                        if (now.getHour() == spawnTime.getHour() && now.getMinute() == spawnTime.getMinute()) {

                            if (!autoStartBroadcasted) {
                                autoStartBroadcasted = true;
                                manualAutoSpawnOnce();
                            }

                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                autoStartBroadcasted = false;
                                autoHalfBroadcasted = false;
                                autoExpiredBroadcasted = false;
                            }, 20L * 60);

                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 60);
    }

    public UUID spawnRandomCrate() {
        FileConfiguration config = plugin.getConfig();
        List<String> allowedWorlds = config.getStringList("region.world");
        if (allowedWorlds.isEmpty()) return null;

        String worldName = allowedWorlds.get(ThreadLocalRandom.current().nextInt(allowedWorlds.size()));
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        int minX = config.getInt("region.min-x", -500);
        int maxX = config.getInt("region.max-x", 500);
        int minZ = config.getInt("region.min-z", -500);
        int maxZ = config.getInt("region.max-z", 500);

        int minY = config.getInt("region.min-y", 60);
        int maxY = config.getInt("region.max-y", 256);

        Random rand = ThreadLocalRandom.current();

        for (int i = 0; i < 50; i++) {
            int x = rand.nextInt(maxX - minX + 1) + minX;
            int z = rand.nextInt(maxZ - minZ + 1) + minZ;
            for (int y = minY; y < maxY - 2; y++) {

                Block floor = world.getBlockAt(x, y, z);
                Block crateSpot = world.getBlockAt(x, y + 1, z);
                Block airAbove = world.getBlockAt(x, y + 2, z);

                boolean isFloorSolid = floor.getType().isSolid();
                boolean isSpaceAir = crateSpot.getType().isAir() && airAbove.getType().isAir();

                if (isFloorSolid && isSpaceAir) {
                    Location place = new Location(world, x, y + 1, z);
                    return spawnCrateAt(place, true);
                }
            }
        }

        return null;
    }

    public UUID spawnCrateAt(Location loc) {
        return spawnCrateAt(loc, false);
    }

    public UUID spawnCrateAt(Location loc, boolean isAutoSpawn) {
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

        CrateData data = new CrateData(crateId, loc, lifetime, isAutoSpawn);
        activeCrates.put(crateId, data);

        if (!isAutoSpawn) {
            String locationString = loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
            String msg = plugin.getConfig().getString("messages.manual-spawn-broadcast", "&aCrate spawned at %location%!");
            broadcast(msg.replace("%location%", locationString));
        }

        data.startTimer();
        return crateId;
    }

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

        String claimedSuccessMsg = plugin.getConfig().getString("messages.claim-success", "&aYou successfully claimed the crate!");
        sendPlayerMessage(player, claimedSuccessMsg);

        if (!data.isAutoSpawn()) {
            String msg = plugin.getConfig().getString("messages.claimed-broadcast", "&b%player% &7claimed the box!");
            broadcast(msg.replace("%player%", player.getName()));
        }

        data.cancel();
        activeCrates.remove(crateId);
        if (data.getLocation().getBlock().getType() == Material.PLAYER_HEAD)
            data.getLocation().getBlock().setType(Material.AIR);
    }

    private void logWarning(String entry, String issue) {
        plugin.getLogger().warning("==========================================================");
        plugin.getLogger().warning("[QEventBox] CONFIG WARNING: " + issue);
        plugin.getLogger().warning("Entry causing issue: '" + entry + "'");
        plugin.getLogger().warning("New Format Required: 'CHANCE%|REWARD:DATA' (e.g., '50%|item:DIAMOND:1')");
        plugin.getLogger().warning("==========================================================");
    }

    private void handleRewards(Player player) {
        List<String> rewards = plugin.getConfig().getStringList("rewards");
        Random random = ThreadLocalRandom.current();

        for (String entry : rewards) {
            entry = entry.trim();

            double chance = 100.0;
            String rewardString = entry;

            if (entry.contains("%|")) {
                try {
                    String[] parts = entry.split("%\\|", 2);
                    chance = Double.parseDouble(parts[0].trim());
                    rewardString = parts[1].trim();
                } catch (Exception e) {
                    logWarning(entry, "Invalid chance value in the new reward format. Skipping this reward.");
                    continue;
                }
            } else {
                logWarning(entry, "Deprecated reward format detected! Please update this entry to the new format.");
            }

            if (random.nextDouble() * 100.0 > chance) {
                continue;
            }

            if (rewardString.startsWith("eventpoints:")) {
                try {
                    int amt = Integer.parseInt(rewardString.split(":", 2)[1]);
                    plugin.getPointsManager().addPoints(player.getUniqueId(), amt);

                    String msg = plugin.getConfig().getString("messages.points-reward", "&a+%amount% Event Points");
                    sendPlayerMessage(player, msg.replace("%amount%", String.valueOf(amt)));
                } catch (Exception ignored) {}
            } else if (rewardString.startsWith("command:")) {
                String cmd = rewardString.split(":", 2)[1].replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else if (rewardString.startsWith("item:")) {
                try {
                    String[] parts = rewardString.split(":");
                    Material mat = Material.getMaterial(parts[1].toUpperCase());
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
            try {
                LocalTime spawnTime = LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
                if (spawnTime.isAfter(now)) {
                    nextSpawn = spawnTime;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (nextSpawn == null) {
            try {
                String first = spawnTimes.get(0);
                String[] split = first.split(":");
                nextSpawn = LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
            } catch (Exception ignored) {
                return "Error calculating time.";
            }
        }

        Duration duration = Duration.between(now, nextSpawn);
        if (duration.isNegative()) duration = duration.plusDays(1);

        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;

        return "Next Box: " + hours + " hours " + minutes + " minute";
    }

    private void broadcast(String msg) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&e[QEventBox] &r");
        String finalMsg = ChatColor.translateAlternateColorCodes('&', prefix + msg);

        Bukkit.broadcastMessage(finalMsg);
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(),
                "minecraft:entity.experience_orb.pickup", 1f, 1f));
    }

    private void sendPlayerMessage(Player player, String msg) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&e[QEventBox] &r");
        String finalMsg = ChatColor.translateAlternateColorCodes('&', prefix + msg);
        player.sendMessage(finalMsg);
        player.playSound(player.getLocation(), "minecraft:entity.experience_orb.pickup", 1f, 1f);
    }

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
                    String msg;

                    if (autoSpawn && !autoHalfBroadcasted && ticksLeft <= half) {
                        autoHalfBroadcasted = true;
                        msg = plugin.getConfig().getString("messages.half-auto", "&6Auto Box will disappear in %seconds% seconds!");
                        broadcast(msg.replace("%seconds%", String.valueOf(ticksLeft)));
                    } else if (!autoSpawn && !halfBroadcasted && ticksLeft <= half) {
                        halfBroadcasted = true;
                        msg = plugin.getConfig().getString("messages.half-manual", "&6Box will disappear in %seconds% seconds!");
                        broadcast(msg.replace("%seconds%", String.valueOf(ticksLeft)));
                    }

                    if (autoSpawn && !autoExpiredBroadcasted && ticksLeft <= 0) {
                        autoExpiredBroadcasted = true;
                        msg = plugin.getConfig().getString("messages.expired-auto", "&cAuto Box disappeared!");
                        broadcast(msg);
                    } else if (!autoSpawn && ticksLeft <= 0) {
                        msg = plugin.getConfig().getString("messages.expired-manual", "&cBox disappeared due to unclaimed!");
                        broadcast(msg);
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