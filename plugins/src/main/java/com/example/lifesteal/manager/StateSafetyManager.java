package com.example.lifesteal.manager;

import com.example.lifesteal.LifeStealPlugin;
import com.example.lifesteal.utils.MessageUtils;

import java.util.logging.Level;

public class StateSafetyManager {

    private final LifeStealPlugin plugin;
    private boolean restrictedMode = false;

    public StateSafetyManager(LifeStealPlugin plugin) {
        this.plugin = plugin;
    }

    public void enterRestrictedMode(String reason) {
        if (restrictedMode) return;
        this.restrictedMode = true;
        MessageUtils.log(Level.SEVERE, "!!! ENTERING RESTRICTED MODE !!!");
        MessageUtils.log(Level.SEVERE, "Reason: " + reason);
        MessageUtils.broadcast("<red><b>[System]</b> LifeSteal has entered Restricted Mode due to a storage failure. Hearts will not be modified.</red>");
    }

    public boolean isRestricted() {
        return restrictedMode;
    }

    public void flushAllData() {
        if (plugin.getStorageProvider() != null && plugin.getHeartRegistry() != null) {
            MessageUtils.log(Level.INFO, "Performing emergency data flush...");
            plugin.getStorageProvider().saveAllSync(plugin.getHeartRegistry().getAllCached().values());
            MessageUtils.log(Level.INFO, "Flush complete.");
        }
    }
}
