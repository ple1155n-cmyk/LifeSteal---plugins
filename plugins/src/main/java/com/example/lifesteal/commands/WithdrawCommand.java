package com.example.lifesteal.commands;

import com.example.lifesteal.LifeStealPlugin;
import com.example.lifesteal.data.PlayerData;
import com.example.lifesteal.events.PreHeartChangeEvent;
import com.example.lifesteal.utils.ItemUtils;
import com.example.lifesteal.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WithdrawCommand implements CommandExecutor {

    private final LifeStealPlugin plugin;

    public WithdrawCommand(LifeStealPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMessage(sender, "<red>Only players can use this command!</red>");
            return true;
        }

        PlayerData data = plugin.getHeartRegistry().getPlayerData(player.getUniqueId());
        if (data == null) return true;

        int minHearts = plugin.getConfig().getInt("gameplay.heart.min", 1);
        if (data.getCurrentMaxHearts() <= minHearts) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("min-hearts-reached"));
            return true;
        }

        // Call Custom Event
        PreHeartChangeEvent event = new PreHeartChangeEvent(player, data, -1);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return true;

        // Modify hearts
        plugin.getHealthManager().modifyHearts(player, -1);
        
        // Give heart item
        player.getInventory().addItem(ItemUtils.getHeartItem()).forEach((idx, item) -> {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        });

        MessageUtils.sendMessage(player, "<green>You have withdrawn 1 heart!</green>");
        return true;
    }
}
