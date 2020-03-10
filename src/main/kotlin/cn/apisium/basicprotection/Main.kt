package cn.apisium.basicprotection

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.parse
import kotlinx.serialization.parseMap
import kotlinx.serialization.stringify
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Animals
import org.bukkit.entity.Entity
import org.bukkit.entity.Monster
import org.bukkit.entity.Squid
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*
import kotlin.collections.HashMap

internal lateinit var CONFIG: HashMap<World, WorldConfig>
internal lateinit var GLOBAL_CONFIG: GlobalConfig
private val JSON = Json(JsonConfiguration.Stable.copy(prettyPrint = true, indent = "  "))

@Suppress("UNUSED")
@kotlinx.serialization.ImplicitReflectionSerializer
class Main: JavaPlugin(), CommandExecutor {
    private val globalConfigPath = File(dataFolder, "global-config.json")
    private val configPath = File(dataFolder, "config.json")
    private var timer = -1
    override fun onEnable() {
        logger.info("Loading...")
        loadConfig()
        server.pluginManager.registerEvents(Events(this), this)
        registerEntitiesCleaner()
        server.getPluginCommand("bcp").executor = this
        logger.info("Loaded!")
    }

    private fun loadConfig() {
        if (!dataFolder.exists()) dataFolder.mkdir()
        val loaded = (if (configPath.isFile) {
            try {
                JSON.parseMap<String, WorldConfig>(configPath.readText()).toMap(HashMap())
            } catch (ignored: Exception) {
                configPath.renameTo(File(dataFolder, "config.${System.currentTimeMillis()}.json"))
                null
            }
        } else null) ?: HashMap()
        CONFIG = HashMap()
        server.worlds.forEach { if (!loaded.containsKey(it.name)) loaded[it.name] = WorldConfig() }
        configPath.writeText(JSON.stringify(loaded))
        loaded.forEach { (k, v) ->
            try {
                CONFIG[server.getWorld(k)] = v
            } catch (ignored: Exception) { }
        }
        val globalLoaded = if (globalConfigPath.isFile) {
            try {
                JSON.parse<GlobalConfig>(globalConfigPath.readText())
            } catch (ignored: Exception) {
                globalConfigPath.renameTo(File(dataFolder, "global-config.${System.currentTimeMillis()}.json"))
                null
            }
        } else null
        if (globalLoaded == null) {
            GLOBAL_CONFIG = GlobalConfig()
            globalConfigPath.writeText(JSON.stringify(GLOBAL_CONFIG))
        } else GLOBAL_CONFIG = globalLoaded
    }

    private fun registerEntitiesCleaner() {
        val worlds = server.worlds.mapNotNull {
            val i = CONFIG[it]!!.maxEntitiesPreChunk
            if (i >= 0) it to i else null
        }
        timer = server.scheduler.runTaskTimer(this, {
            worlds.forEach { w ->
                w.first.loadedChunks.forEach { c ->
                    var entities = c.entities.filter { it is Animals || it is Monster || it is Squid }
                    if (entities.size > w.second) {
                        if (!GLOBAL_CONFIG.hideMessage) server.broadcastMessage(GLOBAL_CONFIG.entitiesRemoveMessage
                                .replace("{world}", c.world.name)
                                .replace("{x}", (c.x shl 4).toString())
                                .replace("{z}", (c.z shl 4).toString())
                        )
                        entities = entities.filter { it.customName == null }
                        if (entities.size > w.second) {
                            entities.shuffled().subList(0, w.second - 1).forEach(Entity::remove)
                        }
                    }
                }
            }
        }, GLOBAL_CONFIG.entitiesCleanerCheckTime * 20L, GLOBAL_CONFIG.entitiesCleanerCheckTime * 20L).taskId
    }

    private fun reload(sender: CommandSender) {
        sender.sendMessage("§b[Basic Protection]: §eReloading...")
        if (timer != -1) server.scheduler.cancelTask(timer)
        loadConfig()
        registerEntitiesCleaner()
        sender.sendMessage("§b[Basic Protection]: §aReloaded!")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("§b[Basic Protection]: §eAuthor: Shirasawa §7(https://apisium.cn)")
        } else if (args[0] == "reload") {
            if (sender.isOp) reload(sender)
            else sender.sendMessage("§b[Basic Protection]: §cYou don't have enough permission!")
        }
        return true
    }
}
