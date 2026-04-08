package com.example.lifesteal.service;

import com.example.lifesteal.LifeStealPlugin;
import com.example.lifesteal.data.PlayerData;
import com.example.lifesteal.storage.StorageProvider;
import com.example.lifesteal.storage.impl.YamlStorage;
import com.example.lifesteal.utils.MessageUtils;

import java.io.File;
import java.util.Collection;
import java.util.logging.Level;

public class MigrationService {

    private final LifeStealPlugin plugin;

    public MigrationService(LifeStealPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkAndMigrate(StorageProvider targetProvider) {
        if (targetProvider instanceof YamlStorage) return;

        File playersFolder = new File(plugin.getDataFolder(), "userdata");
        if (!playersFolder.exists() || playersFolder.listFiles() == null || playersFolder.listFiles().length == 0) {
            return;
        }

        if (!plugin.getConfig().getBoolean("storage.auto-migrate", true)) {
            return;
        }

        MessageUtils.log(Level.INFO, "Found legacy YAML data. Starting migration to SQL...");
        
        try {
            // Backup YAML folder first (Simple rename)
            File backup = new File(plugin.getDataFolder(), "userdata_backup_" + System.currentTimeMillis());
            
            YamlStorage yaml = new YamlStorage(plugin);
            yaml.init();
            
            Collection<PlayerData> allData = yaml.loadAll().join();
            MessageUtils.log(Level.INFO, "Migrating " + allData.size() + " player records...");
            
            targetProvider.saveAllSync(allData);
            
            // Rename after success
            playersFolder.renameTo(backup);
            
            MessageUtils.log(Level.INFO, "Migration completed successfully! Backup created at " + backup.getName());
        } catch (Exception e) {
            MessageUtils.log(Level.SEVERE, "Migration failed!");
            e.printStackTrace();
        }
    }
}
