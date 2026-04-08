package com.example.lifesteal.manager;

import com.example.lifesteal.data.PlayerData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HeartRegistry {

    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    public HeartRegistry() {
    }

    public void cachePlayer(PlayerData data) {
        playerDataMap.put(data.getUuid(), data);
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public void removePlayer(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    public Map<UUID, PlayerData> getAllCached() {
        return playerDataMap;
    }

    public void clear() {
        playerDataMap.clear();
    }
}
