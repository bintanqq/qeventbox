package me.bintanq.qEventBox.commands;

import me.bintanq.qEventBox.QEventBox;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class PointsCommand implements CommandExecutor, TabCompleter {

    private final QEventBox plugin;
    private final List<String> subCommands = Arrays.asList("check", "give", "remove", "set");

    public PointsCommand(QEventBox plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("qeventbox.admin")) {
            sendMessage(sender,"§cNo permission");
            return true;
        }

        if (args.length<2) { sendMessage(sender,"Usage: /qpoints <check|give|remove|set> <player> [amount]"); return true; }

        String sub = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        if(target==null) { sendMessage(sender,"Player not online"); return true; }
        UUID tid = target.getUniqueId();

        try {
            switch(sub) {
                case "check": sendMessage(sender,"Points: "+plugin.getPointsManager().getPoints(tid)); break;
                case "give":
                    int g = Integer.parseInt(args[2]);
                    plugin.getPointsManager().addPoints(tid,g);
                    sendMessage(sender,"§aGiven "+g+" points to "+target.getName());
                    break;
                case "remove":
                    int r = Integer.parseInt(args[2]);
                    plugin.getPointsManager().removePoints(tid,r);
                    sendMessage(sender,"§aRemoved "+r+" points from "+target.getName());
                    break;
                case "set":
                    int s = Integer.parseInt(args[2]);
                    plugin.getPointsManager().setPoints(tid,s);
                    sendMessage(sender,"§aSet "+target.getName()+" points to "+s);
                    break;
                default: sendMessage(sender,"Unknown subcommand");
            }
        } catch(Exception e){ sendMessage(sender,"Bad number or usage"); }

        return true;
    }

    private void sendMessage(CommandSender sender,String msg){
        String formatted = "\n\n§e§l[QEventBox] §r"+msg+"\n\n";
        sender.sendMessage(formatted);
        if(sender instanceof Player){
            ((Player)sender).playSound(((Player)sender).getLocation(),org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("qeventbox.admin")) return Collections.emptyList();

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String current = args[0].toLowerCase();
            for (String sub : subCommands) if (sub.startsWith(current)) completions.add(sub);
            return completions;
        } else if (args.length == 2) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        return Collections.emptyList();
    }
}
