package com.example.lifesteal.storage.impl;

import com.example.lifesteal.LifeStealPlugin;
import com.example.lifesteal.data.PlayerData;
import com.example.lifesteal.storage.StorageProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;


public class SqlStorage implements StorageProvider {

    private final LifeStealPlugin plugin;
    private HikariDataSource dataSource;
    private final boolean isMySQL;

    public SqlStorage(LifeStealPlugin plugin, boolean isMySQL) {
        this.plugin = plugin;
        this.isMySQL = isMySQL;
    }

    @Override
    public void init() throws Exception {
        HikariConfig config = new HikariConfig();
        
        if (isMySQL) {
            String host = plugin.getConfig().getString("storage.mysql.host");
            int port = plugin.getConfig().getInt("storage.mysql.port");
            String database = plugin.getConfig().getString("storage.mysql.database");
            String username = plugin.getConfig().getString("storage.mysql.username");
            String password = plugin.getConfig().getString("storage.mysql.password");
            
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else {
            File dbFile = new File(plugin.getDataFolder(), "lifesteal.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(plugin.getConfig().getInt("storage.mysql.pool-size", 10));
        config.setPoolName("LifeStealPool");
        
        this.dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            String query = "CREATE TABLE IF NOT EXISTS lifesteal_players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16), " +
                    "hearts INT, " +
                    "last_revived BIGINT, " +
                    "total_revives INT)";
            stmt.executeUpdate(query);
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public CompletableFuture<Optional<PlayerData>> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM lifesteal_players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return Optional.of(PlayerData.builder()
                            .uuid(uuid)
                            .lastKnownName(rs.getString("name"))
                            .currentMaxHearts(rs.getInt("hearts"))
                            .lastRevivedAt(rs.getLong("last_revived"))
                            .totalRevivesPerformed(rs.getInt("total_revives"))
                            .build());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Void> savePlayer(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "REPLACE INTO lifesteal_players (uuid, name, hearts, last_revived, total_revives) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, data.getUuid().toString());
                ps.setString(2, data.getLastKnownName());
                ps.setInt(3, data.getCurrentMaxHearts());
                ps.setLong(4, data.getLastRevivedAt());
                ps.setInt(5, data.getTotalRevivesPerformed());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveAll(Collection<PlayerData> data) {
        return CompletableFuture.runAsync(() -> saveAllSync(data));
    }

    @Override
    public void saveAllSync(Collection<PlayerData> data) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "REPLACE INTO lifesteal_players (uuid, name, hearts, last_revived, total_revives) VALUES (?, ?, ?, ?, ?)")) {
            conn.setAutoCommit(false);
            for (PlayerData player : data) {
                ps.setString(1, player.getUuid().toString());
                ps.setString(2, player.getLastKnownName());
                ps.setInt(3, player.getCurrentMaxHearts());
                ps.setLong(4, player.getLastRevivedAt());
                ps.setInt(5, player.getTotalRevivesPerformed());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Collection<PlayerData>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerData> players = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM lifesteal_players")) {
                while (rs.next()) {
                    players.add(PlayerData.builder()
                            .uuid(UUID.fromString(rs.getString("uuid")))
                            .lastKnownName(rs.getString("name"))
                            .currentMaxHearts(rs.getInt("hearts"))
                            .lastRevivedAt(rs.getLong("last_revived"))
                            .totalRevivesPerformed(rs.getInt("total_revives"))
                            .build());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return players;
        });
    }
}
