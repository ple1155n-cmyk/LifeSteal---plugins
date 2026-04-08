package com.example.lifesteal.utils;

import com.example.lifesteal.LifeStealPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;

public class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String PREFIX = "<gradient:#FF0000:#8B0000><b>LifeSteal</b></gradient> <gray>»</gray> ";

    public static void log(Level level, String message) {
        LifeStealPlugin.getInstance().getLogger().log(level, message);
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            LifeStealPlugin.getInstance().adventure().player(player).sendMessage(parse(PREFIX + message));
        } else {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message.replaceAll("<[^>]*>", "")));
        }
    }

    public static void sendMessage(Player player, String message) {
        LifeStealPlugin.getInstance().adventure().player(player).sendMessage(parse(PREFIX + message));
    }

    public static void sendActionBar(Player player, String message) {
        LifeStealPlugin.getInstance().adventure().player(player).sendActionBar(parse(message));
    }

    public static void broadcast(String message) {
        LifeStealPlugin.getInstance().adventure().all().sendMessage(parse(PREFIX + message));
    }

    public static Component parse(String message) {
        return MINI_MESSAGE.deserialize(message);
    }
}
