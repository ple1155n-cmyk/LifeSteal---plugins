package com.example.lifesteal.hooks;

import com.example.lifesteal.LifeStealPlugin;
import com.example.lifesteal.data.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class LifeStealPlaceholders extends PlaceholderExpansion {

    private final LifeStealPlugin plugin;

    public LifeStealPlaceholders(LifeStealPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "lifesteal";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Antigravity";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return null;

        PlayerData data = plugin.getHeartRegistry().getPlayerData(player.getUniqueId());
        if (data == null) return "0";

        switch (params.toLowerCase()) {
            case "hearts":
                return String.valueOf(data.getCurrentMaxHearts());
            case "max_hearts":
                return String.valueOf(plugin.getConfig().getInt("gameplay.heart.max", 20));
            case "hp":
                return String.valueOf(data.getHeartHP());
            default:
                return null;
        }
    }
}
