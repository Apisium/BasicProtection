package cn.apisium.basicprotection

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.entity.EntityType
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.block.*
import org.bukkit.event.entity.*
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.*
import org.bukkit.event.weather.WeatherChangeEvent
import org.bukkit.plugin.java.JavaPlugin

private fun getConfig(w: World) = CONFIG[w] ?: throw NullPointerException("The world (${w.name}) is not exists!")

class Events(private val plugin: JavaPlugin) : Listener {
    private val redstoneRecord = HashMap<Location, Int>()

    init {
//        plugin.server.scheduler.runTaskTimer(plugin, fun () {
//            plugin.server.onlinePlayers.forEach(this::checkPlayerFlight)
//        }, 10L, 10L)
        plugin.server.scheduler.runTaskTimer(plugin, redstoneRecord::clear, 5 * 20L, 5 * 20L)
    }

    // 耕地保护 & 交互保护 & 禁止发射 & 刷怪笼保护
    @EventHandler
    private fun onPlayerInteract(e: PlayerInteractEvent) {
        if (e.player.isOp) return
        val cfg = getConfig(e.clickedBlock?.world ?: e.player.world)

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
        if (getConfig(e.block.world).farmProtection && e.entityType != EntityType.PLAYER && e.block.type == Material.SOIL)
            e.isCancelled = true
    }

    // 爆炸保护
    @EventHandler
    private fun onEntityExplode(e: EntityExplodeEvent) {
        if (getConfig(e.entity.world).explosionProtection) e.blockList().clear()
    }

    // 爆炸保护
    @EventHandler
    private fun onExplosionPrime(e: BlockExplodeEvent) {
        if (getConfig(e.block.world).explosionProtection) e.blockList().clear()
    }

    // 建造保护
    @EventHandler
    private fun onBlockPlace(e: BlockPlaceEvent) {
        if (!e.player.isOp && !getConfig(e.block.world).buildable) e.isCancelled = true
    }

    // 破坏保护
    @EventHandler
    private fun onBlockBreak(e: BlockBreakEvent) {
        if (!e.player.isOp && !getConfig(e.block.world).breakable) e.isCancelled = true
    }

    // 禁止刷怪笼刷新生物
    @EventHandler
    private fun onSpawnerSpawn(e: SpawnerSpawnEvent) {
        if (!getConfig(e.spawner.world).spanwerMobsSpanwable) e.isCancelled = true
    }

    // 禁止玩家丢弃物品
    @EventHandler
    private fun onPlayerDropItem(e: PlayerDropItemEvent) {
        if (!e.player.isOp && !getConfig(e.player.world).itemsDroppable) e.isCancelled = true
    }

    // 禁止下雨
    @EventHandler
    private fun onWeatherChange(e: WeatherChangeEvent) {
        if (!getConfig(e.world).weatherChangeable) e.isCancelled = true
    }

    // 禁止PVP & 击杀生物
    @EventHandler
    private fun onEntityDamageByEntity(e: EntityDamageByEntityEvent) {
        val p = e.damager
        if (p !is Player || p.isOp) return
        val d = e.entity
        val cfg = getConfig(e.entity.world)
        if (d is Player) {
            if (!cfg.pvp) e.isCancelled = true
        } else if (!cfg.entitiesDamageable) e.isCancelled = true
    }

    // 防止玩家受到伤害
    @EventHandler
    private fun onEntityDamage(e: EntityDamageEvent) {
        if (e.cause != EntityDamageEvent.DamageCause.VOID && e.cause != EntityDamageEvent.DamageCause.SUICIDE &&
                e.entity is Player && !getConfig(e.entity.world).playerDamageable) e.isCancelled = true
    }

    // 防止玩家物品坏掉
    @EventHandler
    private fun onPlayerItemDamage(e: PlayerItemDamageEvent) {
        if (!e.player.isOp && !getConfig(e.player.world).itemsDamageable) e.isCancelled = true
    }

    // 自动死亡不掉落
    @EventHandler
    private fun onPlayerDeath(e: PlayerDeathEvent) {
        val cfg = getConfig(e.entity.world)
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
        if (p is Player && !p.isOp && !getConfig(p.world).dischargeable) e.isCancelled = true
    }

    // 射了
    @EventHandler
    private fun onPlayerEggThrow(e: PlayerEggThrowEvent) {
        e.player.isFlying
        if (!e.player.isOp && !getConfig(e.player.world).dischargeable) e.isHatching = false
    }

