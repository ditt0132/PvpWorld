package me.ujun.pvpWorld.listener;

import me.ujun.pvpWorld.PvpWorld;
import me.ujun.pvpWorld.config.ConfigHandler;
import me.ujun.pvpWorld.duel.DuelManager;
import me.ujun.pvpWorld.duel.Instance;
import me.ujun.pvpWorld.util.ResetUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;


public class JoinPvpWorldListener implements Listener {

    private final DuelManager duel;
    public JoinPvpWorldListener(DuelManager duel) { this.duel = duel; }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (duel.offlineDuelInvited.contains(player.getUniqueId())) {
            return;
        }

        if (ConfigHandler.pvpWorld.contains(player.getLocation().getWorld().getName())) {
            ResetUtil.joinLobby(player);
        }
    }

    @EventHandler
    private void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World to = player.getWorld();

        if (duel.isInDuel(player)) {
            if (event.getFrom().getName().equals("pvpworld_void")) {
                Instance inst = duel.getInstanceOf(player);

                if (!inst.isShuttingDown) {
                    duel.leaveDuel(player, inst);
                }
            } else {
                return;
            }
        }

        if (PvpWorld.devPlayers.contains(player.getUniqueId())) {
            return;
        }


        if (!ConfigHandler.pvpWorld.contains(to.getName())) {
            return;
        }

        ResetUtil.joinLobby(player);
    }

}
