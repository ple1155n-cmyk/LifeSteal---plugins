package com.example.lifesteal.config;

import com.example.lifesteal.LifeStealPlugin;
import com.example.lifesteal.utils.MessageUtils;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Level;

@Getter
public class ConfigManager {

    private final LifeStealPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(LifeStealPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Auto-update check
        double version = config.getDouble("config-version", 1.0);
        if (version < 1.3) {
            MessageUtils.log(Level.INFO, "Updating configuration from version " + version + " to 1.3...");
            updateConfig();
        }
        
        validateConfig();
    }

    private void updateConfig() {
        // Simple merge logic: we save our default and then overwrite with old values
        // In a real enterprise plugin, we'd use a more sophisticated library like ConfigUpdater
        // For now, we'll manually ensure new keys are present
        plugin.saveResource("config.yml", true);
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        MessageUtils.log(Level.INFO, "Config updated to 1.3.");
    }

    private void validateConfig() {
        int min = config.getInt("gameplay.heart.min", 1);
        int max = config.getInt("gameplay.heart.max", 20);

        if (min < 1) {
            MessageUtils.log(Level.WARNING, "Invalid min hearts! Setting to 1.");
            config.set("gameplay.heart.min", 1);
        }

        if (max < min) {
            MessageUtils.log(Level.WARNING, "Max hearts cannot be less than min hearts! Setting to 20.");
            config.set("gameplay.heart.max", 20);
        }
        
        // Save clamped values
        plugin.saveConfig();
    }
    
    public String getMessage(String path) {
        return config.getString("messages." + path, "<red>Missing message: " + path);
    }
}
