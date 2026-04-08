package com.example.lifesteal.storage.impl;

import com.example.lifesteal.LifeStealPlugin;
import com.example.lifesteal.data.PlayerData;
import com.example.lifesteal.storage.StorageProvider;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class YamlStorage implements StorageProvider {

    private final LifeStealPlugin plugin;
    private final File playersFolder;
    
    // Concurrency Controls
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();
    private final Map<UUID, ReentrantLock> playerLocks = new ConcurrentHashMap<>();
    private ExecutorService ioExecutor;
    
    // Persistence State
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();

    public YamlStorage(LifeStealPlugin plugin) {
        this.plugin = plugin;
        this.playersFolder = new File(plugin.getDataFolder(), "userdata");
    }

    @Override
    public void init() throws Exception {
        if (!playersFolder.exists() && !playersFolder.mkdirs()) {
            throw new IOException("Failed to create userdata directory at: " + playersFolder.getAbsolutePath());
        }

        // Enterprise tuning: min(cores * 2, 8)
        int threads = Math.min(Runtime.getRuntime().availableProcessors() * 2, 8);
        this.ioExecutor = Executors.newFixedThreadPool(threads, r -> {
            Thread thread = new Thread(r, "LifeSteal-IO-Worker");
            thread.setPriority(Thread.NORM_PRIORITY - 1); // Lower priority than main/game threads
            return thread;
        });

        plugin.getLogger().info("YamlStorage initialized with " + threads + " I/O worker threads.");
    }

    @Override
    public void shutdown() {
        if (ioExecutor != null) {
            ioExecutor.shutdown();
            try {
                if (!ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    ioExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                ioExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        playerLocks.clear();
    }

    @Override
    public CompletableFuture<Optional<PlayerData>> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            globalLock.readLock().lock();
            ReentrantLock pLock = getPlayerLock(uuid);
            pLock.lock();
            try {
                File file = new File(playersFolder, uuid.toString() + ".yml");
                if (!file.exists()) return Optional.empty();

                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                
                // Validate required fields with defaults to prevent nulls
                String name = config.getString("name", "Unknown");
                int hearts = config.getInt("hearts", 20);
                long lastRevived = config.getLong("lastRevived", 0L);
                int totalRevives = config.getInt("totalRevives", 0);

                PlayerData data = PlayerData.builder()
                        .uuid(uuid)
                        .lastKnownName(name)
                        .currentMaxHearts(hearts)
                        .lastRevivedAt(lastRevived)
                        .totalRevivesPerformed(totalRevives)
                        .build();
                
                return Optional.of(data);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid + " from " + playersFolder.getAbsolutePath(), e);
                return Optional.empty();
            } finally {
                pLock.unlock();
                globalLock.readLock().unlock();
            }
        }, ioExecutor);
    }

    @Override
    public CompletableFuture<Void> savePlayer(PlayerData data) {
        dirtyPlayers.add(data.getUuid());
        return CompletableFuture.runAsync(() -> {
            saveSyncInternal(data);
        }, ioExecutor);
    }

    @Override
    public CompletableFuture<Void> saveAll(Collection<PlayerData> data) {
        return CompletableFuture.runAsync(() -> saveAllSync(data), ioExecutor);
    }

    @Override
    public void saveAllSync(Collection<PlayerData> data) {
        globalLock.writeLock().lock();
        try {
            for (PlayerData player : data) {
                saveSyncInternal(player);
            }
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    @Override
    public CompletableFuture<Collection<PlayerData>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            // Snapshot file list first to minimize lock time
            File[] files = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files == null) return Collections.emptyList();

            List<PlayerData> players = new ArrayList<>();
            for (File file : files) {
                try {
                    String uuidStr = file.getName().replace(".yml", "");
                    UUID uuid = UUID.fromString(uuidStr);
                    loadPlayer(uuid).thenAccept(opt -> opt.ifPresent(players::add)).join();
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Skipping invalid player data file: " + file.getName());
                }
            }
            return players;
        }, ioExecutor);
    }

    private void saveSyncInternal(PlayerData data) {
        ReentrantLock pLock = getPlayerLock(data.getUuid());
        pLock.lock();
        try {
            File file = new File(playersFolder, data.getUuid() + ".yml");
            
            // Retry system (3 attempts)
            boolean success = false;
            Exception lastEx = null;
            
            for (int i = 0; i < 3; i++) {
                try {
                    FileConfiguration config = new YamlConfiguration();
                    config.set("name", data.getLastKnownName());
                    config.set("hearts", data.getCurrentMaxHearts());
                    config.set("lastRevived", data.getLastRevivedAt());
                    config.set("totalRevives", data.getTotalRevivesPerformed());
                    
                    config.save(file);
                    success = true;
                    dirtyPlayers.remove(data.getUuid());
                    break;
                } catch (IOException e) {
                    lastEx = e;
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                }
            }

            if (!success) {
                handleSaveFailure(data, file, lastEx);
            }
        } finally {
            pLock.unlock();
        }
    }

    private void handleSaveFailure(PlayerData data, File file, Exception ex) {
        plugin.getLogger().log(Level.SEVERE, "FATAL: Could not save data for " + data.getUuid() + " after 3 attempts!", ex);
        
        // Corruption handling (rename existing file if it exists and might be corrupting writes)
        if (file.exists()) {
            File corruptFile = new File(file.getParent(), file.getName() + ".corrupt");
            if (file.renameTo(corruptFile)) {
                plugin.getLogger().severe("Existing data file was moved to " + corruptFile.getName() + " due to persistence failure.");
            }
        }
    }

    private ReentrantLock getPlayerLock(UUID uuid) {
        return playerLocks.computeIfAbsent(uuid, k -> new ReentrantLock());
    }
    
    // For Lock cleanup (can be called periodically)
    public void cleanupLocks() {
        playerLocks.entrySet().removeIf(entry -> !entry.getValue().isLocked());
    }

    public Set<UUID> getDirtyPlayers() {
        return dirtyPlayers;
    }
}
