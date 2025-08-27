package me.ujun.pvpWorld.command;

import me.ujun.pvpWorld.PvpWorld;
import me.ujun.pvpWorld.config.ConfigHandler;
import me.ujun.pvpWorld.kit.Kit;
import me.ujun.pvpWorld.kit.KitManager;
import me.ujun.pvpWorld.util.ResetUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FfaCMD implements CommandExecutor {
    private final KitManager kitManager;


    public FfaCMD(KitManager kitManager) {
        this.kitManager = kitManager;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        if (args.length < 1) {
            return false;
        }

        String kitName = args[0];

        if (!kitManager.exists(kitName)) {
            sender.sendMessage(ChatColor.RED + "없는 키트입니다: " + kitName);
            return false;
        }

        Location spawnLocation = PvpWorld.ffaSpawnLocations.get(kitName);

        if (spawnLocation == null) {
            sender.sendMessage(ChatColor.RED + kitName + " 키트의 스폰이 정해지지 않았습니다.");
            return false;
        }

        Kit kit = kitManager.get(kitName);
        PvpWorld.playerKits.put(player.getUniqueId(), kit);
        kitManager.applyTo(player, kit, true, true);
        player.teleport(spawnLocation);
        player.addScoreboardTag(ConfigHandler.ffaTag);
        ResetUtil.resetPlayerState(player);



        player.sendMessage("FFA에 참여했습니다.");

        return true;
    }
}
