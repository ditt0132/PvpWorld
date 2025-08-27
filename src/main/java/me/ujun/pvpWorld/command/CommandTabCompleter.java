package me.ujun.pvpWorld.command;

import me.ujun.pvpWorld.PvpWorld;
import me.ujun.pvpWorld.arena.ArenaManager;
import me.ujun.pvpWorld.kit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class CommandTabCompleter implements TabCompleter {


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (command.getName().equals("pvpworld")) {
            if (args.length == 1) {
                return prefixed(Arrays.asList("reload", "setlobby", "kit", "dev", "set_ffa_spawn", "arena"), args[0]);
            } else if (args.length == 2) {
                if (args[0].equals("kit")) {
                    return prefixed(Arrays.asList("create", "delete", "edit", "save", "load", "apply", "list"), args[1]);
                } else if (args[0].equals("dev")) {
                    return prefixed(Arrays.asList("add", "remove", "list"), args[1]);
                } else if (args[0].equals("set_ffa_spawn")) {
                    return prefixed(KitManager.kits.keySet().stream().toList(), args[1]);
                } else if (args[0].equals("arena")) {
                    return prefixed(Arrays.asList("create", "mark", "register", "list", "delete", "cancel", "pastehere"), args[1]);
                }
            } else if (args.length == 3) {
                if (args[0].equals("kit")) {
                 if (args[1].equals("delete") || args[1].equals("edit")) {
                    return prefixed(KitManager.kits.keySet().stream().toList(), args[2]);
                 } else if (args[1].equals("apply")) {
                     return prefixed(getOnlinePlayerNames(), args[2]);
                    }
                } else if (args[0].equals("dev")) {
                    if (args[1].equals("add")) {
                        return prefixed(getOnlinePlayerNames(), args[2]);
                    } else if (args[1].equals("remove")) {
                        return prefixed(PvpWorld.devPlayers.stream()
                                .map(u -> {
                                    Player p = Bukkit.getPlayer(u);
                                    if (p != null) return p.getName();             // 온라인
                                    OfflinePlayer off = Bukkit.getOfflinePlayer(u);
                                    return off.getName();                          // 마지막으로 알려진 이름(없을 수도 있음)
                                })
                                .filter(Objects::nonNull)                          // 이름 모를 때(null) 제거
                                .collect(Collectors.toCollection(ArrayList::new)), args[2]);
                    }

                } else if (args[0].equals("arena")) {
                    if (args[1].equals("mark")) {
                        return prefixed(Arrays.asList("spawn1", "spawn2", "center"), args[2]);
                    } else if (args[1].equals("delete") || args[1].equals("pastehere")) {
                        return prefixed(ArenaManager.arenas.keySet().stream().toList() ,args[2]);
                    }
                }
            } else if (args.length == 4) {
                if (args[0].equals("kit")) {
                    if (args[1].equals("apply")) {
                        return prefixed(KitManager.kits.keySet().stream().toList(), args[3]);
                    } else if (args[1].equals("edit")) {
                        return prefixed(Arrays.asList("name", "type", "inventory"), args[3]);
                    }
                }
            }
        } else if (command.getName().equals("ffa")) {
            if (args.length == 1) {
                return prefixed(KitManager.kits.keySet().stream().toList(), args[0]);
            }
        } else if (command.getName().equals("duel")) {
            if (args.length == 1) {
                return prefixed(getOnlinePlayerNames(), args[0]);
            } else if (args.length == 2) {
                return prefixed(KitManager.kits.keySet().stream().toList(),args[1]);
            }
        } else if (command.getName().equals("party")) {
            if (args.length == 1) {
                return prefixed(Arrays.asList("create", "disband", "leader", "leave", "invite", "accept", "list", "ffa", "duel", "kick"),args[0]);
            } else if (args.length == 2) {
                if (args[0].equals("leader") || args[0].equals("accept") || args[0].equals("invite") || args[0].equals("deny") || args[0].equals("kick") || args[0].equals("duel")) {
                    return prefixed(getOnlinePlayerNames(), args[1]);
                } else if (args[0].equals("ffa")) {
                    return prefixed(KitManager.kits.keySet().stream().toList(), args[1]);
                }
            } else if (args.length == 3) {
                if (args[0].equals("duel")) {
                    return prefixed(KitManager.kits.keySet().stream().toList(), args[2]);
                }
            }
        }


        return Collections.emptyList();

    }


    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<String> prefixed(List<String> items, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return items.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(p))
                .sorted()
                .collect(Collectors.toList());
    }
}
