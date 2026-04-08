package com.example.lifesteal.listeners;

import com.example.lifesteal.LifeStealPlugin;
import com.example.lifesteal.data.PlayerData;
import com.example.lifesteal.utils.ItemUtils;
import com.example.lifesteal.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.logging.Level;

public class LifeStealListener implements Listener {

    private final LifeStealPlugin plugin;

    public LifeStealListener(LifeStealPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getStorageProvider().loadPlayer(player.getUniqueId()).thenAccept(optData -> {
            PlayerData data = optData.orElseGet(() -> PlayerData.builder()
                    .uuid(player.getUniqueId())
                    .lastKnownName(player.getName())
                    .currentMaxHearts(plugin.getConfig().getInt("gameplay.heart.default", 10))
                    .build());
            
            data.setLastKnownName(player.getName());
            data.setLastKnownIP(player.getAddress().getAddress().getHostAddress());
            
            plugin.getHeartRegistry().cachePlayer(data);
            
            // Apply on main thread
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getHealthManager().applyHealth(player, data));
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerData data = plugin.getHeartRegistry().getPlayerData(event.getPlayer().getUniqueId());
        if (data != null) {
            plugin.getStorageProvider().savePlayer(data);
            plugin.getHeartRegistry().removePlayer(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.equals(victim)) return;

        // Anti-Farm check
        if (!plugin.getCombatService().canSteal(killer.getUniqueId(), victim.getUniqueId())) {
            MessageUtils.sendMessage(killer, plugin.getConfigManager().getMessage("cooldown-warning"));
            return;
        }

        // IP Check
        String victimIP = victim.getAddress().getAddress().getHostAddress();
        String killerIP = killer.getAddress().getAddress().getHostAddress();
        if (victimIP.equals(killerIP) && plugin.getConfig().getString("gameplay.combat.ip-check", "BLOCK").equals("BLOCK")) {
            MessageUtils.log(Level.INFO, "Blocked heart gain for same-IP: " + killer.getName() + " and " + victim.getName());
            return;
        }

        // Steal Logic
        int gain = plugin.getConfig().getInt("gameplay.combat.gain-amount", 1);
        int loss = plugin.getConfig().getInt("gameplay.combat.loss-amount", 1);

        plugin.getHealthManager().modifyHearts(killer, gain);
        plugin.getHealthManager().modifyHearts(victim, -loss);

        plugin.getCombatService().recordKill(killer.getUniqueId(), victim.getUniqueId());

        // Check for ban
        PlayerData victimData = plugin.getHeartRegistry().getPlayerData(victim.getUniqueId());
        if (victimData != null && victimData.getCurrentMaxHearts() <= plugin.getConfig().getInt("gameplay.heart.min", 1)) {
            if (plugin.getConfig().getBoolean("ban.enabled", true)) {
                String reason = plugin.getConfig().getString("ban.reason", "Eliminated!");
                victim.kickPlayer(org.bukkit.ChatColor.translateAlternateColorCodes('&', reason));
                victim.ban(reason, (java.util.Date) null, null, true);
                plugin.adventure().all().sendMessage(MessageUtils.parse(plugin.getConfig().getString("ban.broadcast", "").replace("{player}", victim.getName())));
                
                if (plugin.getDiscordService() != null) {
                    plugin.getDiscordService().sendWebhook(":skull: Player **" + victim.getName() + "** has been eliminated!");
                }
            }
        }

        MessageUtils.sendMessage(killer, plugin.getConfigManager().getMessage("heart-gain").replace("{hearts}", String.valueOf(gain)));
        MessageUtils.sendMessage(victim, plugin.getConfigManager().getMessage("heart-loss").replace("{hearts}", String.valueOf(loss)));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;
        org.bukkit.inventory.ItemStack item = event.getItem();
        if (item == null || !ItemUtils.isHeartItem(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        PlayerData data = plugin.getHeartRegistry().getPlayerData(player.getUniqueId());
        if (data == null) return;

        int maxHearts = plugin.getConfig().getInt("gameplay.heart.max", 20);
        if (data.getCurrentMaxHearts() >= maxHearts) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("max-hearts-reached"));
            return;
        }

        item.setAmount(item.getAmount() - 1);
        plugin.getHealthManager().modifyHearts(player, 1);
        
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 1, 0), 5);
        
        MessageUtils.sendMessage(player, "<green>You used a heart item and gained +1 heart!</green>");
    }
}
