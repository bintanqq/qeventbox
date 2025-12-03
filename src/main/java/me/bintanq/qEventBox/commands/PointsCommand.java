package me.bintanq.qEventBox.commands;

import me.bintanq.qEventBox.QEventBox;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PointsCommand implements CommandExecutor, TabCompleter {

    private final QEventBox plugin;
    private final List<String> subCommands = Arrays.asList("check", "give", "remove", "set");

    public PointsCommand(QEventBox plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("qeventbox.admin")) {
            sendMessage(sender,"&cNo permission");
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender,"&cUsage: /qpoints <check|give|remove|set> <player> [amount]");
            return true;
        }

        String sub = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);

        if(target == null) {
            sendMessage(sender,"&cPlayer not online");
            return true;
        }
        UUID tid = target.getUniqueId();

        String pointName = plugin.getConfig().getString("points", "Points");

        try {
            switch(sub) {
                case "check":
                    int points = plugin.getPointsManager().getPoints(tid);
                    sendMessage(sender,"&e" + pointName + ": &f" + points);
                    break;
                case "give":
                    int g = Integer.parseInt(args[2]);
                    plugin.getPointsManager().addPoints(tid, g);
                    sendSuccessMessage(sender,"&aGiven " + g + " " + pointName + " to " + target.getName());
                    break;
                case "remove":
                    int r = Integer.parseInt(args[2]);
                    plugin.getPointsManager().removePoints(tid, r);
                    sendSuccessMessage(sender,"&aRemoved " + r + " " + pointName + " from " + target.getName());
                    break;
                case "set":
                    int s = Integer.parseInt(args[2]);
                    plugin.getPointsManager().setPoints(tid, s);
                    sendSuccessMessage(sender,"&aSet " + target.getName() + " " + pointName + " to " + s);
                    break;
                default:
                    sendMessage(sender,"&cUnknown subcommand");
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            sendMessage(sender,"&cMissing amount argument.");
        } catch(NumberFormatException e){
            sendMessage(sender,"&cInvalid number format.");
        } catch(Exception e){
            sendMessage(sender,"&cAn error occurred: " + e.getMessage());
        }

        return true;
    }

    private void sendMessage(CommandSender sender, String msg) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&e[QEventBox] &r");
        String finalMessage = ChatColor.translateAlternateColorCodes('&', prefix + msg);

        sender.sendMessage(finalMessage);
    }

    private void sendSuccessMessage(CommandSender sender, String msg) {
        sendMessage(sender, msg);
        if (sender instanceof Player) {
            ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("qeventbox.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return subCommands.stream()
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}