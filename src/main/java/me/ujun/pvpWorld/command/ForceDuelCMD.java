package me.ujun.pvpWorld.command;

import me.ujun.pvpWorld.duel.DuelManager;
import me.ujun.pvpWorld.kit.Kit;
import me.ujun.pvpWorld.kit.KitManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ForceDuelCMD implements CommandExecutor {
    private final KitManager kitManager;
    private final DuelManager duelManager;

    private final long EXPIRE_MILLIS = 60_000;
    private final Map<UUID, Map<UUID, me.ujun.pvpWorld.command.DuelCMD.Invite>> inbox = new ConcurrentHashMap<>();

    public ForceDuelCMD(KitManager kitManager, DuelManager duelManager) {
        this.kitManager = kitManager;
        this.duelManager = duelManager;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        if (args.length < 2) {
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        String kitName = args[1];
        int roundSetting = 1;

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "존재하지 않는 플레이어");
            return false;
        }

        if (target.equals(player)) {
            sender.sendMessage(ChatColor.RED + "본인에게 듀얼을 신청할 수 없습니다");
            return false;
        }

        if (duelManager.isInDuel(player) || duelManager.isInDuel(target)) { player.sendMessage("§c이미 듀얼 중인 플레이어가 있습니다."); return true; }

        if (!kitManager.exists(kitName)) {
            sender.sendMessage(ChatColor.RED + "없는 키트입니다: " + kitName);
            return false;
        }

        Kit kit = kitManager.get(kitName);

        if (args.length > 2) {
            roundSetting = Integer.parseInt(args[2]);
        }

        player.sendMessage("§a§l" + target.getName() + "§f§l님에게 강제 듀얼을 실행했습니다. (키트: §b§l" + kit.getDisplayName() + "§f§l | 라운드: §e§l" + roundSetting + "§f§l)");
        // 클릭 수락 메시지
        Component msg = Component.text(player.getName(), NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("님에게 강제 듀얼을 실행받았습니다. (키트: §b§l" + kit.getDisplayName() + "§f§l | 라운드: §e§l" + String.valueOf(roundSetting) + "§f§l)", NamedTextColor.WHITE, TextDecoration.BOLD));
        target.sendMessage(msg);

        boolean ok = duelManager.startDuel(new ArrayList<>(List.of(player)), new ArrayList<>(List.of(target)), kit, roundSetting, false);
        if (!ok) {
            player.sendMessage("§c듀얼 시작 실패.");
            target.sendMessage("§c듀얼 시작 실패.");
            return false;
        }

        return true;
    }
}
