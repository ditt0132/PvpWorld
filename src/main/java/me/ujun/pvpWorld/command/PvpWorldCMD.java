package me.ujun.pvpWorld.command;

import me.ujun.pvpWorld.PvpWorld;
import me.ujun.pvpWorld.arena.ArenaManager;
import me.ujun.pvpWorld.arena.ArenaMeta;
import me.ujun.pvpWorld.config.ConfigHandler;
import me.ujun.pvpWorld.duel.DuelManager;
import me.ujun.pvpWorld.duel.Instance;
import me.ujun.pvpWorld.saving.ArenasFile;
import me.ujun.pvpWorld.saving.KitsFile;
import me.ujun.pvpWorld.kit.Kit;
import me.ujun.pvpWorld.kit.KitManager;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

public class PvpWorldCMD implements CommandExecutor {
    private final JavaPlugin plugin;
    private final KitManager kitManager;
    private final KitsFile kitsFile;
    private final ArenaManager arenaManager;
    private final DuelManager duel;


    public PvpWorldCMD(JavaPlugin plugin, KitManager kitManager, KitsFile kitsFile, ArenaManager arenaManager, DuelManager duel) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        this.kitsFile = kitsFile;
        this.arenaManager = arenaManager;
        this.duel = duel;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        String subCommand = args[0];

