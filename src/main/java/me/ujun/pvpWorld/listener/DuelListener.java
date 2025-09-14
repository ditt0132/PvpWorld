package me.ujun.pvpWorld.listener;

import me.ujun.pvpWorld.duel.DuelManager;
import me.ujun.pvpWorld.duel.Instance;
import me.ujun.pvpWorld.util.ResetUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.net.http.WebSocket;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DuelListener implements Listener {
    private final DuelManager duel;

    private final Set<UUID> inWater = new HashSet<>();

    public DuelListener(DuelManager duel) { this.duel = duel; }

    // 이동 제한 (카운트다운 중)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();

        if (!duel.isInDuel(p)) return;

        Instance inst = duel.getInstanceOf(p);
        if (inst.countdown) e.setCancelled(true);

    }

    // 블록 설치/파괴 제한 (카운트다운 중)
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!duel.isInDuel(p)) return;

        Instance inst = duel.getInstanceOf(p);

        if (inst.countdown) e.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!duel.isInDuel(p)) return;
        Instance inst = duel.getInstanceOf(p);

        if (inst.countdown) e.setCancelled(true);

        if (inst.kit.getType().equals("spleef")) {
            Block block = e.getBlock();

            if (block.getType() == Material.SNOW_BLOCK) {
                p.getInventory().addItem(ItemStack.of(Material.SNOWBALL, 4));
                e.setDropItems(false);
            }
        }
    }

    // 데미지 제한 (카운트다운 중)
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        Instance inst = duel.getInstanceOf(victim);

        if (inst == null) {
            return;
        }

        if (inst.countdown) {
            e.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true)
    public void onDamageBy(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player dam)) return;
        Instance inst = duel.getInstanceOf(dam);

        if (inst == null) {
            return;
        }

        if (inst.type.equals("duel")) {
            if (inst.teamB.contains(dam.getUniqueId()) && inst.teamB.contains(victim.getUniqueId())) {
                e.setCancelled(true);
                return;
            }

            if (inst.teamA.contains(dam.getUniqueId()) && inst.teamA.contains(victim.getUniqueId())) {
                e.setCancelled(true);
                return;
            }
        }

        if (inst.countdown) {
            e.setCancelled(true);
            return;
        }
        if (duel.spectators.contains(dam.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInWater(PlayerMoveEvent e) {

        Player p = e.getPlayer();
        Instance inst = duel.getInstanceOf(p);

        if (!duel.isInDuel(p)) return;
        if (!inst.kit.getType().equals("spleef") && !inst.kit.getType().equals("sumo")) return;
        if (duel.spectators.contains(p.getUniqueId())) return;
        if (p.isInvulnerable()) return;


        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        boolean was = inWater.contains(p.getUniqueId());
        boolean now = isWaterAtFeetOrBelow(e.getTo());

        if (!was && now) {
            duel.eliminate(p, p.getKiller());
            inWater.add(p.getUniqueId());
        } else if (was && !now) {
            // 물에서 "나옴"
            inWater.remove(p.getUniqueId());
        }
    }

    private boolean isWaterAtFeetOrBelow(Location loc) {
        Block b = loc.getBlock();
        if (isWaterLike(b)) return true;
        Block below = b.getRelative(0, -1, 0);
        return isWaterLike(below);
    }
    private boolean isWaterLike(Block b) {
        Material m = b.getType();
        if (m == Material.WATER || m == Material.BUBBLE_COLUMN) return true;
        // 워터로그드 블럭 처리
        BlockData bd = b.getBlockData();
        if (bd instanceof org.bukkit.block.data.Waterlogged w && w.isWaterlogged()) return true;
        return false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent e) {
        Projectile proj = e.getEntity();
        if (!(proj instanceof Snowball)) return;
        if (!(proj.getShooter() instanceof Player p)) return;

        if (!duel.isInDuel(p)) return;
        Instance inst = duel.getInstanceOf(p);

        if (!inst.kit.getType().equals("spleef")) return;

        Block b = e.getHitBlock();
        if (b == null) return;

        Material type = b.getType();

        /* 다른 블록은 무시 */
        if (type == Material.SNOW_BLOCK) {
            b.setType(Material.AIR);
            b.getWorld().playSound(b.getLocation(), Sound.BLOCK_SNOW_BREAK, 1f, 1f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreDeath(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!duel.isInDuel(p)) return;
        if (duel.isCountdown(p)) { e.setCancelled(true); return; }

        if (e.getFinalDamage() >= p.getHealth()) {
            if (hasTotemInHand(p) && !(e.getCause().equals(EntityDamageEvent.DamageCause.VOID))) {
                return;
            }

            e.setCancelled(true);

            Player killer = null;
            if (e instanceof EntityDamageByEntityEvent by) {
                if (by.getDamager() instanceof Player dp) killer = dp;
                else if (by.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player sp) killer = sp;
            }
            duel.eliminate(p, killer);

            if (e.getCause().equals(EntityDamageEvent.DamageCause.VOID)) {
                Instance inst = duel.getInstanceOf(p);

                Location center = inst.origin.clone().add(inst.meta.center().dx(), inst.meta.center().dy(), inst.meta.center().dz());;
                p.teleport(center);
            }
        }
    }

    private boolean hasTotemInHand(Player p) {
        var main = p.getInventory().getItemInMainHand();
        var off  = p.getInventory().getItemInOffHand();
        return (main != null && main.getType() == Material.TOTEM_OF_UNDYING)
                || (off  != null && off.getType()  == Material.TOTEM_OF_UNDYING);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        if (!duel.isInDuel(player)) {
            return;
        }

        Instance inst = duel.getInstanceOf(player);

        if (inst.watchers.contains(player.getUniqueId())) {
            ResetUtil.joinLobby(player);
            return;
        }


        duel.offlinePlayers.add(player.getUniqueId());

        if (!duel.spectators.contains(player.getUniqueId())) {
            duel.eliminate(player, player.getKiller());
        }
    }
}
