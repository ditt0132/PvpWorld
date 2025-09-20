package me.ujun.pvpWorld;

import me.ujun.pvpWorld.arena.ArenaAllocator;
import me.ujun.pvpWorld.arena.ArenaManager;
import me.ujun.pvpWorld.command.PartyCMD;
import me.ujun.pvpWorld.arena.VoidManager;
import me.ujun.pvpWorld.command.*;
import me.ujun.pvpWorld.config.ConfigHandler;
import me.ujun.pvpWorld.duel.DuelManager;
import me.ujun.pvpWorld.duel.DuelUtil;
import me.ujun.pvpWorld.listener.*;
import me.ujun.pvpWorld.saving.ArenasFile;
import me.ujun.pvpWorld.saving.DeveloperListFile;
import me.ujun.pvpWorld.saving.KitsFile;
import me.ujun.pvpWorld.kit.Kit;
import me.ujun.pvpWorld.kit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class PvpWorld extends JavaPlugin {
    public static HashMap<UUID, Integer> pvpPlayerTimer = new HashMap<>();
    public static HashMap<UUID, Set<UUID>> lastAttackers = new HashMap<>();
    public static HashMap<UUID, Kit> playerKits = new HashMap<>();
    public static HashMap<String, Location> ffaSpawnLocations = new HashMap<>();
    public static HashSet<UUID> devPlayers = new HashSet<>();


    private DeveloperListFile developerListFile;
    private KitsFile kitsFile;
    private ArenasFile arenasFile;
    private final KitManager kitManager = new KitManager();
    private ArenaManager arenaManager;
    private DuelManager duelManager;
    private final DuelUtil duelUtil = new DuelUtil();


    private final VoidManager voidWorld = new VoidManager("pvpworld_void");
    private final ArenaAllocator allocator = new ArenaAllocator(200, 200, 0);
    

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigHandler.init(this);

        developerListFile = new DeveloperListFile(getDataFolder());
        developerListFile.loadSets(devPlayers);

        kitsFile = new KitsFile(getDataFolder());
        kitsFile.load();

        arenasFile = new ArenasFile(getDataFolder(), kitManager);
        arenasFile.load();

        arenaManager = new ArenaManager(arenasFile);
        duelManager = new DuelManager(arenaManager, arenasFile, voidWorld, allocator, kitManager, duelUtil, this);

        voidWorld.ensure();

       registerCommands();
       registerListeners();


        run();
    }

    @Override
    public void onDisable() {
        getConfig().set("lobby", ConfigHandler.lobby);
        saveConfig();
        developerListFile.saveSets(devPlayers);
        kitsFile.save();
        arenasFile.save();
    }


    private void registerCommands() {
        CommandTabCompleter commandTabCompleter = new CommandTabCompleter(duelManager);
        ForceDuelCMD forceDuelCMD = new ForceDuelCMD(kitManager, duelManager, this);

        getCommand("lobby").setExecutor(new LobbyCMD());
        getCommand("pvpworld").setExecutor(new PvpWorldCMD(this, kitManager, kitsFile, arenaManager, duelManager));
        getCommand("ffa").setExecutor(new FfaCMD(kitManager));
        getCommand("duel").setExecutor(new DuelCMD(kitManager, duelManager));
        getCommand("party").setExecutor(new PartyCMD(kitManager, duelManager));
        getCommand("forceduel").setExecutor(forceDuelCMD);
        getServer().getPluginManager().registerEvents(forceDuelCMD, this);
        getCommand("duelwatch").setExecutor(new DuelWatchCMD(duelManager));
        getCommand("leave").setExecutor(new LeaveCMD(duelManager));

        getCommand("lobby").setTabCompleter(commandTabCompleter);
        getCommand("pvpworld").setTabCompleter(commandTabCompleter);
        getCommand("ffa").setTabCompleter(commandTabCompleter);
        getCommand("duel").setTabCompleter(commandTabCompleter);
        getCommand("party").setTabCompleter(commandTabCompleter);
        getCommand("forceduel").setTabCompleter(commandTabCompleter);
        getCommand("duelwatch").setTabCompleter(commandTabCompleter);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new FfaListener(this, kitManager), this);
        getServer().getPluginManager().registerEvents(new FoodChangeListener(), this);
        getServer().getPluginManager().registerEvents(new BlockCommandListener(duelManager), this);
        getServer().getPluginManager().registerEvents(new JoinPvpWorldListener(duelManager), this);
        getServer().getPluginManager().registerEvents(new KitEditorListener(kitManager), this);
        getServer().getPluginManager().registerEvents(new DuelListener(duelManager), this);

    }

    private void run() {
        new BukkitRunnable() {
            @Override
            public void run() {

                Iterator<UUID> iterator = pvpPlayerTimer.keySet().iterator();
                while (iterator.hasNext()) {
                    UUID uuid = iterator.next();

                    int sec = pvpPlayerTimer.get(uuid);
                    Player player = Bukkit.getPlayer(uuid);
                    String fightMessage = ConfigHandler.fightMessage;
                    fightMessage = fightMessage.replace("%sec%", String.valueOf(sec));

                    if (sec == 0) {
                        if (player != null) {
                            player.sendActionBar(ConfigHandler.fightEndMessage);
                        }
                        iterator.remove();
                        continue;
                    } else {
                        if (player != null) {
                            player.sendActionBar(fightMessage);
                        }
                    }

                    pvpPlayerTimer.put(uuid, sec-1);
                }
            }
        }.runTaskTimer(this, 0L, 20L);

    }
}
