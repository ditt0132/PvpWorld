package me.ujun.pvpWorld.command;

import me.ujun.pvpWorld.PvpWorld;
import me.ujun.pvpWorld.duel.DuelManager;
import me.ujun.pvpWorld.kit.Kit;
import me.ujun.pvpWorld.kit.KitManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.antlr.v4.semantics.BlankActionSplitterListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.nio.file.attribute.FileAttribute;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DuelCMD implements CommandExecutor {
    private final KitManager kitManager;
    private final DuelManager duelManager;

    private final long EXPIRE_MILLIS = 60_000;
    private final Map<UUID, Map<UUID, Invite>> inbox = new ConcurrentHashMap<>();

    public DuelCMD(KitManager kitManager, DuelManager duelManager) {
        this.kitManager = kitManager;
        this.duelManager = duelManager;
    }

    public static class Invite {
        final UUID from, to;
        final String kitName;
        final int roundSetting;
        final long expireAt;
        Invite(UUID from, UUID to, String kitName, int roundSetting, long expireAt) {
            this.from = from; this.to = to; this.kitName = kitName; this.roundSetting = roundSetting; this.expireAt = expireAt;
        }
        boolean expired() { return System.currentTimeMillis() > expireAt; }

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        if (args.length < 2) {
            return false;
        }

        if (args[0].equals("accept")) {
            Player from = Bukkit.getPlayerExact(args[1]);
            if (from == null) { player.sendMessage("§c상대가 오프라인입니다."); return true; }
            if (duelManager.isInDuel(player) || duelManager.isInDuel(from)) { player.sendMessage("§c이미 듀얼 중입니다."); return true; }

            Map<UUID, Invite> map = inbox.getOrDefault(player.getUniqueId(), Collections.emptyMap());
            Invite inv = map.get(from.getUniqueId());
            if (inv == null) { player.sendMessage("§c" + from.getName() + "님의 초대가 없습니다."); return true; }
            if (inv.expired()) {
                map.remove(from.getUniqueId());
                player.sendMessage("§c초대가 만료되었습니다.");
                return false;
            }

            Kit kit = kitManager.get(inv.kitName);
            if (!kitManager.exists(inv.kitName)) {
                sender.sendMessage(ChatColor.RED + "없는 키트입니다: " + inv.kitName);
                map.remove(from.getUniqueId());
                return false;
            }

            // 수락 → 인박스 정리
            map.remove(from.getUniqueId());
            if (map.isEmpty()) inbox.remove(player.getUniqueId());

            player.sendMessage("§a§l듀얼을 수락했습니다!");
            from.sendMessage("§a§l" + player.getName() + "님이 듀얼을 수락했습니다!" );


            boolean ok = duelManager.startDuel(new ArrayList<>(List.of(from)), new ArrayList<>(List.of(player)), kit, inv.roundSetting, false);
            if (!ok) {
                player.sendMessage("§c듀얼 시작 실패.");
                from.sendMessage("§c듀얼 시작 실패.");
                map.remove(from.getUniqueId());
                return false;
            }

            return true;
        } else if (args[0].equals("deny")) {
            Player from = Bukkit.getPlayerExact(args[1]);
            if (from == null) { player.sendMessage("§c상대가 오프라인입니다."); return false; }
            if (duelManager.isInDuel(player) || duelManager.isInDuel(from)) { player.sendMessage("§c이미 듀얼 중입니다."); return false; }

            Map<UUID, Invite> map = inbox.getOrDefault(player.getUniqueId(), Collections.emptyMap());
            Invite inv = map.get(from.getUniqueId());
            if (inv == null) { player.sendMessage("§c" + from.getName() + "님의 초대가 없습니다."); return false; }
            if (inv.expired()) {
                map.remove(from.getUniqueId());
                player.sendMessage("§c초대가 만료되었습니다.");
                return false;
            }



            // 수락 → 인박스 정리
            map.remove(from.getUniqueId());
            if (map.isEmpty()) inbox.remove(player.getUniqueId());

            player.sendMessage("§c§l듀얼을 거절했습니다!");
            from.sendMessage("§c§l" + player.getName() + "님이 듀얼을 거절했습니다!" );

            return true;
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

        inbox.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>())
                .put(player.getUniqueId(), new Invite(player.getUniqueId(), target.getUniqueId(), kit.getName(), roundSetting, System.currentTimeMillis() + EXPIRE_MILLIS));

        player.sendMessage("§a§l" + target.getName() + "§f§l님에게 듀얼 신청 보냈습니다. (키트: §b§l" + kit.getDisplayName() + "§f§l | 라운드: §e§l" + roundSetting + "§f§l)");
        // 클릭 수락 메시지
        Component msg = Component.text(player.getName(), NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(Component.text("님에게 듀얼 신청을 받았습니다. (키트: §b§l" + kit.getDisplayName() + "§f§l | 라운드: §e§l" + String.valueOf(roundSetting) + "§f§l)", NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text("\n\n[수락하기]", NamedTextColor.GREEN,  TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/" + label + " accept " + player.getName()))
                                .hoverEvent(HoverEvent.showText(Component.text("클릭하여 수락하기"))))
                .append(Component.text("  [거절하기]", NamedTextColor.RED,  TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/" + label + " deny " + player.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("클릭하여 거절하기"))));
        target.sendMessage(msg);

        return true;

    }

}
