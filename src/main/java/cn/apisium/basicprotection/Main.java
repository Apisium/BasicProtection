package cn.apisium.basicprotection;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Squid;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.command.Command;
import org.bukkit.plugin.java.annotation.command.Commands;
import org.bukkit.plugin.java.annotation.dependency.SoftDependency;
import org.bukkit.plugin.java.annotation.permission.Permission;
import org.bukkit.plugin.java.annotation.permission.Permissions;
import org.bukkit.plugin.java.annotation.plugin.*;
import org.bukkit.plugin.java.annotation.plugin.author.Author;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

@Plugin(name = "BasicProtection", version = "1.0")
@Description("An simple protection plugin.")
@Author("Shirasawa")
@Website("https://apisium.cn")
@SoftDependency("Multiverse-Core")
@SoftDependency("Essentials")
@SoftDependency("EssentialsX")
@Permissions(@Permission(name = "basicprotection.reload", defaultValue = PermissionDefault.OP))
@Commands(@Command(name = "basicprotection", aliases = { "bcp" }, permission = "basicprotection.reload"))
public final class Main extends JavaPlugin {
    protected int redstoneThreshold = -1;
    private final File CONFIG_FILE = new File(getDataFolder(), "config.json");
    private final Path CONFIG_PATH = CONFIG_FILE.toPath();
    private final HashMap<String, Config> worldConfigs = new HashMap<>();
    private final static Type TYPE = new TypeToken<HashMap<String, Config>>() {}.getType();
    private final static Random RANDOM = new Random();

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            _loadConfig();
        } catch (Exception e) {
            e.printStackTrace();
            setEnabled(false);
            return;
        }
        getServer().getPluginCommand("basicprotection").setExecutor(this);
        getServer().getPluginManager().registerEvents(new Events(worldConfigs, this), this);
        getServer().getScheduler().runTaskTimer(this, () -> getServer().getWorlds().forEach(w -> {
             final Config cfg = worldConfigs.get(w.getName());
             if (cfg == null || cfg.maxEntitiesPreChunk < 1) return;
             for (final Chunk c : w.getLoadedChunks()) {
                 final ArrayList<Entity> list = new ArrayList<>();
                 for (final Entity e : c.getEntities()) if (e.getCustomName() == null &&
                     (e instanceof Animals || e instanceof Monster || e instanceof Squid))
                     list.add(e);
                 int i = list.size();
                 while (--i > cfg.maxEntitiesPreChunk) list.get(RANDOM.nextInt(i)).remove();
            }
        }), 10 * 20L, 10 * 20L);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void _loadConfig() throws Exception {
        if (!getDataFolder().exists()) getDataFolder().mkdir();
        worldConfigs.clear();
        if (CONFIG_FILE.exists()) try (final BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
            new JsonParser().parse(reader).getAsJsonObject().entrySet()
                .forEach(it -> worldConfigs.put(it.getKey(), new Gson().fromJson(it.getValue(), Config.class)));
        }
        getServer().getWorlds().forEach(it -> worldConfigs.putIfAbsent(it.getName(), new Config()));
        try (final BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH);
             final JsonWriter jw = new JsonWriter(writer)) {
            jw.setIndent("  ");
            new Gson().toJson(worldConfigs, TYPE, jw);
        }
        if (redstoneThreshold != -1) reloadConfig();
        redstoneThreshold = getConfig().getInt("redstoneThreshold", 20);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        try {
            _loadConfig();
            sender.sendMessage("Reload successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage("Reload failed!");
        }
        return true;
    }
}