        if (subCommand.equals("reload")) {
            sender.sendMessage(ChatColor.GREEN + "구성 설정을 리로드했습니다!");
            plugin.reloadConfig();
            ConfigHandler.getInstance().loadConfig();
        } else if (subCommand.equals("setlobby")) {
            if (sender instanceof Player player) {
                sender.sendMessage(ChatColor.GOLD + "로비를 설정했습니다.");
                ConfigHandler.lobby = player.getLocation();
            }
        } else if (subCommand.equals("dev")) {
            if (args.length < 2) {
                return false;
            }

            if (args[1].equals("list")) {
                sender.sendMessage("개발자 목록:");

                for (UUID uuid : PvpWorld.devPlayers) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

                    sender.sendMessage( ChatColor.WHITE + "- " + ChatColor.GOLD +  offlinePlayer.getName());
                }

                return true;
            } else {
                if (args.length < 3) {
                    return false;
                }

                Player target = Bukkit.getPlayer(args[2]);

                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "존재하지 않는 플레이어");
                    return false;
                }

                if (args[1].equals("add")) {
                    if (PvpWorld.devPlayers.contains(target.getUniqueId())) {
                        sender.sendMessage(ChatColor.RED + "이미 개발자 목록에 존재합니다.");
                        return false;
                    }

                    sender.sendMessage(  ChatColor.GOLD + target.getName() + "님을 개발자 목록에 추가했습니다.");
                    PvpWorld.devPlayers.add(target.getUniqueId());
                } else if (args[1].equals("remove")) {
                    if (!PvpWorld.devPlayers.contains(target.getUniqueId())) {
                        sender.sendMessage(ChatColor.RED + "개발자 목록에 존재하지 않습니다.");
                        return false;
                    }

                    sender.sendMessage(ChatColor.GOLD + target.getName() + "님을 개발자 목록에서 제거했습니다.");
                    PvpWorld.devPlayers.remove(target.getUniqueId());

                    if (target.getGameMode().equals(GameMode.CREATIVE) || target.getGameMode().equals(GameMode.SPECTATOR)) {
                        target.setGameMode(GameMode.ADVENTURE);
                    }
                } else {
                    return false;
                }
            }
        } else if (subCommand.equals("kit")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("플레이어만 사용 가능합니다.");
                return false;
            }
            if (args.length < 2) {
                return false;
            }

            String kitSubCommand = args[1];

            if (kitSubCommand.equals("create")) {
                if (args.length < 4) {
                    return false;
                }

                String name = args[2].toLowerCase(Locale.ROOT);
                String displayName = ChatColor.translateAlternateColorCodes('&',
                        String.join(" ", Arrays.copyOfRange(args, 3, args.length)));

                if (kitManager.exists(name)) {
                    sender.sendMessage(ChatColor.RED + "이미 존재하는 키트입니다: " + name);
                    return false;
                }

                Kit kit = kitManager.createEmpty(name, displayName);
                Inventory editor = kit.createEditorInventory();

                kitManager.openEditor(player.getUniqueId(), kit);

                player.sendMessage(kitManager.getEditingKitName(player.getUniqueId()).toString());

                player.openInventory(editor);
            } else if (kitSubCommand.equals("delete")) {
                if (args.length < 3) {
                    return false;
                }

                String name = args[2].toLowerCase(Locale.ROOT);
                if (!kitManager.exists(name)) {
                    sender.sendMessage(ChatColor.RED + "없는 키트입니다: " + name);
                    return false;
                }
                kitManager.delete(name);
                sender.sendMessage(ChatColor.GREEN + "키트를 제거했습니다: " + name);
                return true;
            } else if (kitSubCommand.equals("edit")) {
                if (args.length < 4) {
                    return false;
                }

                String name = args[2].toLowerCase(Locale.ROOT);
                String kitEditSubCommand = args[3];
                Kit kit = kitManager.get(name);
                if (kit == null) {
                    sender.sendMessage(ChatColor.RED + "없는 키트입니다: " + name);
                    return false;
                }

                if (kitEditSubCommand.equals("inventory")) {
                    kitManager.openEditor(player.getUniqueId(), kit);
                    player.openInventory(kit.createEditorInventory());

                } else if (kitEditSubCommand.equals("name")) {
                    if (args.length < 5) {
                        return false;
                    }

                    String newDisplay = ChatColor.translateAlternateColorCodes('&',
                            String.join(" ", Arrays.copyOfRange(args, 4, args.length)));
                    kit.setDisplayName(newDisplay);
                    kitManager.setEditingDisplay(player.getUniqueId(), newDisplay);

                    player.sendMessage(ChatColor.GREEN + "디스플레이 이름을 " + newDisplay + "으로 변경했습니다.");
                } else if (kitEditSubCommand.equals("type")) {
                    if (args.length < 5) {
                        return false;
                    }

                    String newType = args[4];
                    kit.setType(newType);
                    player.sendMessage(ChatColor.GREEN + "유형을 " + newType + "으로 변경했습니다.");
                } else if (kitEditSubCommand.equals("time")) {
                    if (args.length < 5) {
                        return false;
                    }

                    int newTime = Integer.parseInt(args[4]);
                    kit.setDuelTime(newTime);
                    player.sendMessage(ChatColor.GREEN + "제한 시간을 " + newTime + "으로 변경했습니다.");

                } else {
                    return false;
                }
            } else if (kitSubCommand.equals("apply")) {
                if (args.length < 4) {
                    return false;
                }

                Player target = Bukkit.getPlayer(args[2]);
                String name = args[3];

                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "존재하지 않는 플레이어");
                    return false;
                }

                if (!kitManager.exists(name)) {
                    sender.sendMessage(ChatColor.RED + "없는 키트입니다: " + name);
                    return false;
                }

                Kit kit = kitManager.get(name);

                kitManager.applyTo(player, kit, true, true);
                player.sendMessage(  ChatColor.GOLD +  player.getName() + "에게 " + name + " 키트를 적용했습니다.");
            } else if (kitSubCommand.equals("save")) {
                kitsFile.save();
                sender.sendMessage(ChatColor.GREEN + "kits.yml에 키트를 저장했습니다");
            } else  if (kitSubCommand.equals("list")) {
                sender.sendMessage("키트 목록:");

                for (Kit kit : KitManager.kits.values()) {
                    String kitName = kit.getName();
                    String kitDisplayName = kit.getDisplayName();
                    String kitType = kit.getType();

                    sender.sendMessage( ChatColor.WHITE + "- " + ChatColor.GOLD +  kitDisplayName + ChatColor.GRAY + " [" + kitName + "|" + kitType + "]" );
                }

                return true;
            }
        } else if (subCommand.equals("set_ffa_spawn")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("플레이어만 사용 가능합니다.");
                return false;
            }

            if (args.length < 2) {
                return false;
            }

            String name = args[1].toLowerCase(Locale.ROOT);
            Kit kit = kitManager.get(name);
            if (kit == null) {
                sender.sendMessage(ChatColor.RED + "없는 키트입니다: " + name);
                return false;
            }

            PvpWorld.ffaSpawnLocations.put(name, player.getLocation());
            player.sendMessage(ChatColor.GOLD + name + " 키트의 FFA 스폰 위치를 설정했습니다.");
        } else if (subCommand.equals("arena")) {
            if (args.length < 2) {
                return false;
            }

            String arenaSubCommand = args[1];

            if (arenaSubCommand.equals("create")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("플레이어만 사용 가능합니다.");
                    return false;
                }

                if (args.length < 3) {
                    return false;
                }

                String name = args[2].toLowerCase(Locale.ROOT);
                String displayName= name;

                if (args.length > 3) {
                    displayName = ChatColor.translateAlternateColorCodes('&',
                            String.join(" ", Arrays.copyOfRange(args, 3, args.length)));
                }
                if (arenaManager.exists(name)) { player.sendMessage("§c이미 존재: "+name); return false; }
                arenaManager.beginCreate(player.getUniqueId(), name, displayName);
                player.sendMessage("§aWorldEdit로 영역을 선택하세요(//wand).");
                player.sendMessage("§a스폰/센터 지정: §e/pvpworld arena mark <spawn1|spawn2|center>");
                player.sendMessage("§a등록: §e/pvpworld arena register");
                return true;
            } else if (arenaSubCommand.equals("mark")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("플레이어만 사용 가능합니다.");
                    return false;
                }

                if (args.length < 3) {
                    return false;
                }

                String which = args[2];
                boolean ok = arenaManager.mark(player.getUniqueId(), player, which);
                player.sendMessage(ok ? "§a" + which + " 설정 완료." : "§c마크 실패(세션/선택/인자 확인).");
                return true;
            } else if (arenaSubCommand.equals("register")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("플레이어만 사용 가능합니다.");
                    return false;
                }

                String type = "default";
                if (args.length > 2) {
                    type = args[2];
                }

                boolean ok = arenaManager.register(player.getUniqueId(), player, type);
                player.sendMessage(ok ? "§a아레나 등록 완료." : "§c등록 실패(선택/마크 확인).");
                return true;
            } else if (arenaSubCommand.equals("list")) {
                var list = arenaManager.all();
                if (list.isEmpty()) { sender.sendMessage("§7등록된 아레나가 없습니다."); return false; }
                sender.sendMessage("§6[Arenas]");
                for (ArenaMeta m : list) {
                    sender.sendMessage("§e- " + m.name() + " §7(" + ChatColor.RESET + m.display() + "§7) " +
                            "size: " + m.sizeX()+"x"+m.sizeY()+"x"+m.sizeZ() + "  schem: " + m.schem());
                }
                return true;
            } else if (arenaSubCommand.equals("delete")) {

                if (args.length < 3) {
                    return false;
                }

                boolean ok = arenaManager.delete(args[2].toLowerCase(Locale.ROOT));
                sender.sendMessage(ok ? "§a삭제됨: " + args[2] : "§c없는 아레나: " + args[2]);
                return true;
            } else if (arenaSubCommand.equals("cancel")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("플레이어만 사용 가능합니다.");
                    return false;
                }

                arenaManager.cancel(player.getUniqueId());
                player.sendMessage("§7생성 세션을 취소했습니다.");
                return true;
            } else if (arenaSubCommand.equals("pastehere")) {


                if (!(sender instanceof Player player)) {
                    sender.sendMessage("플레이어만 사용 가능합니다.");
                    return false;
                }

                if (args.length < 3) {
                    return false;
                }

                String name = args[2].toLowerCase(Locale.ROOT);

                boolean ok = arenaManager.pasteHere(player, name.toLowerCase(Locale.ROOT));
                player.sendMessage(ok ? "§a현재 위치에 붙여넣었습니다." : "§c붙여넣기 실패(아레나/파일 확인).");
                return true;
            }
        } else if (subCommand.equals("duel")) {
            if (args.length < 2) {
                return false;
            }

            String duelSubCommand = args[1];

            if (duelSubCommand.equals("shutdown")) {
                Player target = Bukkit.getPlayer(args[2]);

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

                if (inst.isShuttingDown) {
                    sender.sendMessage("게임이 종료되는 중");
                    return false;
                }

                sender.sendMessage("해당 플레이어가 참여한 듀얼을 종료시킵니다");
                duel.endInternal(inst);
                duel.byPlayer.remove(target.getUniqueId());
            }
        }

        return true;
    }
}
