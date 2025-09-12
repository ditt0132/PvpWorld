package me.ujun.pvpWorld.command;

import com.ibm.icu.impl.data.HolidayBundle_ja_JP;
import it.unimi.dsi.fastutil.ints.IntLists;
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
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class PartyCMD implements CommandExecutor {

    public static Map<UUID, Set<UUID>> partys = new HashMap<>(); //파티용 맵
    public static Map<UUID, UUID> partyLeaderMap = new HashMap<>(); //플레이어에게서 파티 리더를 얻기 위한 맵 + 플레이어가 파티에 있는지 확인용
    private final KitManager kitManager;
    private final DuelManager duelManager;

    public PartyCMD(KitManager kitManager, DuelManager duelManager) {
        this.kitManager = kitManager;
        this.duelManager = duelManager;
    }

    private static final long EXPIRE_MS = 60_000; // 60초 만료
    private static class Invite {
        final UUID leaderId; final long expireAt;
        Invite(UUID leaderId, long expireAt) { this.leaderId = leaderId; this.expireAt = expireAt; }
        boolean expired() { return System.currentTimeMillis() > expireAt; }
    }

    private static class DuelRequest {
        final UUID leaderId; final long expireAt; final Kit kit; final int roundSetting;
        DuelRequest(UUID leaderId, long expireAt, Kit kit, int roundSetting) { this.leaderId = leaderId; this.expireAt = expireAt; this.kit = kit; this.roundSetting = roundSetting;}
        boolean expired() { return System.currentTimeMillis() > expireAt; }
    }

    private final Map<UUID, Map<UUID, Invite>> inbox = new HashMap<>();
    private final Map<UUID, Map<UUID, DuelRequest>> duelInbox = new HashMap<>();



    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용 가능");
            return false;
        }

        if (args.length < 1) {
            return false;
        }


        String subCommand = args[0];

        if (subCommand.equals("create")) {
            if (partyLeaderMap.containsKey(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "이미 파티에 속해 있습니다");
                return false;
            }


            Set<UUID> party = new HashSet<>();
            party.add(p.getUniqueId());
            partyLeaderMap.put(p.getUniqueId(), p.getUniqueId());
            partys.put(p.getUniqueId(), party);
            p.sendMessage(ChatColor.GREEN + "파티를 생성했습니다");
        } else if (subCommand.equals("disband")) {
            if (!partyLeaderMap.containsKey(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "파티에 속해 있지 않습니다");
                return false;
            }

            if (!partys.containsKey(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "파티 리더가 아닙니다");
                return false;
            }


            for (UUID id : partys.get(p.getUniqueId())) {
                Player partyPlayer = Bukkit.getPlayer(id);

                partyLeaderMap.remove(id);
                if (partyPlayer != null) {
                    partyPlayer.sendMessage(ChatColor.GOLD + "파티가 해산되었습니다");
                }
            }

            partys.remove(p.getUniqueId());
        } else if (subCommand.equals("leader")) {
            if (!partyLeaderMap.containsKey(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "파티에 속해 있지 않습니다");
                return false;
            }

            if (!partys.containsKey(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "파티 리더가 아닙니다");
                return false;
            }

            if (args.length < 2) {
                return false;
            }

            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                p.sendMessage(ChatColor.RED + "확인할 수 없는 플레이어");
                return false;
            }

            Set<UUID> party = partys.get(p.getUniqueId());

            if (!party.contains(target.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "파티에 없는 플레이어");
                return false;
            }

            for (UUID id : partys.get(p.getUniqueId())) {
                Player partyPlayer = Bukkit.getPlayer(id);

                partyLeaderMap.put(id, target.getUniqueId());

                if (partyPlayer != null) {
                    partyPlayer.sendMessage("파티 리더를 §e" + target.getName() + "§f님에게 위임했습니다");
                }
            }

            partys.put(target.getUniqueId(), party);
            party.remove(p.getUniqueId());
        } else if (subCommand.equals("leave")) {
            if (!partyLeaderMap.containsKey(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "파티에 속해 있지 않습니다");
                return false;
            }

            if (partys.containsKey(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "파티 리더는 파티를 탈퇴할 수 없습니다");
                return false;
            }

            UUID partyLeaderId = partyLeaderMap.get(p.getUniqueId());
            Set<UUID> party = partys.get(partyLeaderId);

            for (UUID id : party) {
                Player partyPlayer = Bukkit.getPlayer(id);

                if (partyPlayer != null) {
                    partyPlayer.sendMessage(ChatColor.YELLOW + p.getName() + "§f님이 파티를 탈퇴했습니다");
                }
            }

            party.remove(p.getUniqueId());
            partyLeaderMap.remove(p.getUniqueId());
            partys.put(partyLeaderId, party);
        } else if (subCommand.equals("invite")) { // ---------- 초대 ----------
            if (!partys.containsKey(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "파티 리더만 초대할 수 있습니다");
                return true;
            }
            if (args.length < 2) {
                p.sendMessage("§c사용법: /party invite <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { p.sendMessage("§c플레이어를 찾을 수 없습니다"); return true; }
            if (target.getUniqueId().equals(p.getUniqueId())) {
                p.sendMessage("§c자기 자신은 초대할 수 없습니다");
                return true;
            }
            if (partyLeaderMap.containsKey(target.getUniqueId())) {
                p.sendMessage("§c상대는 이미 다른 파티에 속해 있습니다");
                return true;
            }

            inbox.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>())
                    .put(p.getUniqueId(), new Invite(p.getUniqueId(), System.currentTimeMillis() + EXPIRE_MS));

            p.sendMessage("§a" + target.getName() + "§f님에게 초대를 보냈습니다");
            Component msg = Component.text(p.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .append(Component.text("님에게 파티 초대를 받았습니다", NamedTextColor.WHITE, TextDecoration.BOLD))
                    .append(Component.text("\n\n[수락하기]", NamedTextColor.GREEN,  TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand("/" + label + " accept " + p.getName()))
                            .hoverEvent(HoverEvent.showText(Component.text("클릭하여 수락하기"))))
                    .append(Component.text("  [거절하기]", NamedTextColor.RED,  TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand("/" + label + " deny " + p.getName()))
                            .hoverEvent(HoverEvent.showText(Component.text("클릭하여 거절하기"))));
            target.sendMessage(msg);

            return true;

        } else if (subCommand.equals("accept")) {
            if (args.length < 2) {
                p.sendMessage("§e사용법: /party accept <leader>");
                return true;
            }
            if (partyLeaderMap.containsKey(p.getUniqueId())) {
                p.sendMessage("§c이미 파티에 속해 있습니다 먼저 탈퇴하세요.");
                return true;
            }
            Player leader = Bukkit.getPlayer(args[1]);
            if (leader == null) { p.sendMessage("§c리더가 오프라인이거나 존재하지 않습니다"); return true; }

            Map<UUID, Invite> map = inbox.getOrDefault(p.getUniqueId(), Collections.emptyMap());
            Invite inv = map.get(leader.getUniqueId());
            if (inv == null) { p.sendMessage("§c해당 리더의 초대가 없습니다"); return true; }
            if (inv.expired()) {
                map.remove(leader.getUniqueId());
                if (map.isEmpty()) inbox.remove(p.getUniqueId());
                p.sendMessage("§f초대가 §c만료§f되었습니다");
                return true;
            }

            // 리더 파티 유효성 확인
            Set<UUID> party = partys.get(leader.getUniqueId());
            if (party == null) {
                p.sendMessage("§c해당 리더의 파티가 존재하지 않습니다");
                map.remove(leader.getUniqueId());
                if (map.isEmpty()) inbox.remove(p.getUniqueId());
                return true;
            }

            // 가입
            party.add(p.getUniqueId());
            partyLeaderMap.put(p.getUniqueId(), leader.getUniqueId());

            // 안내
            Player l = leader;
            if (l != null) l.sendMessage("§a" + p.getName() + "§f님이 파티 초대를 수락했습니다");
            for (UUID id : party) {
                Player pp = Bukkit.getPlayer(id);
                if (pp != null) pp.sendMessage("§e" + p.getName() + "§f님이 파티에 합류했습니다");
            }

            // 인박스 정리
            map.remove(leader.getUniqueId());
            if (map.isEmpty()) inbox.remove(p.getUniqueId());
            return true;
        } else if (subCommand.equalsIgnoreCase("deny")) {
            if (args.length < 2) {
                p.sendMessage("§e사용법: /party deny <leader>");
                return true;
            }

            // 내 인박스 확인
            Map<UUID, Invite> map = inbox.get(p.getUniqueId());
            if (map == null || map.isEmpty()) {
                p.sendMessage("§c대기 중인 초대가 없습니다");
                return true;
            }

            // 리더 찾기 (온라인 우선, 없으면 인박스 키들로 오프로 매칭)
            String targetName = args[1];
            Player leaderOnline = Bukkit.getPlayerExact(targetName);
            UUID leaderId = (leaderOnline != null) ? leaderOnline.getUniqueId() : null;

            if (leaderId == null) {
                // 인박스에 있는 리더들 중 이름 일치하는 UUID 찾기
                String needle = targetName.toLowerCase(Locale.ROOT);
                for (UUID id : map.keySet()) {
                    String name = Bukkit.getOfflinePlayer(id).getName();
                    if (name != null && name.toLowerCase(Locale.ROOT).equals(needle)) {
                        leaderId = id;
                        break;
                    }
                }
            }

            if (leaderId == null) {
                p.sendMessage("§c해당 리더의 초대를 찾을 수 없습니다");
                return true;
            }

            Invite inv = map.get(leaderId);
            if (inv == null) {
                p.sendMessage("§c해당 리더의 초대가 없습니다");
                return true;
            }

            if (inv.expired()) {
                map.remove(leaderId);
                if (map.isEmpty()) inbox.remove(p.getUniqueId());
                p.sendMessage("§f초대가 §c만료§f되어 제거되었습니다");
                return true;
            }

            // 거절 처리
            map.remove(leaderId);
            if (map.isEmpty()) inbox.remove(p.getUniqueId());

            p.sendMessage("§c초대를 거절했습니다");
            if (leaderOnline != null) {
                leaderOnline.sendMessage("§e" + p.getName() + "§f님이 파티 초대를 거절했습니다");
            }
            return true;
        } else if (subCommand.equals("kick")) {
            if (args.length < 2) {
                return false;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

            if (target == null) {
                p.sendMessage("§c해당 플레이어를 찾을 수 없습니다");
                return false;
            }

            Set<UUID> party = partys.get(p.getUniqueId());

            if (!party.contains(target.getUniqueId())) {
                p.sendMessage("§c해당 플레이어를 팀에서 찾을 수 없습니다");
                return false;
            }

            for (UUID id : party) {
                Player pp = Bukkit.getPlayer(id);
                if (pp != null) pp.sendMessage("§c" + target.getName() + "§f님을 파티에서 추방했습니다");
            }


            party.remove(target.getUniqueId());
            partyLeaderMap.remove(target.getUniqueId());

        } else if (subCommand.equals("list")) {
            if (!partyLeaderMap.containsKey(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "파티에 속해 있지 않습니다");
                return false;
            }

            UUID partyLeaderId = partyLeaderMap.get(p.getUniqueId());
            Set<UUID> party = new HashSet<>(partys.get(partyLeaderId));
            Player leader = Bukkit.getPlayer(partyLeaderId);
            String leaderName = "알 수 없음";

            p.sendMessage(joinAnyNames(party));

            party.remove(partyLeaderId);
            String names = joinAnyNames(party);

            if (leader != null) {
                leaderName = leader.getName();
            }

            p.sendMessage("§7파티 플레이어 리스트:");
            p.sendMessage("§6리더: §f" + leaderName);
            p.sendMessage("§a구성원: §f" + names);

        } else if (subCommand.equals("ffa")) {
            if (args.length < 2) {
                return false;
            }


            if (!partyLeaderMap.containsKey(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "파티에 속해 있지 않습니다");
                return false;
            }

            if (!partys.containsKey(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "파티 리더가 아닙니다");
                return false;
            }

            String kitName = args[1];
            if (!kitManager.exists(kitName)) {
                sender.sendMessage(ChatColor.RED + "없는 키트입니다: " + kitName);
                return false;
            }

            Kit kit = kitManager.get(kitName);

            if (partys.get(p.getUniqueId()).size() < 2) {
                p.sendMessage(ChatColor.RED + "2명 이상 있어야 시작할 수 있습니다");
                return false;
            }

            int roundSetting = 1;

            if (args.length > 2) {
                roundSetting = Math.max(Integer.parseInt(args[2]), 1);
            }

            Set<UUID> partyPlayerIds = partys.get(p.getUniqueId());
            List<Player> players = idsToPlayers(p, partyPlayerIds);

            if (players.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "파티 ffa를 실행할 수 없습니다");
                return false;
            }

            return duelManager.startFFA(players, kit, roundSetting);

        } else if (subCommand.equals("duel")) {

            if (args.length < 3) {
                return false;
            }

            if (!partyLeaderMap.containsKey(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "파티에 속해 있지 않습니다");
                return false;
            }

            if (!partys.containsKey(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "파티 리더가 아닙니다");
                return false;
            }

            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                p.sendMessage("§c해당 플레이어를 찾을 수 없습니다");
                return false;
            }

            if (target.equals(p)) {
                return false;
            }

            if (!partys.containsKey(target.getUniqueId())) {
                p.sendMessage("§c해당 플레이어는 파티 리더가 아닙니다");
                return false;
            }


            String kitName = args[2];
            if (!kitManager.exists(kitName)) {
                sender.sendMessage(ChatColor.RED + "없는 키트입니다: " + kitName);
                return false;
            }

            Kit kit = kitManager.get(kitName);

            List<Player> teamA = idsToPlayers(p, partys.get(p.getUniqueId()));
            List<Player> teamB = idsToPlayers(p, partys.get(p.getUniqueId()));

            if (teamA.isEmpty() || teamB.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "파티 듀얼을 신청할 수 없습니다");
                return false;
            }

            int roundSetting = 1;

            if (args.length > 3) {
                roundSetting = Math.max(Integer.parseInt(args[2]), 1);
            }


            duelInbox.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>())
                    .put(p.getUniqueId(), new DuelRequest(p.getUniqueId(), System.currentTimeMillis() + EXPIRE_MS, kit, roundSetting));

            p.sendMessage("§a" + target.getName() + "§f님에게 파티 듀얼을 신청했습니다");
            Component msg = Component.text(p.getName(), NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("님에게 파티 듀얼을 신청받았습니다 (키트: §b§l" + kit.getDisplayName()  + "§f§l | 라운드: §e§l" + roundSetting + "§f§l)", NamedTextColor.WHITE, TextDecoration.BOLD))
                    .append(Component.text("\n\n[수락하기]", NamedTextColor.GREEN,  TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand("/" + label + " duelaccept " + p.getName()))
                            .hoverEvent(HoverEvent.showText(Component.text("클릭하여 수락하기"))))
                    .append(Component.text("  [거절하기]", NamedTextColor.RED,  TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand("/" + label + " dueldeny " + p.getName()))
                            .hoverEvent(HoverEvent.showText(Component.text("클릭하여 거절하기"))));
            target.sendMessage(msg);

        } else if (subCommand.equals("duelaccept")) {
            if (args.length < 2) {
                return false;
            }

            Player leader = Bukkit.getPlayer(args[1]);
            if (leader == null) { p.sendMessage("§c리더가 오프라인이거나 존재하지 않습니다"); return false; }

            Map<UUID, DuelRequest> map = duelInbox.getOrDefault(p.getUniqueId(), Collections.emptyMap());
            DuelRequest duelRequest = map.get(leader.getUniqueId());
            if (duelRequest == null) { p.sendMessage("§c해당 리더의 듀얼 신청이 없습니다"); return false; }
            if (duelRequest.expired()) {
                map.remove(leader.getUniqueId());
                if (map.isEmpty()) duelInbox.remove(p.getUniqueId());
                p.sendMessage("§f듀얼 신청이 §c만료§f되었습니다");
                return false;
            }


            List<Player> teamA = idsToPlayers(p, partys.get(p.getUniqueId()));
            List<Player> teamB = idsToPlayers(p, partys.get(leader.getUniqueId()));

            if (teamA.isEmpty() || teamB.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "파티 듀얼을 실행할 수 없습니다");
                return false;
            }


            // 안내

            leader.sendMessage("§a" + p.getName() + "§f님이 듀얼 신청을 수락했습니다");


            duelManager.startDuel(teamA, teamB, duelRequest.kit, duelRequest.roundSetting, true);
            // 인박스 정리
            map.remove(leader.getUniqueId());
            if (map.isEmpty()) duelInbox.remove(p.getUniqueId());
            return true;
        } else if (subCommand.equals("dueldeny")) {
            if (args.length < 2) {
                return true;
            }

            // 내 인박스 확인
            Map<UUID, DuelRequest> map = duelInbox.get(p.getUniqueId());
            if (map == null || map.isEmpty()) {
                p.sendMessage("§c대기 중인 듀얼 신청이 없습니다");
                return true;
            }

            // 리더 찾기 (온라인 우선, 없으면 인박스 키들로 오프로 매칭)
            String targetName = args[1];
            Player leaderOnline = Bukkit.getPlayerExact(targetName);

            if (leaderOnline == null) {
                p.sendMessage("§c해당 리더의 듀얼 신청을 찾을 수 없습니다");
                return true;
            }

            DuelRequest duelRequest = map.get(leaderOnline.getUniqueId());
            if (duelRequest == null) {
                p.sendMessage("§c해당 리더의 듀얼 신청이 없습니다");
                return true;
            }

            if (duelRequest.expired()) {
                map.remove(leaderOnline.getUniqueId());
                if (map.isEmpty()) duelInbox.remove(p.getUniqueId());
                p.sendMessage("§f듀얼 신청이 §c만료§f되어 제거되었습니다");
                return true;
            }

            // 거절 처리
            map.remove(leaderOnline.getUniqueId());
            if (map.isEmpty()) duelInbox.remove(p.getUniqueId());

            p.sendMessage("§c듀얼 신청을 거절했습니다");
            leaderOnline.sendMessage("§e" + p.getName() + "§f님이 듀얼 신청을 거절했습니다");

            return true;
        } else {
            return false;
        }

        return true;
    }

    public List<Player> idsToPlayers(Player leader, Set<UUID> ids) {
        List<Player> players = new ArrayList<>();

        for (UUID id :ids) {
            OfflinePlayer offlinePartyPlayer = Bukkit.getPlayer(id);

            if (offlinePartyPlayer == null) {
                return new ArrayList<>();
            }

            if (offlinePartyPlayer.isOnline()) {
                Player partyPlayer = (Player) offlinePartyPlayer;
                players.add(partyPlayer);
                if (duelManager.isInDuel(partyPlayer)) {
                    leader.sendMessage("§c" + partyPlayer + "§f님이 듀얼 중입니다");
                    return new ArrayList<>();
                }
            } else {
                leader.sendMessage("§c" + offlinePartyPlayer.getName() + "§f님이 오프라인입니다");
                return new ArrayList<>();
            }
        }

        return players;
    }

    public String joinAnyNames(Set<UUID> ids) {
        return ids.stream()
                .map(id -> {
                    var op = Bukkit.getOfflinePlayer(id);
                    String name = op.getName();       // null일 수 있음(한 번도 접속 안했을 때 등)
                    return (name != null) ? name : id.toString().substring(0, 8);
                })
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
    }
}
