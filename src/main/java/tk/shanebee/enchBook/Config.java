package tk.shanebee.enchBook;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Config implements Listener {

    private final EnchBook plugin;
    private FileConfiguration config;
    private File configFile;

    public String PREFIX;
    public boolean SAFE_ENCHANTS;
    public boolean SAFE_BOOKS;
    public boolean IGNORE_CONFLICTS;
    public Map<Enchantment, Integer> MAX_LEVELS;
    public boolean ABOVE_VAN_REQUIRES_PERM;
    public boolean ABOVE_VAN_REQUIRES_PERM_ITEM;
    public String MSG_NO_PERM;

    Config(EnchBook plugin) {
        this.plugin = plugin;
        loadConfigFile();
    }

    private void loadConfigFile() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        matchConfig();
        loadConfigs();
    }

    // Used to update config
    @SuppressWarnings("ConstantConditions")
    private void matchConfig() {
        try {
            boolean hasUpdated = false;
            InputStream stream = plugin.getResource(configFile.getName());
            assert stream != null;
            InputStreamReader is = new InputStreamReader(stream);
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(is);
            for (String key : defConfig.getConfigurationSection("").getKeys(true)) {
                if (key.startsWith("Options.Max Levels")) continue;
                if (!config.contains(key)) {
                    config.set(key, defConfig.get(key));
                    hasUpdated = true;
                }
            }
            for (String key : config.getConfigurationSection("").getKeys(true)) {
                if (key.startsWith("Options.Max Levels")) continue;
                if (!defConfig.contains(key)) {
                    config.set(key, null);
                    hasUpdated = true;
                }
            }
            if (hasUpdated)
                config.save(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConfigs() {
        this.PREFIX = config.getString("Options.Prefix");
        this.SAFE_ENCHANTS = config.getBoolean("Options.Safe Enchants");
        this.SAFE_BOOKS = config.getBoolean("Options.Safe Books");
        this.IGNORE_CONFLICTS = config.getBoolean("Options.Ignore Conflicts");
        {
            MAX_LEVELS = new HashMap<>();
            ConfigurationSection section = Objects.requireNonNull(config.getConfigurationSection("Options.Max Levels"), "Max Level section is null");
            for (String key : section.getKeys(false)) {
                Enchantment enchantment = Objects.requireNonNull(Enchantment.getByKey(NamespacedKey.minecraft(key)), key + " is not a valid enchantment");
                MAX_LEVELS.put(enchantment, section.getInt(key));
            }
        }
        this.ABOVE_VAN_REQUIRES_PERM = config.getBoolean("Options.Above Vanilla Requires Permission");
        this.ABOVE_VAN_REQUIRES_PERM_ITEM = config.getBoolean("Options.Above Vanilla Item Requires Permission");
        this.MSG_NO_PERM = config.getString("Messages.No Permission");
    }

}
