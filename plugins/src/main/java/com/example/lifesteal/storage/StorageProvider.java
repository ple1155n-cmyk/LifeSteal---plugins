package com.example.lifesteal.storage;

import com.example.lifesteal.data.PlayerData;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider {

    void init() throws Exception;

    void shutdown();

    CompletableFuture<Optional<PlayerData>> loadPlayer(UUID uuid);

    CompletableFuture<Void> savePlayer(PlayerData data);

    CompletableFuture<Void> saveAll(Collection<PlayerData> data);

    void saveAllSync(Collection<PlayerData> data);
    
    CompletableFuture<Collection<PlayerData>> loadAll();
}
