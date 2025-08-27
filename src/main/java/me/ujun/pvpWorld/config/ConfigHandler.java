package me.ujun.pvpWorld.config;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ConfigHandler {
    private final JavaPlugin plugin;
    private static ConfigHandler instance;

    public static String fightMessage;
    public static String fightEndMessage;
    public static int fightTime;
    public static List<String> blockedCommandsInFight = new ArrayList<>();
    public static String ffaTag;
    public static String pvpWorld;
    public static Location lobby;
    public static List<String> allowedCommandsInPvpWorld = new ArrayList<>();



    public ConfigHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ConfigHandler getInstance() {
        return instance;
    }

    public static void init(JavaPlugin plugin) {
        instance = new ConfigHandler(plugin);
        instance.loadConfig();
    }


    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        fightMessage = config.getString("fight_message", "%sec% sec remain fighting mode!");
        fightEndMessage = config.getString("fight_end_message", "fight end!");
        fightTime = config.getInt("fight_time", 15);
        blockedCommandsInFight = config.getStringList("blocked_commands_during_fight");
        ffaTag = config.getString("ffa_tag");
        pvpWorld = config.getString("pvp_world");
        lobby = config.getLocation("lobby");
        allowedCommandsInPvpWorld = config.getStringList("allowed_commands_in_pvp_world");

    }
}
