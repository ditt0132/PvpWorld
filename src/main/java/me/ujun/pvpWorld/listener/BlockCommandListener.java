package me.ujun.pvpWorld.listener;

import me.ujun.pvpWorld.PvpWorld;
import me.ujun.pvpWorld.config.ConfigHandler;
import me.ujun.pvpWorld.duel.DuelManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public class BlockCommandListener implements Listener {

    private final DuelManager duel;
    public BlockCommandListener(DuelManager duel) { this.duel = duel; }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();


        if (PvpWorld.pvpPlayerTimer.containsKey(player.getUniqueId()) || duel.isInDuel(player)) {
            for (String blocked : ConfigHandler.blockedCommandsInFight) {
                if (command.startsWith(blocked.toLowerCase())) {
                    player.sendMessage(ChatColor.RED + "PVP 중에는 해당 명령어를 사용할 수 없습니다!");
                    event.setCancelled(true);
                    return;
                }
            }
        }

//        player.sendMessage(player.getLocation().getWorld().getName());

        if (!PvpWorld.devPlayers.contains(player.getUniqueId()) && ConfigHandler.pvpWorld.contains(player.getLocation().getWorld().getName())) {
            for (String allowed : ConfigHandler.allowedCommandsInPvpWorld) {
                if (command.startsWith(allowed.toLowerCase())) {
                    return;
                }
            }

//            player.sendMessage(command);
            player.sendMessage(ChatColor.RED + "PVP 월드에서는 해당 명령어를 사용할 수 없습니다!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGamemodeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();

        if (PvpWorld.devPlayers.contains(player.getUniqueId()) || (!ConfigHandler.pvpWorld.contains(player.getLocation().getWorld().getName()))) {
            return;
        }

        GameMode from = player.getGameMode();
        GameMode to  = event.getNewGameMode();

        boolean fromSurvOrAdv = (from == GameMode.SURVIVAL || from == GameMode.ADVENTURE);
        boolean toCreativeOrSpec = (to == GameMode.CREATIVE || to == GameMode.SPECTATOR);

        if (fromSurvOrAdv && toCreativeOrSpec) {
            event.setCancelled(true);
        }

    }
}
