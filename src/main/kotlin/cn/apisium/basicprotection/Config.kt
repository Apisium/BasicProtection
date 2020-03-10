package cn.apisium.basicprotection

import kotlinx.serialization.Serializable

@Serializable
data class WorldConfig(
        val farmProtection: Boolean = false,
        val keepInventory: Boolean = false,
        val spawnerChangeable: Boolean = false,
        val explosionProtection: Boolean = false,
        val containerOpenable: Boolean = true,
        val buildable: Boolean = true,
        val breakable: Boolean = true,
        val physicalChangeable: Boolean = true,
        val spanwerMobsSpanwable: Boolean = true,
        val itemsDroppable: Boolean = true,
        val itemsPickupable: Boolean = true,
        val itemsCraftable: Boolean = true,
        val flying: Boolean = false,
        val weatherChangeable: Boolean = true,
        val pvp: Boolean = true,
        val dischargeable: Boolean = true,
        val entitiesDamageable: Boolean = true,
        val playerDamageable: Boolean = true,
        val itemsDamageable: Boolean = true,
        val autoRespawn: Boolean = false,
        val redstoneRemove: Boolean = false,
        val maxEntitiesPreChunk: Int = -1
)

@Serializable
data class GlobalConfig(
        val entitiesCleanerCheckTime: Int = 5,
        val redstoneThreshold: Int = 20,
        val hideMessage: Boolean = false,
        val redstoneRemoveMessage: String = "§cHigh frequency redstone signal detected: §eIn ({world},{x},{y},{z})",
        val entitiesRemoveMessage: String = "§cCrowded farming detected: §eIn ({world},{x},{z})"
)