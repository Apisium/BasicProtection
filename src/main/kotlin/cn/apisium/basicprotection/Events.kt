package cn.apisium.basicprotection

import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.entity.EntityType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.*
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerEggThrowEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.weather.WeatherChangeEvent
import org.bukkit.plugin.java.JavaPlugin

class Events(private val plugin: JavaPlugin) : Listener {
    private val redstoneRecord = HashMap<Location, Int>()

    init {
        plugin.server.scheduler.runTaskTimer(plugin, {
            redstoneRecord.clear()
        }, 50 * 20L, 50 * 20L)
    }

    // 耕地保护 & 交互保护 & 禁止发射 & 刷怪笼保护
    @EventHandler
    private fun onPlayerInteract(e: PlayerInteractEvent) {
        if (e.player.isOp) return
        val cfg = CONFIG[e.clickedBlock.world]!!

        val type = e.item?.type
        if (
                (!cfg.containerOpenable && (e.action == Action.RIGHT_CLICK_BLOCK || e.action == Action.PHYSICAL)) ||
                (cfg.farmProtection && e.action == Action.PHYSICAL && e.clickedBlock?.type == Material.SOIL) ||
                (!cfg.dischargeable && (type == Material.EGG || type == Material.ENDER_PEARL)) ||
                (!cfg.spawnerChangeable && e.action == Action.RIGHT_CLICK_BLOCK &&
                        e.clickedBlock?.type == Material.MOB_SPAWNER &&
                        (type == Material.MONSTER_EGG || type == Material.MONSTER_EGGS))
        ) e.isCancelled = true
    }

    // 耕地保护
    @EventHandler
    private fun onEntityInteract(e: EntityInteractEvent) {
        if (CONFIG[e.block.world]!!.farmProtection && e.entityType != EntityType.PLAYER && e.block.type == Material.SOIL)
            e.isCancelled = true
    }

    // 爆炸保护
    @EventHandler
    private fun onExplosionPrime(e: EntityExplodeEvent) {
        if (CONFIG[e.entity.world]!!.explosionProtection) e.blockList().clear()
    }

    // 建造保护
    @EventHandler
    private fun onBlockPlace(e: BlockPlaceEvent) {
        if (!e.player.isOp && !CONFIG[e.block.world]!!.buildable) e.isCancelled = true
    }

    // 破坏保护
    @EventHandler
    private fun onBlockBreak(e: BlockBreakEvent) {
        if (!e.player.isOp && !CONFIG[e.block.world]!!.breakable) e.isCancelled = true
    }

    // 禁止刷怪笼刷新生物
    @EventHandler
    private fun onSpawnerSpawn(e: SpawnerSpawnEvent) {
        if (!CONFIG[e.spawner.world]!!.spanwerMobsSpanwable) e.isCancelled = true
    }

    // 禁止玩家丢弃物品
    @EventHandler
    private fun onPlayerDropItem(e: PlayerDropItemEvent) {
        if (!e.player.isOp && !CONFIG[e.player.world]!!.itemsDroppable) e.isCancelled = true
    }

    // 禁止下雨
    @EventHandler
    private fun onWeatherChange(e: WeatherChangeEvent) {
        if (!CONFIG[e.world]!!.weatherChangeable) e.isCancelled = true
    }

    // 禁止PVP & 击杀生物
    @EventHandler
    private fun onEntityDamageByEntity(e: EntityDamageByEntityEvent) {
        val p = e.damager
        if (p !is Player || p.isOp) return
        val d = e.entity
        val cfg = CONFIG[e.entity.world]!!
        if (d is Player) {
            if (!cfg.pvp) e.isCancelled = true
        } else if (!cfg.entitiesDamageable) e.isCancelled = true
    }

    // 防止玩家受到伤害
    @EventHandler
    private fun onEntityDamage(e: EntityDamageEvent) {
        if (e.cause != EntityDamageEvent.DamageCause.VOID && e.cause != EntityDamageEvent.DamageCause.SUICIDE &&
                e.entity is Player && !CONFIG[e.entity.world]!!.playerDamageable) e.isCancelled = true
    }

    // 防止玩家物品坏掉
    @EventHandler
    private fun onPlayerItemDamage(e: PlayerItemDamageEvent) {
        if (!e.player.isOp && !CONFIG[e.player.world]!!.itemsDamageable) e.isCancelled = true
    }

    // 自动死亡不掉落
    @EventHandler
    private fun onPlayerDeath(e: PlayerDeathEvent) {
        val cfg = CONFIG[e.entity.world]!!
        if (cfg.autoRespawn) {
            plugin.server.scheduler.scheduleSyncDelayedTask(plugin, {
                try { e.entity.spigot().respawn() } catch (ignored: Exception) { }
            }, 2)
        }
        if (cfg.keepInventory) {
            e.keepInventory = true
            e.keepLevel = true
        }
    }

    // 射了
    @EventHandler
    private fun onEntityShootBow(e: EntityShootBowEvent) {
        val p = e.entity
        if (e is Player && !p.isOp && !CONFIG[e.player.world]!!.dischargeable) e.isCancelled = true
    }

    // 射了
    @EventHandler
    private fun onPlayerEggThrow(e: PlayerEggThrowEvent) {
        if (!e.player.isOp && !CONFIG[e.player.world]!!.dischargeable) e.isHatching = false
    }

    // 枯了
    @EventHandler
    private fun onLeavesDecay(e: LeavesDecayEvent) {
        if (!CONFIG[e.block.world]!!.physicalChangeable) e.isCancelled = true
    }

    // 长了
    @EventHandler
    private fun onBlockGrow(e: BlockGrowEvent) {
        if (!CONFIG[e.block.world]!!.physicalChangeable) e.isCancelled = true
    }

    // 冻了
    @EventHandler
    private fun onBlockForm(e: BlockFormEvent) {
        if (!CONFIG[e.block.world]!!.physicalChangeable) e.isCancelled = true
    }

    // 解了
    @EventHandler
    private fun onBlockFade(e: BlockFadeEvent) {
        if (!CONFIG[e.block.world]!!.physicalChangeable) e.isCancelled = true
    }

    // 传了
    @EventHandler
    private fun onBlockSpread(e: BlockSpreadEvent) {
        if (!CONFIG[e.block.world]!!.physicalChangeable) e.isCancelled = true
    }

    // 红石信号
    @EventHandler
    private fun onBlockRedstone(e: BlockRedstoneEvent) {
        val cfg = CONFIG[e.block.world]!!
        if (!cfg.redstoneRemove) return
        val loc = e.block.location
        var i = redstoneRecord[loc]
        if (i == null) {
            redstoneRecord[loc] = 1
            i = 1
        }
        if (i >= cfg.redstoneThreshold) {
            e.block.breakNaturally()
            plugin.server.broadcastMessage("§c检测到高频红石: §e${loc.world.name}-${loc.blockX},${loc.blockY},${loc.blockZ}")
            redstoneRecord.remove(loc)
        }
    }
}
