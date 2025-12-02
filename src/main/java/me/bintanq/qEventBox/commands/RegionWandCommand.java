package me.bintanq.qEventBox.commands;

import me.bintanq.qEventBox.QEventBox;
import me.bintanq.qEventBox.listeners.RegionWandListener;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RegionWandCommand implements CommandExecutor, TabCompleter {

    private final QEventBox plugin;
    private final RegionWandListener listener;

    private static final List<String> SUBCOMMANDS = Arrays.asList("give", "save", "reset");

    public RegionWandCommand(QEventBox plugin, RegionWandListener listener) {
        this.plugin = plugin;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "[QEventBox] Command ini hanya bisa dieksekusi oleh pemain.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            args = new String[]{"give"};
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "give":
                handleGive(player);
                break;
            case "save":
                handleSave(player);
                break;
            case "reset":
                handleReset(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Penggunaan: /qwand <give|save|reset>");
                break;
        }

        return true;
    }

    private void handleGive(Player player) {
        String perm = "qeventbox.wand.give";
        if (!player.hasPermission(perm)) {
            player.sendMessage(ChatColor.RED + "[QEventBox] No permission (" + perm + ").");
            return;
        }

        ItemStack wand = createRegionWand();
        player.getInventory().addItem(wand);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a&l[QEventBox] &7Anda menerima &b&lRegion Wand&7."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7Gunakan &b/qwand save &7untuk menyimpan region."));
    }

    private void handleSave(Player player) {
        String perm = "qeventbox.wand.save";
        if (!player.hasPermission(perm)) {
            player.sendMessage(ChatColor.RED + "[QEventBox] No permission (" + perm + ").");
            return;
        }

        UUID uuid = player.getUniqueId();
        Location loc1 = listener.getPos1Map().get(uuid);
        Location loc2 = listener.getPos2Map().get(uuid);

        if (loc1 == null || loc2 == null) {
            player.sendMessage(ChatColor.RED + "[QEventBox] Harap tentukan Posisi 1 dan Posisi 2 terlebih dahulu.");
            return;
        }

        if (!loc1.getWorld().equals(loc2.getWorld())) {
            player.sendMessage(ChatColor.RED + "[QEventBox] Error: Posisi 1 dan Posisi 2 harus berada di dunia yang sama!");
            return;
        }

        // Hitung koordinat Min/Max (X, Y, Z)
        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());

        // NEW: Y-Axis Calculation
        int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());

        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        // Update config.yml
        FileConfiguration config = plugin.getConfig();

        config.set("region.min-x", minX);
        config.set("region.max-x", maxX);

        // NEW: Menyimpan Y-Axis
        config.set("region.min-y", minY);
        config.set("region.max-y", maxY);

        config.set("region.min-z", minZ);
        config.set("region.max-z", maxZ);

        // Tambahkan dunia ke list 'region.world'
        List<String> allowedWorlds = config.getStringList("region.world");
        World currentWorld = loc1.getWorld();

        if (!allowedWorlds.contains(currentWorld.getName())) {
            allowedWorlds.add(currentWorld.getName());
            config.set("region.world", allowedWorlds);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&e&l[QEventBox] &7Dunia &e" + currentWorld.getName() + " &7ditambahkan ke daftar region.world."));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&e&l[QEventBox] &7Dunia &e" + currentWorld.getName() + " &7sudah ada di daftar region.world."));
        }

        plugin.saveConfig();

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a&l[QEventBox] &a&lRegion berhasil disimpan!"));
        // NEW: Menambahkan output Y-Axis
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7Koordinat Region: &aX:&f " + minX + " to " + maxX + "&a, Y:&f " + minY + " to " + maxY + "&a, Z:&f " + minZ + " to " + maxZ + "&a."));

        // Otomatis reset setelah save
        handleReset(player);
    }

    private void handleReset(Player player) {
        String perm = "qeventbox.wand.reset";
        if (!player.hasPermission(perm)) {
            player.sendMessage(ChatColor.RED + "[QEventBox] No permission (" + perm + ").");
            return;
        }

        UUID uuid = player.getUniqueId();
        listener.getPos1Map().remove(uuid);
        listener.getPos2Map().remove(uuid);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a&l[QEventBox] &7Posisi wand telah &areset&7."));
    }

    // --- WAND ITEM CREATION ---

    private ItemStack createRegionWand() {
        FileConfiguration config = plugin.getConfig();

        Material material = Material.getMaterial(config.getString("wand-item.material", "GOLDEN_AXE"));
        if (material == null) material = Material.GOLDEN_AXE;

        ItemStack wand = new ItemStack(material);
        ItemMeta meta = wand.getItemMeta();

        String name = config.getString("wand-item.name", "&b&lEventBox Region Wand");
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

        List<String> lore = config.getStringList("wand-item.lore");

        List<String> coloredLore = lore.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());

        meta.setLore(coloredLore);
        wand.setItemMeta(meta);

        return wand;
    }

    // --- TAB COMPLETER ---

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}