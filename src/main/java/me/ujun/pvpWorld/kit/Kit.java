package me.ujun.pvpWorld.kit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Objects;

public class Kit {
    private final String name;
    private String displayName;
    private String type = "default";
    private ItemStack[] contents;

    public static final int GUI_SIZE = 54;

    public Kit(String name, String displayName, String type, ItemStack[] contents) {
        this.name = name;
        this.displayName = displayName;
        this.type = type;
        this.contents = normalize(contents);
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public ItemStack[] getContents() { return contents; }
    public void setContents(ItemStack[] contents) { this.contents = normalize(contents); }

    public Inventory createEditorInventory() {
        String title = ChatColor.DARK_GRAY + "Kit 편집: " + ChatColor.DARK_RED + displayName + ChatColor.DARK_GRAY + " [" + name + "|" + type + "]";
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, title);
        inv.setContents(contents);

        put(inv, KitLayout.UI_BOOTS,   contents[36]); // boots
        put(inv, KitLayout.UI_LEG,     contents[37]); // leggings
        put(inv, KitLayout.UI_CHEST,   contents[38]); // chest
        put(inv, KitLayout.UI_HELMET,  contents[39]); // helmet
        put(inv, KitLayout.UI_OFFHAND, contents[40]); // offhand



        inv.setItem(9, pane(DyeColor.ORANGE, ChatColor.GRAY + "feet" ));
        inv.setItem(10, pane(DyeColor.ORANGE,ChatColor.GRAY +  "legs"));
        inv.setItem(11, pane(DyeColor.ORANGE, ChatColor.GRAY + "chest"));
        inv.setItem(12, pane(DyeColor.ORANGE, ChatColor.GRAY +  "head"));
        inv.setItem(13, pane(DyeColor.ORANGE, ChatColor.GRAY +  "offhand"));



        ItemStack sep = pane(DyeColor.LIGHT_BLUE, ChatColor.GRAY + "");
        for (int s = 14; s <= 17; s++) inv.setItem(s, sep);

        ItemStack blocked = pane(DyeColor.GRAY, ChatColor.DARK_GRAY + "");
        for (int s : KitLayout.UI_UNUSED_TOP) inv.setItem(s, blocked);

        // 3) 인벤 3줄(캐논컬 9..35 → UI 18..44)
        for (int i = 0; i < KitLayout.UI_INV_3ROWS.length; i++) {
            int ui = KitLayout.UI_INV_3ROWS[i];
            int canon = KitLayout.CANON_INV_3ROWS[i];
            inv.setItem(ui, contents[canon]);
        }

        // 4) 핫바(캐논컬 0..8 → UI 45..53)
        for (int i = 0; i < KitLayout.UI_HOTBAR.length; i++) {
            int ui = KitLayout.UI_HOTBAR[i];
            int canon = KitLayout.CANON_HOTBAR[i];
            inv.setItem(ui, contents[canon]);
        }
        return inv;
    }

    private ItemStack[] normalize(ItemStack[] src) {
        ItemStack[] arr = new ItemStack[GUI_SIZE];
        if (src != null) {
            System.arraycopy(src, 0, arr, 0, Math.min(src.length, GUI_SIZE));
        }
        return arr;
    }

    @Override
    public String toString() {
        return "Kit{" + name + ", display='" + displayName + "', items=" + Arrays.stream(contents).filter(Objects::nonNull).count() + "}";
    }

    public void updateFromEditor(Inventory inv) {
        // 장비/오프핸드
        contents[36] = inv.getItem(KitLayout.UI_BOOTS);
        contents[37] = inv.getItem(KitLayout.UI_LEG);
        contents[38] = inv.getItem(KitLayout.UI_CHEST);
        contents[39] = inv.getItem(KitLayout.UI_HELMET);
        contents[40] = inv.getItem(KitLayout.UI_OFFHAND);

        // 인벤 3줄
        for (int i = 0; i < KitLayout.UI_INV_3ROWS.length; i++) {
            int ui = KitLayout.UI_INV_3ROWS[i];
            int canon = KitLayout.CANON_INV_3ROWS[i];
            contents[canon] = inv.getItem(ui);
        }
        // 핫바
        for (int i = 0; i < KitLayout.UI_HOTBAR.length; i++) {
            int ui = KitLayout.UI_HOTBAR[i];
            int canon = KitLayout.CANON_HOTBAR[i];
            contents[canon] = inv.getItem(ui);
        }
    }

    // 도우미
    private void put(Inventory inv, int slot, ItemStack item) {
        if (item != null) inv.setItem(slot, item);
    }
    private ItemStack pane(DyeColor color, String name) {
        ItemStack it = new ItemStack(Material.valueOf(color.name() + "_STAINED_GLASS_PANE"));
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        it.setItemMeta(meta);
        return it;
    }
}
