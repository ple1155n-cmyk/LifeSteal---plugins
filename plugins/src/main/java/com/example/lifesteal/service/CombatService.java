package com.example.lifesteal.service;

import com.example.lifesteal.LifeStealPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatService {

    private final LifeStealPlugin plugin;
    
    // Killer -> Victim -> LastKillTimestamp
    private final Map<UUID, Map<UUID, Long>> killCache = new ConcurrentHashMap<>();

    public CombatService(LifeStealPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean canSteal(UUID killer, UUID victim) {
        if (!plugin.getConfig().getBoolean("gameplay.combat.anti-farm.enabled", true)) {
            return true;
        }

        Map<UUID, Long> victimMap = killCache.get(killer);
        if (victimMap == null) return true;

        Long lastKill = victimMap.get(victim);
        if (lastKill == null) return true;

        long cooldown = plugin.getConfig().getLong("gameplay.combat.kill-cooldown", 3600) * 1000L;
        return (System.currentTimeMillis() - lastKill) > cooldown;
    }

    public void recordKill(UUID killer, UUID victim) {
        killCache.computeIfAbsent(killer, k -> new ConcurrentHashMap<>()).put(victim, System.currentTimeMillis());
    }

    public void cleanup() {
        long cooldown = plugin.getConfig().getLong("gameplay.combat.kill-cooldown", 3600) * 1000L;
        long now = System.currentTimeMillis();

        killCache.values().forEach(victimMap -> {
            victimMap.entrySet().removeIf(entry -> (now - entry.getValue()) > cooldown);
        });
        
        // Remove empty maps
        killCache.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
