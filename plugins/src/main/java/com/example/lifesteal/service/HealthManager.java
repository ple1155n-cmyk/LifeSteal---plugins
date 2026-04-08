package com.example.lifesteal.service;

import com.example.lifesteal.LifeStealPlugin;
import com.example.lifesteal.data.PlayerData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public class HealthManager {

    private final LifeStealPlugin plugin;

    public HealthManager(LifeStealPlugin plugin) {
        this.plugin = plugin;
    }

    public void applyHealth(Player player, PlayerData data) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute == null) return;

        double newMax = data.getHeartHP();
        attribute.setBaseValue(newMax);

        // Optional: Sync current health
        if (plugin.getConfig().getBoolean("gameplay.heart.heal-on-gain", true)) {
            player.setHealth(newMax);
        }
    }

    public void modifyHearts(Player player, int heartDelta) {
        PlayerData data = plugin.getHeartRegistry().getPlayerData(player.getUniqueId());
        if (data == null) return;

        int min = plugin.getConfig().getInt("gameplay.heart.min", 1);
        int max = plugin.getConfig().getInt("gameplay.heart.max", 20);

        int newHearts = Math.clamp(data.getCurrentMaxHearts() + heartDelta, min, max);
        
        // Check for ban condition separately in the listener, but here we just clamp
        data.setCurrentMaxHearts(newHearts);
        applyHealth(player, data);
        
        // Queue for saving
        plugin.getStorageProvider().savePlayer(data);
    }
}
