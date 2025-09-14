package me.ujun.pvpWorld.command;

import me.ujun.pvpWorld.duel.DuelManager;
import me.ujun.pvpWorld.duel.Instance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DuelWatchCMD implements CommandExecutor {

    private final DuelManager duel;

    public DuelWatchCMD(DuelManager duel) {
        this.duel = duel;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능");
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage("타겟 이름 입력하기");
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            sender.sendMessage("없는 플레이어");
            return false;
        }

        if (!(duel.isInDuel(target))) {
            sender.sendMessage("듀얼 중인 플레이어가 아님");
            return false;
        }

        Instance inst = duel.getInstanceOf(target);

        if (inst.watchers.contains(target.getUniqueId())) {
            sender.sendMessage("듀얼 중인 플레이어가 아님");
            return false;
        }
        duel.byPlayer.put(player.getUniqueId(), inst);

        Location loc = inst.origin.clone().add(inst.meta.center().dx(), inst.meta.center().dy(), inst.meta.center().dz());
        loc.add(0.5, 0, 0.5);
        player.teleport(loc);

        sender.sendMessage(target.getName() + "의 관전을 시작했습니다");
        duel.setSpectator(player, true, inst);
        Bukkit.getLogger().info(String.valueOf(player.getAllowFlight()));
        inst.watchers.add(player.getUniqueId());


        return true;
    }
}
