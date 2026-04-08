package com.example.lifesteal.commands;

import com.example.lifesteal.LifeStealPlugin;
import com.example.lifesteal.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandRouter implements CommandExecutor, TabCompleter {

    private final LifeStealPlugin plugin;

    public CommandRouter(LifeStealPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            MessageUtils.sendMessage(sender, "<gray>Running <gradient:#FF0000:#8B0000>LifeSteal v" + plugin.getDescription().getVersion() + "</gradient>");
            return true;
        }

        String sub = args[0].toLowerCase();
        
        switch (sub) {
            case "reload":
                if (!sender.hasPermission("lifesteal.admin")) {
                    MessageUtils.sendMessage(sender, plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                plugin.getConfigManager().loadConfig();
                MessageUtils.sendMessage(sender, plugin.getConfigManager().getMessage("reload-success"));
                break;
                
            case "debug":
                if (!sender.hasPermission("lifesteal.admin")) return true;
                showDebugInfo(sender);
                break;
                
            default:
                MessageUtils.sendMessage(sender, "<red>Unknown subcommand!</red>");
                break;
        }

        return true;
    }

    private void showDebugInfo(CommandSender sender) {
        MessageUtils.sendMessage(sender, "<aqua><b>LifeSteal Debug Info:</b></aqua>");
        MessageUtils.sendMessage(sender, " <gray>»</gray> Storage: <white>" + plugin.getConfig().getString("storage.type"));
        MessageUtils.sendMessage(sender, " <gray>»</gray> Cached Players: <white>" + plugin.getHeartRegistry().getAllCached().size());
        MessageUtils.sendMessage(sender, " <gray>»</gray> Restricted Mode: <white>" + plugin.getSafetyManager().isRestricted());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("lifesteal.admin")) {
                completions.add("reload");
                completions.add("debug");
            }
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return completions;
    }
}
