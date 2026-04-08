package com.example.lifesteal.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerData {
    private UUID uuid;
    private String lastKnownName;
    private int currentMaxHearts;
    private long lastRevivedAt;
    private int totalRevivesPerformed;
    
    // For anti-farm/IP checks (not persisted, but cached)
    private transient String lastKnownIP;
    
    public int getHeartHP() {
        return currentMaxHearts * 2;
    }
}
