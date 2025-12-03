package me.bintanq.qEventBox.listeners;

import me.bintanq.qEventBox.QEventBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final QEventBox plugin;

    public PlayerListener(QEventBox plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        plugin.getPointsManager().savePlayer(e.getPlayer().getUniqueId());
    }
}