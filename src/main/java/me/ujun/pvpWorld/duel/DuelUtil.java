package me.ujun.pvpWorld.duel;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import org.bukkit.util.ChatPaginator;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class DuelUtil {

    private final Map<Player, Scoreboard> scoreboardMap = new HashMap<>();


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

    public List<Player> getInstWatchersAndPlayers(Instance inst) {
         List<Player> players  = getInstOnlinePlayers(inst);

         for (UUID id : inst.watchers) {
             Player p = Bukkit.getPlayer(id);
             players.add(p);
         }

         return players;
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
        for (Player player : getInstWatchersAndPlayers(inst)) {
            player.sendTitle(message, "", a, b, c);
        }
    }

    public void sendEndTitle(Instance inst, Set<UUID> winnerTeam) {
        String winnerNames = joinAnyNames(winnerTeam, "§c");

        for (Player p : getInstWatchersAndPlayers(inst)) {

            if (winnerTeam.contains(p.getUniqueId())) {
                p.sendTitle(ChatColor.GREEN + "승리",winnerNames + ChatColor.RESET + "님이 듀얼에서 승리했습니다");
            } else {
                if (inst.watchers.contains(p.getUniqueId())) {
                    p.sendTitle(ChatColor.RED + "", winnerNames + ChatColor.RESET + "님이 듀얼에서 승리했습니다");
                } else {
                    p.sendTitle(ChatColor.RED + "패배", winnerNames + ChatColor.RESET + "님이 듀얼에서 승리했습니다");
                }
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

        String loserNames = joinAnyNames(loserTeam, "§b");
        if (loserTeam.isEmpty()) {
            loserNames = "§cUnknown";
        }

        Bukkit.broadcastMessage(ChatColor.RED + winnerNames + ChatColor.RESET + "님이 듀얼에서 승리했습니다 (상대: " + loserNames + ChatColor.RESET  + ")");
    }

    public void playSoundToPlayers(Instance inst, Sound sound, float a, float b) {
        for (Player player : getInstWatchersAndPlayers(inst)) {
            player.playSound(player.getLocation(), sound, a, b);
        }
    }


    public String joinAnyNames(List<Player> players, String join) {
        Set<UUID> ids = new HashSet<>();

        for (Player p : players) {
            ids.add(p.getUniqueId());
        }

        return joinAnyNames(ids, join);

    }

    public String joinAnyNames(Set<UUID> ids, String join) {
        return join + ids.stream()
                .map(id -> {
                    var op = Bukkit.getOfflinePlayer(id);
                    String name = op.getName();       // null일 수 있음(한 번도 접속 안했을 때 등)
                    return (name != null) ? name : id.toString().substring(0, 8);
                })
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining("§7, " + join));
    }

    public void clearSidebar(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard mainBoard = manager.getMainScoreboard();

        player.setScoreboard(mainBoard);
        scoreboardMap.remove(player);
    }


    public void setSidebar(Player p, Instance inst) {

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (!scoreboardMap.containsKey(p)) {
            Scoreboard board = manager.getNewScoreboard();

            Objective objective = board.registerNewObjective("pvpworld_sidebar", "dummy",
                    Component.text("§6§l[ 듀얼 ]"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            Objective belowNameObjective = board.registerNewObjective("pvpworld_belowname", "health",
                    Component.text("§c❤"));
            belowNameObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);

            p.setScoreboard(board);
            scoreboardMap.put(p, board);
        }

        Scoreboard board = scoreboardMap.get(p);
        Objective objective = board.getObjective("pvpworld_sidebar");

        String leftTimer = String.format("%d:%02d", inst.leftTime / 60, inst.leftTime % 60);

        TextColor textColor = TextColor.color(244, 164, 96);
        List<Component> sidebar = new ArrayList<>();

        sidebar.add(Component.text("Kit: ").color(textColor).append(Component.text(inst.kit.getDisplayName()).color(NamedTextColor.WHITE)));
        sidebar.add(Component.text("Time left: ").color(textColor).append(Component.text(leftTimer).color(NamedTextColor.WHITE)));
        sidebar.add(Component.text("First to ").color(textColor).append(Component.text(inst.roundSetting).color(NamedTextColor.WHITE)));
        sidebar.add(Component.text(""));
        sidebar.add(Component.text("Score:").color(textColor));
        sidebar.add(Component.text(inst.top[0] + " §7-§r " + inst.top[1]));
        sidebar.add(Component.text(""));
        sidebar.add(Component.text("Opponent: ").color(textColor));


        if (inst.watchers.contains(p.getUniqueId())) {
            for (UUID id : inst.teamA) {
                sidebar.add(formatPlayerHealth(inst, id));
            }
            sidebar.add(Component.text("vs").color(textColor));
            for (UUID id : inst.teamB) {
                sidebar.add(formatPlayerHealth(inst, id));
            }
        } else {
            Set<UUID> opponentTeam = inst.teamA.contains(p.getUniqueId()) ? inst.teamB : inst.teamA;
            for (UUID id : opponentTeam) {
                sidebar.add(formatPlayerHealth(inst, id));
            }
        }

        Score score;
        int sidebarLength = sidebar.size();
        for (int i = 0; i < sidebarLength; i++) {
            score = objective.getScore("score" + i);
            score.setScore(sidebarLength - i);
            score.numberFormat(NumberFormat.blank());
            score.customName(sidebar.get(i));
        }
    }

    private Component formatPlayerHealth(Instance inst, UUID id) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(id);
        String healthInfo = "§cdead";

        DecimalFormat df = new DecimalFormat("#.##");

        if (offlinePlayer.isOnline() && !inst.eliminated.contains(id)) {
            Player player = (Player) offlinePlayer;
            healthInfo = "§c" + df.format(player.getHealth()) + "❤";
        }

        return Component.text(offlinePlayer.getName()).color(NamedTextColor.WHITE).append(Component.text(" " + healthInfo));
    }


}

//            if (inst.countdown) {
//                inst.countdown = false; //카운트다운 중이면 데미지 안받으니까 카운트다운 아니라고 구라 치기
//                p.damage(0.01); //데미지 조금 주기
//                inst.countdown = true; //다시 카운트다운으로 되돌리기
//                Bukkit.getScheduler().runTaskLater(plugin, () -> {
//                    p.setHealth(p.getMaxHealth());
//                }, 2L); //2틱 뒤에 플레이어 체력 원상복귀하기(딜레이 준 이유는 너무 빠르면 반영 안됨)
//            }
