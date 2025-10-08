package me.ujun.pvpWorld.listener

import me.ujun.pvpWorld.duel.DuelManager
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import java.util.UUID
import kotlin.math.roundToInt

class DuelStatListener(private val duelManager: DuelManager) : Listener {
    val damagesDealt: MutableMap<UUID, Pair<Double, Int>> = mutableMapOf() // Player, Pair<DamageAdded, Count>
    val damagesGot: MutableMap<UUID, Pair<Double, Int>> = mutableMapOf() // Player, Pair<DamageAdded, Count>
    val misses: MutableMap<UUID, Pair<Double, Int>> = mutableMapOf() // Player, Pair<MissAdded, Count>
    val reaches: MutableMap<UUID, Pair<Double, Int>> = mutableMapOf() // Player, Pair<ReachAdded, Count>

    @EventHandler
    @Suppress("UnstableApiUsage")
    fun onPlayerHit(e: EntityDamageEvent) {
        val attacker = e.damageSource.causingEntity ?: return
        val victim = e.entity
        if (attacker !is Player) return
        if (victim !is Player) return
        if (!duelManager.byPlayer.containsKey(attacker.uniqueId)) return
        if (!duelManager.byPlayer.containsKey(victim.uniqueId)) return

        val maxDistance = attacker.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE)?.value ?: 3.0
        val result = attacker.rayTraceEntities(maxDistance.roundToInt())
        if (result == null || result.hitEntity != victim) return
        val dist = attacker.eyeLocation.distance(result.hitPosition.toLocation(attacker.world))

        reaches[attacker.uniqueId] = (dist to 1) + reaches[attacker.uniqueId]
        damagesDealt[attacker.uniqueId] = (e.finalDamage to 1) + damagesDealt[attacker.uniqueId]
        damagesGot[victim.uniqueId] = (e.finalDamage to 1) + damagesGot[victim.uniqueId]
    }


}

operator fun Pair<Double, Int>.plus(other: Pair<Double, Int>?): Pair<Double, Int> {
    var o = 0.0 to 0
    if (other != null) o = other
    return (this.first + o.first) to (this.second + o.second)
}

operator fun Pair<Int, Int>.plus(other: Pair<Int, Int>?): Pair<Int, Int> {
    var o = 0 to 0
    if (other != null) o = other
    return (this.first + o.first) to (this.second + o.second)
}