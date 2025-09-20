package me.ujun.pvpWorld.listener;

import me.ujun.pvpWorld.PvpWorld;
import me.ujun.pvpWorld.config.ConfigHandler;
import me.ujun.pvpWorld.kit.KitManager;
import me.ujun.pvpWorld.util.ResetUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.UUID;

public class FfaListener implements Listener {
    private final JavaPlugin plugin;
    private final KitManager kitManager;

    public FfaListener(JavaPlugin plugin, KitManager kitManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;

        Player attacker;
        Projectile projectile;


        if (event.getDamager() instanceof Projectile) {
            projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            } else {
                return;
            }
        } else if (event.getDamager() instanceof  Player) {
            attacker = (Player) event.getDamager();
        } else {
            return;
        }
        if (!(event.getEntity() instanceof Player)) return;

        UUID attackerId = attacker.getUniqueId();

        Player damaged = (Player) event.getEntity();
        UUID damagedId = damaged.getUniqueId();

        if (attacker.equals(damaged)) return;
        if (!attacker.getScoreboardTags().contains(ConfigHandler.ffaTag) || !damaged.getScoreboardTags().contains(ConfigHandler.ffaTag)) return;

        PvpWorld.pvpPlayerTimer.put(attackerId, ConfigHandler.fightTime);
        PvpWorld.pvpPlayerTimer.put(damagedId, ConfigHandler.fightTime);

        PvpWorld.lastAttackers.computeIfAbsent(damagedId, k -> new HashSet<>()).add(attackerId);


        String fightMessage = ConfigHandler.fightMessage;
        fightMessage = fightMessage.replace("%sec%", String.valueOf(ConfigHandler.fightTime));

        attacker.sendActionBar(fightMessage);
        damaged.sendActionBar(fightMessage);

    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (PvpWorld.pvpPlayerTimer.containsKey(player.getUniqueId())) {
            player.setHealth(0);
        }
    }



    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getPlayer();

        if (PvpWorld.pvpPlayerTimer.containsKey(dead.getUniqueId())) {
            Player killer = event.getPlayer().getKiller();
            if (killer != null) {
                DecimalFormat df = new DecimalFormat("#.##");

                String hpText = df.format(killer.getHealth()) + "❤";
                Component orig = event.deathMessage();
                Component suffix = Component.space()
                        .append(Component.text("[", NamedTextColor.GRAY))
                        .append(Component.text(hpText, NamedTextColor.RED))
                        .append(Component.text("]", NamedTextColor.GRAY));

                orig = orig.replaceText(
                        builder -> builder.matchLiteral(dead.getName())
                                .replacement(Component.text(dead.getName(), NamedTextColor.RED))
                );

                orig = orig.replaceText(
                        builder -> builder.matchLiteral(killer.getName())
                                .replacement(Component.text(killer.getName(), NamedTextColor.GREEN))
                );

                event.deathMessage(orig.append(suffix));


                ResetUtil.resetPlayerState(killer);
                kitManager.applyTo(killer, PvpWorld.playerKits.get(killer.getUniqueId()), true, true);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (dead.isOnline()) {
                        dead.spigot().respawn();
                        ResetUtil.joinLobby(dead);
                    }
                });
            }

        }

        dead.removeScoreboardTag(ConfigHandler.ffaTag);
        PvpWorld.pvpPlayerTimer.remove(dead.getUniqueId());
        removeAttackerTimer(dead.getUniqueId());

    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (player.getScoreboardTags().contains(ConfigHandler.ffaTag)) {
            event.setCancelled(true);
        }
    }

    private void removeAttackerTimer(UUID deadID) {
        if (PvpWorld.lastAttackers.containsKey(deadID)) {
            for (UUID uuid : PvpWorld.lastAttackers.get(deadID)) {
                if (PvpWorld.pvpPlayerTimer.containsKey(uuid)) {
                    PvpWorld.pvpPlayerTimer.remove(uuid);
                    Player player = Bukkit.getPlayer(uuid);
                    player.sendActionBar(ConfigHandler.fightEndMessage);
                }
            }

            PvpWorld.lastAttackers.remove(deadID);
        }
    }

}
