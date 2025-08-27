package me.ujun.pvpWorld.saving;

import me.ujun.pvpWorld.PvpWorld;
import me.ujun.pvpWorld.arena.ArenaManager;
import me.ujun.pvpWorld.arena.ArenaMeta;
import me.ujun.pvpWorld.kit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenasFile {
    private final File file;
    private final File schemDir;
    private final FileConfiguration config;
    private final KitManager kitManager;

    // 메모리 내 아레나 메타

    public ArenasFile(File dataFolder, KitManager kitManager) {
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.file = new File(dataFolder, "arenas.yml");
        if (!file.exists()) try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }

        this.schemDir = new File(dataFolder, "arenas");
        this.schemDir.mkdirs();

        this.config = YamlConfiguration.loadConfiguration(file);
        this.kitManager = kitManager;
    }

    /* ------------ 저장 ------------ */
    public void save() {
        // 섹션 초기화 후 다시 씀
        config.set("arenas", null);
        config.set("ffa", null);

        // arenas
        for (ArenaMeta m : ArenaManager.arenas.values()) {
            String base = "arenas." + m.name();
            config.set(base + ".type", m.type());
            config.set(base + ".display", m.display());
            config.set(base + ".sizeX", m.sizeX());
            config.set(base + ".sizeY", m.sizeY());
            config.set(base + ".sizeZ", m.sizeZ());
            setPoint(base + ".points.spawn1", m.spawn1());
            setPoint(base + ".points.spawn2", m.spawn2());
            setPoint(base + ".points.center", m.center());
            config.set(base + ".schem", m.schem());
        }

        // ffa (기존 맵 유지)
        for (String kitName : PvpWorld.ffaSpawnLocations.keySet()) {
            Location loc = PvpWorld.ffaSpawnLocations.get(kitName);
            config.set("ffa." + kitName + ".spawn", loc);
        }

        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    private void setPoint(String path, ArenaMeta.Point p) {
        if (p == null) return;
        config.set(path + ".dx", p.dx());
        config.set(path + ".dy", p.dy());
        config.set(path + ".dz", p.dz());
        config.set(path + ".yaw", p.yaw());
        config.set(path + ".pitch", p.pitch());
    }

    /* ------------ 로드 ------------ */
    public void load() {
        ArenaManager.arenas.clear();
        PvpWorld.ffaSpawnLocations.clear();


        ConfigurationSection root = config.getConfigurationSection("arenas");
        if (root == null) return;

        for (String name : root.getKeys(false)) {
            String base = "arenas." + name;
            String display = config.getString(base + ".display", name);
            String type = config.getString(base + ".type", "default");
            int sx = config.getInt(base + ".sizeX", 0);
            int sy = config.getInt(base + ".sizeY", 0);
            int sz = config.getInt(base + ".sizeZ", 0);
            ArenaMeta.Point s1 = getPoint(base + ".points.spawn1");
            ArenaMeta.Point s2 = getPoint(base + ".points.spawn2");
            ArenaMeta.Point c  = getPoint(base + ".points.center");
            String schem = config.getString(base + ".schem", name + ".schem");

            ArenaManager.arenas.put(name.toLowerCase(Locale.ROOT),
                    new ArenaMeta(name, display, type, sx, sy, sz, s1, s2, c, schem));
        }

        // ffa
        ConfigurationSection froot = config.getConfigurationSection("ffa");
        if (froot != null) {
            for (String kitName : froot.getKeys(false)) {
                Location loc = (Location) froot.get(kitName + ".spawn");
                if (loc == null) continue;
                if (kitManager.exists(kitName)) {
                    PvpWorld.ffaSpawnLocations.put(kitName, loc);
                }
            }
        }
    }

    private ArenaMeta.Point getPoint(String path) {
        if (!config.isConfigurationSection(path)) return null;
        int dx = config.getInt(path + ".dx", 0);
        int dy = config.getInt(path + ".dy", 0);
        int dz = config.getInt(path + ".dz", 0);
        float yaw = (float) config.getDouble(path + ".yaw", 0.0);
        float pitch = (float) config.getDouble(path + ".pitch", 0.0);
        return new ArenaMeta.Point(dx, dy, dz, yaw, pitch);
    }

    // ArenasFile.java
    public File schemFile(String nameOrFile) {
        String base = nameOrFile.toLowerCase(Locale.ROOT);
        if (!base.endsWith(".schem")) base += ".schem";
        return new File(schemDir, base);
    }

}