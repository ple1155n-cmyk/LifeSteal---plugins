package com.example.lifesteal.api.v1;

import com.example.lifesteal.LifeStealPlugin;
import com.example.lifesteal.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class LifeStealAPI {

    private static LifeStealAPI instance;

    public static LifeStealAPI getInstance() {
        if (instance == null) {
            instance = new LifeStealAPI();
        }
        return instance;
    }

    private LifeStealAPI() {}

    /**
     * Get the current hearts of a player.
     * @param player The player to check.
     * @return The heart count.
     */
    public int getHearts(Player player) {
        PlayerData data = LifeStealPlugin.getInstance().getHeartRegistry().getPlayerData(player.getUniqueId());
        return data != null ? data.getCurrentMaxHearts() : 0;
    }

    /**
     * Get player data for a specific UUID.
     * @param uuid The UUID of the player.
     * @return Optional PlayerData.
     */
    public Optional<PlayerData> getPlayerData(UUID uuid) {
        return Optional.ofNullable(LifeStealPlugin.getInstance().getHeartRegistry().getPlayerData(uuid));
    }

    /**
     * Set the hearts of a player.
     * @param player The player to modify.
     * @param amount The new heart count.
     */
    public void setHearts(Player player, int amount) {
        PlayerData data = LifeStealPlugin.getInstance().getHeartRegistry().getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setCurrentMaxHearts(amount);
            LifeStealPlugin.getInstance().getHealthManager().applyHealth(player, data);
        }
    }

    /**
     * Add or subtract hearts from a player.
     * @param player The player to modify.
     * @param delta The amount to add (can be negative).
     */
    public void addHearts(Player player, int delta) {
        LifeStealPlugin.getInstance().getHealthManager().modifyHearts(player, delta);
    }
}
