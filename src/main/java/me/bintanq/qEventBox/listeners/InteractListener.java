package me.bintanq.qEventBox.listeners;

import me.bintanq.qEventBox.QEventBox;
import me.bintanq.qEventBox.managers.CrateManager;
import me.bintanq.qEventBox.managers.CrateManager.CrateData;
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
        // Check 1: Ensure a block was clicked
        if(b==null) return;

        CrateManager cm = plugin.getCrateManager();
        // Attempt to retrieve CrateData associated with the clicked block
        Optional<CrateData> crateOpt = cm.getCrateByBlock(b);

        // Crate claiming logic
        if(crateOpt.isPresent()){
            CrateData crate = crateOpt.get();
            Player p = e.getPlayer();

            // Claim the crate (handles rewards, removal, and broadcast)
            cm.claimCrate(crate.getId(),p);

            // Send confirmation message to the player
            sendMessage(p,"You claimed the crate!");

            // Cancel the event to prevent placing/interacting with items
            e.setCancelled(true);
        }
    }

    // Utility method to send formatted plugin messages and sound feedback
    private void sendMessage(Player p,String msg){
        String formatted="\n\n§e[QEventBox] §r"+msg+"\n\n";
        p.sendMessage(formatted);
        p.playSound(p.getLocation(),org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f);
    }
}