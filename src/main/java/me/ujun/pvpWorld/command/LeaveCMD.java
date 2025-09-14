package me.ujun.pvpWorld.command;

import me.ujun.pvpWorld.duel.DuelManager;
import me.ujun.pvpWorld.duel.Instance;
import me.ujun.pvpWorld.util.ResetUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LeaveCMD implements CommandExecutor {

    private final DuelManager duel;

    public LeaveCMD(DuelManager duel) {
        this.duel = duel;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능");
            return false;
        }

        if ((!duel.isInDuel(player))) {
            sender.sendMessage("듀얼 중이 아님");
            return false;
        }

        Instance inst = duel.getInstanceOf(player);

        if (inst.watchers.contains(player.getUniqueId())) {
            player.sendMessage("관전을 종료했습니다");
            ResetUtil.joinLobby(player);
        } else {
            ResetUtil.joinLobby(player);
        }

        return true;
    }
}
