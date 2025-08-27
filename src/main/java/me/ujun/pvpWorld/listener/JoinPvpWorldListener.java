package me.ujun.pvpWorld.listener;

import me.ujun.pvpWorld.PvpWorld;
import me.ujun.pvpWorld.config.ConfigHandler;
import me.ujun.pvpWorld.util.ResetUtil;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;


public class JoinPvpWorldListener implements Listener {


    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (PvpWorld.devPlayers.contains(player.getUniqueId())) {
            return;
        }

        if (!player.getLocation().getWorld().getName().equals(ConfigHandler.pvpWorld)) {
            return;
        }

        ResetUtil.joinLobby(player);
    }

    @EventHandler
    private void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World to = player.getWorld();

        if (PvpWorld.devPlayers.contains(player.getUniqueId())) {
            return;
        }


        if (!to.getName().equals(ConfigHandler.pvpWorld)) {
            return;
        }

        ResetUtil.joinLobby(player);
    }

}
