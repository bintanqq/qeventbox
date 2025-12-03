package me.bintanq.qEventBox.listeners;

import me.bintanq.qEventBox.QEventBox;
import me.bintanq.qEventBox.managers.CrateManager;
import me.bintanq.qEventBox.managers.CrateManager.CrateData;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

public class InteractListener implements Listener {

    private final QEventBox plugin;
    public InteractListener(QEventBox plugin){ this.plugin=plugin; }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e){
        Block b = e.getClickedBlock();
        if(b==null) return;

        CrateManager cm = plugin.getCrateManager();
        Optional<CrateData> crateOpt = cm.getCrateByBlock(b);

        if(crateOpt.isPresent()){
            CrateData crate = crateOpt.get();
            Player p = e.getPlayer();

            cm.claimCrate(crate.getId(),p);

            String claimedMsg = plugin.getConfig().getString("messages.claim-success", "&aYou successfully claimed the crate!");
            sendMessage(p, claimedMsg);

            e.setCancelled(true);
        }
    }

    private void sendMessage(Player p,String msg){
        String prefix = plugin.getConfig().getString("messages.prefix", "&e[QEventBox] &r");
        String finalMsg = ChatColor.translateAlternateColorCodes('&', prefix + msg);
        p.sendMessage(finalMsg);
        p.playSound(p.getLocation(),org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f);
    }
}