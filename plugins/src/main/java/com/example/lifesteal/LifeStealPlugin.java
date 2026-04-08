package com.example.lifesteal;

import com.example.lifesteal.config.ConfigManager;
import com.example.lifesteal.hooks.VaultHook;
import com.example.lifesteal.manager.HeartRegistry;
import com.example.lifesteal.manager.StateSafetyManager;
import com.example.lifesteal.service.CombatService;
import com.example.lifesteal.service.DiscordService;
import com.example.lifesteal.service.HealthManager;
import com.example.lifesteal.service.MigrationService;
import com.example.lifesteal.storage.StorageProvider;
import com.example.lifesteal.utils.MessageUtils;
import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class LifeStealPlugin extends JavaPlugin {

    @Getter
    private static LifeStealPlugin instance;

    @Getter
    private ConfigManager configManager;
    @Getter
    private StorageProvider storageProvider;
    @Getter
    private HeartRegistry heartRegistry;
    @Getter
    private HealthManager healthManager;
    
    @Getter
    private CombatService combatService;
    @Getter
    private MigrationService migrationService;
    @Getter
    private StateSafetyManager safetyManager;
    @Getter
    private DiscordService discordService;
    @Getter
    private VaultHook vaultHook;

    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        instance = this;
        this.adventure = BukkitAudiences.create(this);

        try {
            // 1. Initialize Safety & Config
            this.safetyManager = new StateSafetyManager(this);
            this.configManager = new ConfigManager(this);
            this.configManager.loadConfig();

            // 2. Initialize Storage
            String storageType = getConfig().getString("storage.type", "SQLITE").toUpperCase();
            if (storageType.equals("YAML")) {
                this.storageProvider = new com.example.lifesteal.storage.impl.YamlStorage(this);
            } else {
                this.storageProvider = new com.example.lifesteal.storage.impl.SqlStorage(this, storageType.equals("MYSQL"));
            }
            this.storageProvider.init();
            
            // 3. Migration
            this.migrationService = new MigrationService(this);
            this.migrationService.checkAndMigrate(this.storageProvider);

            // 4. Initialize Managers & Services
            this.heartRegistry = new HeartRegistry();
            this.healthManager = new HealthManager(this);
            this.combatService = new CombatService(this);
            this.discordService = new DiscordService(this);

            // 5. Hooks
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new com.example.lifesteal.hooks.LifeStealPlaceholders(this).register();
            }
            
            this.vaultHook = new com.example.lifesteal.hooks.VaultHook(this);
            if (!this.vaultHook.setupEconomy()) {
                MessageUtils.log(Level.WARNING, "Vault economy not found! Economy features will be disabled.");
            }

            // 6. Cleanup & Background Tasks
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                this.combatService.cleanup();
                
                // YamlStorage specific cleanups
                if (this.storageProvider instanceof com.example.lifesteal.storage.impl.YamlStorage yaml) {
                    yaml.cleanupLocks();
                }
            }, 6000L, 6000L); // Every 5 minutes

            // 7. Auto-Save Task (Every 2 minutes)
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                if (this.storageProvider instanceof com.example.lifesteal.storage.impl.YamlStorage yaml) {
                    for (java.util.UUID uuid : yaml.getDirtyPlayers()) {
                        com.example.lifesteal.data.PlayerData data = this.heartRegistry.getPlayerData(uuid);
                        if (data != null) {
                            yaml.savePlayer(data);
                        }
                    }
                }
            }, 2400L, 2400L);

            // 8. Register Listeners & Commands
            registerListeners();
            registerCommands();

            MessageUtils.log(Level.INFO, "LifeStealPlugin enabled successfully!");
        } catch (Exception e) {
            MessageUtils.log(Level.SEVERE, "Failed to enable LifeStealPlugin!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        
        // Force flush data
        if (storageProvider != null) {
            storageProvider.saveAllSync(heartRegistry.getAllCached().values());
            storageProvider.shutdown();
        }

        MessageUtils.log(Level.INFO, "LifeStealPlugin disabled successfully!");
    }

    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new com.example.lifesteal.listeners.LifeStealListener(this), this);
    }

    private void registerCommands() {
        com.example.lifesteal.commands.CommandRouter router = new com.example.lifesteal.commands.CommandRouter(this);
        getCommand("lifesteal").setExecutor(router);
        getCommand("lifesteal").setTabCompleter(router);
        
        getCommand("withdrawheart").setExecutor(new com.example.lifesteal.commands.WithdrawCommand(this));
        getCommand("buyheart").setExecutor(new com.example.lifesteal.commands.BuyCommand(this));
    }
}
