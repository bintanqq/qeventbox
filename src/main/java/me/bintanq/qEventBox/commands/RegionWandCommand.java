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

    private void sendMessage(CommandSender sender, String msg) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&e[QEventBox] &r");
        String finalMessage = ChatColor.translateAlternateColorCodes('&', prefix + msg);

        sender.sendMessage(finalMessage);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "&cThis command can only be executed by a player.");
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
                sendMessage(player, "&cUsage: /qwand <give|save|reset>");
                break;
        }

        return true;
    }

    private void handleGive(Player player) {
        String perm = "qeventbox.wand.give";
        if (!player.hasPermission(perm)) {
            sendMessage(player, "&cNo permission (&f" + perm + "&c).");
            return;
        }

        ItemStack wand = createRegionWand();
        player.getInventory().addItem(wand);

        sendMessage(player, "&7You received the &b&lRegion Wand&7.");
        sendMessage(player, "&7Use &b/qwand save &7to save the region.");
    }

    private void handleSave(Player player) {
        String perm = "qeventbox.wand.save";
        if (!player.hasPermission(perm)) {
            sendMessage(player, "&cNo permission (&f" + perm + "&c).");
            return;
        }

        UUID uuid = player.getUniqueId();
        Location loc1 = listener.getPos1Map().get(uuid);
        Location loc2 = listener.getPos2Map().get(uuid);

        if (loc1 == null || loc2 == null) {
            sendMessage(player, "&cPlease set Position 1 and Position 2 first.");
            return;
        }

        if (!loc1.getWorld().equals(loc2.getWorld())) {
            sendMessage(player, "&cError: Position 1 and Position 2 must be in the same world!");
            return;
        }

        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());

        int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());

        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        FileConfiguration config = plugin.getConfig();

        config.set("region.min-x", minX);
        config.set("region.max-x", maxX);
        config.set("region.min-y", minY);
        config.set("region.max-y", maxY);
        config.set("region.min-z", minZ);
        config.set("region.max-z", maxZ);

        List<String> allowedWorlds = config.getStringList("region.world");
        World currentWorld = loc1.getWorld();
        String worldName = currentWorld.getName();

        if (!allowedWorlds.contains(worldName)) {
            allowedWorlds.add(worldName);
            config.set("region.world", allowedWorlds);
            sendMessage(player, "&7World &e" + worldName + " &7has been added to the region.world list.");
        } else {
            sendMessage(player, "&7World &e" + worldName + " &7is already in the region.world list.");
        }

        plugin.saveConfig();

        sendMessage(player, "&a&lRegion saved successfully!");

        String coordsMsg = "&7Region Coordinates: &aX:&f " + minX + " to " + maxX +
                "&a, Y:&f " + minY + " to " + maxY +
                "&a, Z:&f " + minZ + " to " + maxZ + "&a.";
        sendMessage(player, coordsMsg);

        handleReset(player);
    }

    private void handleReset(Player player) {
        String perm = "qeventbox.wand.reset";
        if (!player.hasPermission(perm)) {
            sendMessage(player, "&cNo permission (&f" + perm + "&c).");
            return;
        }

        UUID uuid = player.getUniqueId();
        listener.getPos1Map().remove(uuid);
        listener.getPos2Map().remove(uuid);

        sendMessage(player, "&7Wand positions have been &areset&7.");
    }

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