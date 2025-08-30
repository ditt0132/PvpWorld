package me.ujun.pvpWorld.command;

import me.ujun.pvpWorld.config.ConfigHandler;
import me.ujun.pvpWorld.util.ResetUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LobbyCMD implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용할 수 있습니다.");
            return false;
        }

        if (ConfigHandler.lobby != null) {
            ResetUtil.joinLobby(player);
            player.sendMessage(ChatColor.GREEN + "로비로 이동했습니다!");

        } else {
            player.sendMessage(ChatColor.RED + "로비가 설정되지 않았습니다.");
            return false;
        }


        return true;
    }
}
