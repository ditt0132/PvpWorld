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
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.net.http.WebSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ForceDuelCMD implements CommandExecutor, Listener {
    private final KitManager kitManager;
    private final DuelManager duelManager;
    private final JavaPlugin plugin;

    private final long EXPIRE_MILLIS = 60_000;
    private final Map<UUID, me.ujun.pvpWorld.command.DuelCMD.Invite> forceInbox = new ConcurrentHashMap<>();

    public ForceDuelCMD(KitManager kitManager, DuelManager duelManager, JavaPlugin plugin) {
        this.kitManager = kitManager;
        this.duelManager = duelManager;
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        if (args.length < 2) {
            return false;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
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


        if (!kitManager.exists(kitName)) {
            sender.sendMessage(ChatColor.RED + "없는 키트입니다: " + kitName);
            return false;
        }

        Kit kit = kitManager.get(kitName);

        if (args.length > 2) {
            roundSetting = Math.max(Integer.parseInt(args[2]), 1);
        }

        if (!target.isOnline()) {
            duelManager.offlineDuelInvited.add(target.getUniqueId());

            forceInbox.put(target.getUniqueId(), new DuelCMD.Invite(player.getUniqueId(), target.getUniqueId(), kit.getName(), roundSetting, System.currentTimeMillis() + EXPIRE_MILLIS));

            sender.sendMessage( ChatColor.YELLOW + "해당 플레이어가 오프라인입니다.\n접속시 실행됩니다");
            return true;
        }

        Player onlineTarget = (Player) target;
        if (duelManager.isInDuel(player) || duelManager.isInDuel(onlineTarget)) { player.sendMessage("§c이미 듀얼 중인 플레이어가 있습니다."); return true; }

        return startDuel(player, onlineTarget, kit, roundSetting);
    }


    private boolean startDuel(Player player, Player target, Kit kit, int roundSetting) {
        player.sendMessage("§a§l" + target.getName() + "§f§l님에게 강제 듀얼을 실행했습니다. (키트: §b§l" + kit.getDisplayName() + "§f§l | 라운드: §e§l" + roundSetting + "§f§l)");
        Component msg = Component.text(player.getName(), NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("님에게 강제 듀얼을 실행받았습니다. (키트: §b§l" + kit.getDisplayName() + "§f§l | 라운드: §e§l" + roundSetting + "§f§l)", NamedTextColor.WHITE, TextDecoration.BOLD));
        target.sendMessage(msg);

        boolean ok = duelManager.startDuel(new ArrayList<>(List.of(player)), new ArrayList<>(List.of(target)), kit, roundSetting, false);
        if (!ok) {
            player.sendMessage("§c듀얼 시작 실패.");
            target.sendMessage("§c듀얼 시작 실패.");
            return false;
        }
        return true;
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent e) {
        Player target = e.getPlayer();

        if (duelManager.offlineDuelInvited.contains(target.getUniqueId())) {
            duelManager.offlineDuelInvited.remove(target.getUniqueId());

            DuelCMD.Invite inv = forceInbox.get(target.getUniqueId());
            if (inv == null) { return; }
//            if (inv.expired()) {
//                forceInbox.remove(target.getUniqueId());
//                return;
//            }

            Player player = Bukkit.getPlayer(inv.from);

            if (player == null) {
                forceInbox.remove(target.getUniqueId());
                return;
            }

            Kit kit = kitManager.get(inv.kitName);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                startDuel(player, target, kit, inv.roundSetting);
            }, 5L);

        }
    }
}
