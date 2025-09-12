package me.ujun.pvpWorld.duel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class DuelUtil {

    public void sendMessageToPlayers(String message, List<Player> players) {

        for (Player player : players) {
            player.sendMessage(message);

        }
    }

    public void sendMessageToPlayers(Component message, List<Player> players) {

        for (Player player : players) {
            player.sendMessage(message);

        }
    }

    public Set<UUID> getInstPlayers(Instance inst) {
        Set<UUID> instPlayers = new HashSet<>();
        instPlayers.addAll(inst.teamA);
        instPlayers.addAll(inst.teamB);

        return instPlayers;
    }

    public List<Player> getInstOnlinePlayers(Instance inst) {
        List<Player> players = new ArrayList<>();
        Set<UUID> playerIds = getInstPlayers(inst);

        for (UUID id : playerIds) {
            Player player = Bukkit.getPlayer(id);

            if (player != null) {
                players.add(player);
            }
        }

        return players;
    }

    public void sendTitleToPlayers(Instance inst, String message, int a, int b, int c) {
        for (Player player : getInstOnlinePlayers(inst)) {
            player.sendTitle(message, "", a, b, c);
        }
    }

    public void sendEndTitle(Instance inst, Set<UUID> winnerTeam) {
        String winnerNames = joinAnyNames(winnerTeam, NamedTextColor.RED);

        for (Player p : getInstOnlinePlayers(inst)) {

            if (winnerTeam.contains(p.getUniqueId())) {
                p.sendTitle(ChatColor.GREEN + "승리",  ChatColor.RED +  winnerNames + ChatColor.RESET + "님이 듀얼에서 승리했습니다");
            } else {
                p.sendTitle(ChatColor.RED + "패배", ChatColor.RED + winnerNames + ChatColor.RESET + "님이 듀얼에서 승리했습니다");
            }
        }


        Set<UUID> loserTeam = new HashSet<>();
        if (inst.type.equals("duel")) {
            if (winnerTeam.equals(inst.teamA)) {
                loserTeam = inst.teamB;
            } else {
                loserTeam = inst.teamA;
            }
        } else if (inst.type.equals("ffa")) {
            loserTeam = inst.teamA;

            for (UUID id :  inst.teamA) {
                if (winnerTeam.contains(id)) {
                    loserTeam.remove(id);
                    break;
                }
            }
        }

        Bukkit.broadcastMessage(ChatColor.RED + winnerNames + ChatColor.RESET + "님이 듀얼에서 승리했습니다(상대: " + ChatColor.AQUA   + joinAnyNames(loserTeam, NamedTextColor.AQUA) + ChatColor.RESET  + ")");
    }

    public void playSoundToPlayers(Instance inst, Sound sound, float a, float b) {
        for (Player player : getInstOnlinePlayers(inst)) {
            player.playSound(player.getLocation(), sound, a, b);
        }
    }


    public String joinAnyNames(List<Player> players, NamedTextColor color) {
        Set<UUID> ids = new HashSet<>();

        for (Player p : players) {
            ids.add(p.getUniqueId());
        }

        return joinAnyNames(ids, color);

    }

    public String joinAnyNames(Set<UUID> ids, NamedTextColor color) {
        return ids.stream()
                .map(id -> {
                    var op = Bukkit.getOfflinePlayer(id);
                    String name = op.getName();       // null일 수 있음(한 번도 접속 안했을 때 등)
                    return (name != null) ? name : id.toString().substring(0, 8);
                })
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining("§7, " + color));
    }
}
