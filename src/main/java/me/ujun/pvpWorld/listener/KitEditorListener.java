package me.ujun.pvpWorld.listener;

import me.ujun.pvpWorld.kit.Kit;
import me.ujun.pvpWorld.kit.KitLayout;
import me.ujun.pvpWorld.kit.KitManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.Optional;
import java.util.UUID;

public class KitEditorListener implements Listener {

    private final KitManager kitManager;

    public KitEditorListener(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        HumanEntity he = e.getPlayer();
        if (!(he instanceof Player p)) return;

        UUID id = p.getUniqueId();

        Optional<String> editingName = kitManager.getEditingKitName(id);
        if (editingName.isEmpty()) return; // 우리 에디터가 아님


        String kitName = editingName.get();
        Kit kit = kitManager.get(kitName);
        if (kit == null) {
            kitManager.closeEditor(id);
            return;
        }


        Inventory inv = e.getInventory();
        if (inv.getSize() != Kit.GUI_SIZE) {
            kitManager.closeEditor(id);
            return;
        }


        kitManager.getEditingDisplay(id).ifPresent(kit::setDisplayName);

        kit.updateFromEditor(e.getInventory());
        kitManager.closeEditor(id);

        p.sendMessage(ChatColor.GREEN + "키트가 임시 저장되었습니다. /pvpworld kit save 로 디스크에 저장하세요.");
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (kitManager.getEditingKitName(p.getUniqueId()).isEmpty()) return;

        if (e.getClickedInventory() == null || e.getView().getTopInventory() == null) return;
        if (!e.getView().getTopInventory().equals(e.getClickedInventory())) return; // 상단 GUI만 제한

        int slot = e.getRawSlot(); // 상단 기준 인덱스
        if (slot < 0 || slot >= e.getView().getTopInventory().getSize()) return;

        // 구분선/미사용 슬롯 클릭 금지
        if (isLockedSlot(slot)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (kitManager.getEditingKitName(p.getUniqueId()).isEmpty()) return;

        // 상단 GUI 범위에 하나라도 걸치면 취소
        int topSize = e.getView().getTopInventory().getSize();
        for (int s : e.getRawSlots()) {
            if (s < topSize && isLockedSlot(s)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    private boolean isLockedSlot(int slot) {
        for (int s : KitLayout.UI_SEPARATOR)   if (s == slot) return true;
        for (int s : KitLayout.UI_UNUSED_TOP) if (s == slot) return true;
        return false;
    }
}
