package me.ujun.pvpWorld.listener;

import me.ujun.pvpWorld.config.ConfigHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import java.net.http.WebSocket;

public class FoodChangeListener implements Listener {

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!p.getScoreboardTags().contains(ConfigHandler.ffaTag)) return;

        int from = p.getFoodLevel();
        int to = e.getFoodLevel();

        // 감소만 막기
        if (to < from) {
            e.setCancelled(true);
            p.setFoodLevel(from);
            p.setExhaustion(0f);
        }
    }
}
