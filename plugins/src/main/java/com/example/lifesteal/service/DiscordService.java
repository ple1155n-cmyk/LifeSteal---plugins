package com.example.lifesteal.service;

import com.example.lifesteal.LifeStealPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class DiscordService {

    private final LifeStealPlugin plugin;

    public DiscordService(LifeStealPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendWebhook(String content) {
        String urlString = plugin.getConfig().getString("modules.discord-webhooks.webhook-url");
        if (urlString == null || urlString.isEmpty() || !plugin.getConfig().getBoolean("modules.discord-webhooks.enabled", false)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                java.net.URL url = java.net.URI.create(urlString).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "LifeStealPlugin-Enterprise");

                String json = "{\"content\": \"" + content.replace("\"", "\\\"") + "\"}";
                
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    plugin.getLogger().warning("Discord Webhook failed with code: " + code);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