    // 枯了
    @EventHandler
    private fun onLeavesDecay(e: LeavesDecayEvent) {
        if (!getConfig(e.block.world).physicalChangeable) e.isCancelled = true
    }

    // 长了
    @EventHandler
    private fun onBlockGrow(e: BlockGrowEvent) {
        if (!getConfig(e.block.world).physicalChangeable) e.isCancelled = true
    }

    // 冻了
    @EventHandler
    private fun onBlockForm(e: BlockFormEvent) {
        if (!getConfig(e.block.world).physicalChangeable) e.isCancelled = true
    }

    // 解了
    @EventHandler
    private fun onBlockFade(e: BlockFadeEvent) {
        if (!getConfig(e.block.world).physicalChangeable) e.isCancelled = true
    }

    // 传了
    @EventHandler
    private fun onBlockSpread(e: BlockSpreadEvent) {
        if (!getConfig(e.block.world).physicalChangeable) e.isCancelled = true
    }

    // 捡了
    @EventHandler
    private fun onPlayerAttemptPickupItem(@Suppress("DEPRECATION") e: PlayerPickupItemEvent) {
        if (!getConfig(e.player.world).itemsPickupable) e.isCancelled = true
    }

    // 造了
    @EventHandler
    private fun onCraftItem(e: CraftItemEvent) {
        if (!getConfig(e.whoClicked.world).itemsCraftable) e.isCancelled = true
    }

    // 红石信号
    @EventHandler
    private fun onBlockRedstone(e: BlockRedstoneEvent) {
        val type = e.block.type
        when (type) {
            Material.DIODE_BLOCK_OFF -> { }
            Material.DIODE_BLOCK_ON -> { }
            Material.DIODE -> { }
            Material.REDSTONE -> { }
            Material.REDSTONE_WIRE -> { }
            Material.REDSTONE_TORCH_OFF -> { }
            Material.REDSTONE_TORCH_ON -> { }
            else -> return
        }
        val cfg = getConfig(e.block.world)
        if (!cfg.redstoneRemove) return
        val loc = e.block.location
        var i = redstoneRecord[loc]
        if (i == null) {
            redstoneRecord[loc] = 1
            i = 1
        } else {
            i++
            redstoneRecord[loc] = i
        }
        if (i >= GLOBAL_CONFIG.redstoneThreshold) {
            if (type == Material.DIODE_BLOCK_OFF || type == Material.DIODE_BLOCK_ON ||
                    type == Material.DIODE) {
                e.block.type = Material.AIR
                plugin.server.scheduler.runTaskLater(plugin, fun () {
                    e.block.type = Material.AIR
                }, 5L)
            } else e.block.breakNaturally()
            if (!GLOBAL_CONFIG.hideMessage) plugin.server.broadcastMessage(GLOBAL_CONFIG.redstoneRemoveMessage
                    .replace("{world}", loc.world.name)
                    .replace("{x}", loc.blockX.toString())
                    .replace("{y}", loc.blockY.toString())
                    .replace("{z}", loc.blockZ.toString())
            )
            redstoneRecord.remove(loc)
        }
    }

//    private fun checkPlayerFlight(p: Player) {
//        if (p.isOp || p.gameMode == GameMode.CREATIVE || p.gameMode == GameMode.SPECTATOR || (
//                        p.hasPermission("essentials.fly") && getConfig(p.world).flying)) {
//            p.allowFlight = true
//        } else {
//            p.isFlying = false
//            p.allowFlight = false
//        }
//    }
//
//    // 切换世界
//    @EventHandler(priority = EventPriority.MONITOR)
//    private fun onPlayerChangedWorld(e: PlayerChangedWorldEvent) {
//        plugin.server.scheduler.runTaskTimer(plugin, fun () {
//            checkPlayerFlight(e.player)
//        }, 5L, 5L)
//    }
//
//    // 玩家加入游戏
//    @EventHandler(priority = EventPriority.MONITOR)
//    private fun onPlayerJoin(e: PlayerJoinEvent) {
//        plugin.server.scheduler.runTaskTimer(plugin, fun () {
//            checkPlayerFlight(e.player)
//        }, 5L, 5L)
//    }
//
//    // 玩家离开游戏
//    @EventHandler(priority = EventPriority.MONITOR)
//    private fun onPlayerJoin(e: PlayerQuitEvent) {
//        plugin.server.scheduler.runTaskTimer(plugin, fun () {
//            checkPlayerFlight(e.player)
//        }, 5L, 5L)
//    }
}
