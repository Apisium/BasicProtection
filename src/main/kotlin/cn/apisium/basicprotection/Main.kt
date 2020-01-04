package cn.apisium.basicprotection

import kotlinx.serialization.json.Json
import kotlinx.serialization.parseMap
import kotlinx.serialization.stringify
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*
import kotlin.collections.HashMap

internal lateinit var CONFIG: WeakHashMap<World, WorldConfig>

@Suppress("UNUSED")
@kotlinx.serialization.ImplicitReflectionSerializer
class Main: JavaPlugin() {
    private val configPath = File(dataFolder, "config.json")
    override fun onEnable() {
        logger.info("Loading...")
        val loaded = (if (configPath.isFile) {
            try {
                Json.parseMap<String, WorldConfig>(configPath.readText()).toMap(HashMap())
            } catch (ignored: Exception) {
                configPath.renameTo(File(dataFolder, "config.${System.currentTimeMillis()}.json"))
                null
            }
        } else null) ?: HashMap()
        CONFIG = WeakHashMap()
        server.worlds.forEach { if (!loaded.containsKey(it.name)) loaded[it.name] = WorldConfig() }
        configPath.writeText(Json.stringify(loaded))
        loaded.forEach { (k, v) ->
            try {
                CONFIG[server.getWorld(k)] = v
            } catch (ignored: Exception) { }
        }
        server.pluginManager.registerEvents(Events(this), this)
        logger.info("Loaded!")
    }
}
