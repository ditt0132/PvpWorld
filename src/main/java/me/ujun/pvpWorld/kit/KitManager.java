package me.ujun.pvpWorld.kit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class KitManager {
    public static final Map<String, Kit> kits = new HashMap<>(); // key = name (lowercase)
    private final Map<UUID, String> editing = new HashMap<>();
    private final Map<UUID, String> editingDisplay = new HashMap<>();


    public boolean exists(String name) {
        return kits.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public Kit get(String name) {
        return kits.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Kit> all() {
        return Collections.unmodifiableCollection(kits.values());
    }

    public Kit createEmpty(String name, String displayName) {
        String key = name.toLowerCase(Locale.ROOT);
        Kit k = new Kit(key, displayName, "default",  new ItemStack[Kit.GUI_SIZE]);
        kits.put(key, k);
        return k;
    }

    public boolean delete(String name) {
        return kits.remove(name.toLowerCase(Locale.ROOT)) != null;
    }

    public void openEditor(UUID playerId, Kit kit) {
        editing.put(playerId, kit.getName());
        editingDisplay.put(playerId, kit.getDisplayName());
    }

    public void closeEditor(UUID playerId) {
        editing.remove(playerId);
        editingDisplay.remove(playerId);
    }

    public Optional<String> getEditingKitName(UUID playerId) {
        return Optional.ofNullable(editing.get(playerId));
    }

    public Optional<String> getEditingDisplay(UUID playerId) {
        return Optional.ofNullable(editingDisplay.get(playerId));
    }

    public void setEditingDisplay(UUID playerId, String display) {
        editingDisplay.put(playerId, display);
    }

    public void applyTo(Player p, Kit kit, boolean clearInventory, boolean overwriteArmor) {
        PlayerInventory inv = p.getInventory();
        ItemStack[] src = kit.getContents();

        // 1) 초기화 옵션
        if (clearInventory) {
            inv.clear();
            inv.setArmorContents(null);
            inv.setItemInOffHand(null);
        }

        // 2) 메인 인벤토리 0~35 (정확 슬롯 배치)
        for (int slot = 0; slot <= 35 && slot < src.length; slot++) {
            ItemStack it = src[slot];
            if (isAir(it)) continue;
            safeSet(inv, p, slot, it, /*overwrite=*/true); // 메인칸은 덮어쓰는 게 보통 자연스러움
        }

        // 3) 방어구/오프핸드
        if (src.length > 36 && !isAir(src[36])) { // boots
            if (overwriteArmor || isAir(inv.getBoots())) inv.setBoots(src[36].clone());
            else dropOverflow(p, src[36]);
        }
        if (src.length > 37 && !isAir(src[37])) { // leggings
            if (overwriteArmor || isAir(inv.getLeggings())) inv.setLeggings(src[37].clone());
            else dropOverflow(p, src[37]);
        }
        if (src.length > 38 && !isAir(src[38])) { // chest
            if (overwriteArmor || isAir(inv.getChestplate())) inv.setChestplate(src[38].clone());
            else dropOverflow(p, src[38]);
        }
        if (src.length > 39 && !isAir(src[39])) { // helmet
            if (overwriteArmor || isAir(inv.getHelmet())) inv.setHelmet(src[39].clone());
            else dropOverflow(p, src[39]);
        }
        if (src.length > 40 && !isAir(src[40])) { // offhand
            if (overwriteArmor || isAir(inv.getItemInOffHand())) inv.setItemInOffHand(src[40].clone());
            else dropOverflow(p, src[40]);
        }

        p.updateInventory();
    }

    private boolean isAir(ItemStack it) {
        return it == null || it.getType() == Material.AIR;
    }

    private void safeSet(PlayerInventory inv, Player p, int slot, ItemStack item, boolean overwrite) {
        ItemStack cur = inv.getItem(slot);
        if (overwrite || isAir(cur)) {
            inv.setItem(slot, item.clone());
            return;
        }
        // 슬롯이 차있고 덮어쓰기 원치 않으면 인벤토리에 추가 시도
        HashMap<Integer, ItemStack> left = inv.addItem(item.clone());
        if (!left.isEmpty()) {
            left.values().forEach(rem -> dropOverflow(p, rem));
        }
    }

    private void dropOverflow(Player p, ItemStack item) {
        p.getWorld().dropItemNaturally(p.getLocation(), item.clone());
    }

}
