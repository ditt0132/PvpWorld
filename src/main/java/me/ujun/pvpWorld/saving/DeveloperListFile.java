package me.ujun.pvpWorld.saving;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DeveloperListFile {
    private final File file;
    private final FileConfiguration config;

    public DeveloperListFile(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs(); // 폴더 먼저 생성
        }

        this.file = new File(dataFolder, "developers.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void saveSets(Set<UUID> devPlayers) {
        List<String> developerList = devPlayers.stream().map(UUID::toString).toList();

        config.set("developers", developerList);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSets(Set<UUID> devPlayers) {
        List<String> developerList = config.getStringList("developers");

        devPlayers.clear();

        developerList.forEach(s -> {
            try {
                devPlayers.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {}
        });

    }
}
