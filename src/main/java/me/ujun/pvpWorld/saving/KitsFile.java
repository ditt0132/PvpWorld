package me.ujun.pvpWorld.saving;

import me.ujun.pvpWorld.kit.Kit;
import me.ujun.pvpWorld.kit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitsFile {
    private final File file;
    private final FileConfiguration config;

    public KitsFile(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs(); // 폴더 먼저 생성
        }

        this.file = new File(dataFolder, "kits.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {


        ConfigurationSection root = config.createSection("kits");
        for (Kit kit : KitManager.kits.values()) {
            ConfigurationSection sec = root.createSection(kit.getName());
            sec.set("display", kit.getDisplayName());
            sec.set("type", kit.getType());
            sec.set("time", kit.getDuelTime());
            sec.set("items", kit.getContents());
//            Bukkit.getLogger().info(kit.getName());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        KitManager.kits.clear();

        ConfigurationSection root = config.getConfigurationSection("kits");
        if (root == null) {
            Bukkit.getLogger().info("No kits found.");
            return;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;

            String display = sec.getString("display", key);
            String type = sec.getString("type", "default");
            int time = sec.getInt("time", 600);
            List<?> list = sec.getList("items", Collections.emptyList());

            ItemStack[] arr = new ItemStack[Kit.GUI_SIZE];
            for (int i = 0; i < Math.min(arr.length, list.size()); i++) {
                Object o = list.get(i);
                if (o instanceof ItemStack) arr[i] = (ItemStack) o;
            }

            KitManager.kits.put(key.toLowerCase(Locale.ROOT), new Kit(key, display, type, arr, time));
        }

        Bukkit.getLogger().info("Loaded kits: " + KitManager.kits.size());
    }

}
