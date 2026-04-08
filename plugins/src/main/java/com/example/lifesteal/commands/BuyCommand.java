package com.example.lifesteal.commands;

import com.example.lifesteal.LifeStealPlugin;
import com.example.lifesteal.data.PlayerData;
import com.example.lifesteal.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BuyCommand implements CommandExecutor {

    private final LifeStealPlugin plugin;

    public BuyCommand(LifeStealPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMessage(sender, "<red>Only players can use this command!</red>");
            return true;
        }

        if (!plugin.getConfig().getBoolean("modules.vault-economy.enabled", true) || plugin.getVaultHook().getEconomy() == null) {
            MessageUtils.sendMessage(player, "<red>Economy features are currently disabled.</red>");
            return true;
        }

        PlayerData data = plugin.getHeartRegistry().getPlayerData(player.getUniqueId());
        if (data == null) return true;

        int maxHearts = plugin.getConfig().getInt("gameplay.heart.max", 20);
        if (data.getCurrentMaxHearts() >= maxHearts) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("max-hearts-reached"));
            return true;
        }

        double cost = plugin.getConfig().getDouble("modules.vault-economy.heart-cost", 5000.0);
        if (!plugin.getVaultHook().getEconomy().has(player, cost)) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("not-enough-money").replace("{cost}", String.valueOf(cost)));
            return true;
        }

        plugin.getVaultHook().getEconomy().withdrawPlayer(player, cost);
        plugin.getHealthManager().modifyHearts(player, 1);

        MessageUtils.sendMessage(player, "<green>You have purchased 1 heart for $" + cost + "!</green>");
        return true;
    }
}
