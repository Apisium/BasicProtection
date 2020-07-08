package cn.apisium.basicprotection;

import com.destroystokyo.paper.event.entity.ThrownEggHatchEvent;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.HashMap;

final class Events implements Listener {
    private final HashMap<String, Config> worldConfigs;
    Events(final HashMap<String, Config> worldConfigs) {
        this.worldConfigs = worldConfigs;
    }
    
    private Config getConfig(final World world) { return worldConfigs.getOrDefault(world.getName(), new Config()); }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (e.getPlayer().isOp()) return;
        final Config cfg = getConfig(e.getClickedBlock() == null ? e.getPlayer().getWorld() : e.getClickedBlock().getWorld());

        final Material type = e.getItem() == null ? null : e.getItem().getType();
        if (
            (cfg.preventContainerOpen && (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.PHYSICAL)) ||
                (cfg.farmProtection && e.getAction() == Action.PHYSICAL && e.getClickedBlock() != null &&
                    e.getClickedBlock().getType() == Material.FARMLAND) ||
            (cfg.preventDischarge && (type == Material.EGG || type == Material.ENDER_PEARL)) ||
            (cfg.preventSpawnerChange && e.getAction() == Action.RIGHT_CLICK_BLOCK &&
                e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.SPAWNER &&
                type != null && type.name().endsWith("_SPAWN_EGG"))
        ) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityInteract(final EntityInteractEvent e) {
        if (getConfig(e.getBlock().getWorld()).farmProtection &&
            e.getEntityType() != EntityType.PLAYER && e.getBlock().getType() == Material.FARMLAND)
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent e) {
        if (getConfig(e.getLocation().getWorld()).explosionProtection) e.blockList().clear();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockExplode(final BlockExplodeEvent e) {
        if (getConfig(e.getBlock().getWorld()).explosionProtection) e.blockList().clear();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent e) {
        if (!e.getPlayer().isOp() && getConfig(e.getBlock().getWorld()).preventBuild) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(final PlayerBucketEmptyEvent e) {
        if (!e.getPlayer().isOp() && getConfig(e.getBlock().getWorld()).preventBuild) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e) {
        if (!e.getPlayer().isOp() && getConfig(e.getBlock().getWorld()).preventBreak) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBucketFill(final PlayerBucketFillEvent e) {
        if (!e.getPlayer().isOp() && getConfig(e.getBlock().getWorld()).preventBreak) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpawnerSpawn(final SpawnerSpawnEvent e) {
        if (getConfig(e.getSpawner().getWorld()).preventSpanwerMobsSpawn) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropItem(final PlayerDropItemEvent e) {
        if (!e.getPlayer().isOp() && getConfig(e.getPlayer().getWorld()).preventItemsDrop) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onWeatherChange(final WeatherChangeEvent e) {
        if (getConfig(e.getWorld()).preventWeatherChange) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        final Entity p = e.getDamager(), d = e.getEntity();
        if (!(p instanceof Player) || p.isOp()) return;
        final Config cfg = getConfig(d.getWorld());
        if (d instanceof Player) {
            if (cfg.preventPvp) e.setCancelled(true);
        } else if (cfg.preventDamageEntities) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.VOID && e.getCause() != EntityDamageEvent.DamageCause.SUICIDE &&
                e.getEntityType() == EntityType.PLAYER && getConfig(e.getEntity().getWorld()).preventDamagePlayer) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerItemDamage(final PlayerItemDamageEvent e) {
        if (!e.getPlayer().isOp() && getConfig(e.getPlayer().getWorld()).preventDamageItems) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityShootBow(final EntityShootBowEvent e) {
        if (e.getEntityType() == EntityType.PLAYER && !e.getEntity().isOp() &&
            getConfig(e.getEntity().getWorld()).preventDischarge) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerEggThrow(final ThrownEggHatchEvent e) {
        if (getConfig(e.getEgg().getWorld()).preventDischarge) e.setHatching(false);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLeavesDecay(final LeavesDecayEvent e) {
        if (getConfig(e.getBlock().getWorld()).preventPhysicalChange) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockGrow(final BlockGrowEvent e) {
        if (getConfig(e.getBlock().getWorld()).preventPhysicalChange) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockForm(final BlockFormEvent e) {
        if (getConfig(e.getBlock().getWorld()).preventPhysicalChange) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockFade(final BlockFadeEvent e) {
        if (getConfig(e.getBlock().getWorld()).preventPhysicalChange) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockSpread(final BlockSpreadEvent e) {
        if (getConfig(e.getBlock().getWorld()).preventPhysicalChange) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerAttemptPickupItem(final EntityPickupItemEvent e) {
        if (!e.getEntity().isOp() && e.getEntityType() == EntityType.PLAYER &&
            getConfig(e.getEntity().getWorld()).preventItemsPickup) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCraftItem(final CraftItemEvent e) {
        if (!e.getWhoClicked().isOp() && e.getWhoClicked().getType() == EntityType.PLAYER &&
            getConfig(e.getWhoClicked().getWorld()).preventItemsCraft) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockIgnite(final BlockIgniteEvent e) {
        if (e.getPlayer() != null && !e.getPlayer().isOp() &&
            getConfig(e.getPlayer().getWorld()).preventIgnite) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDeath(final PlayerDeathEvent e) {
        final Config cfg = getConfig(e.getEntity().getWorld());
        if (cfg.preventDamagePlayer) e.setCancelled(true);
        else if (cfg.autoRespawn) e.getEntity().spigot().respawn();
    }
}
