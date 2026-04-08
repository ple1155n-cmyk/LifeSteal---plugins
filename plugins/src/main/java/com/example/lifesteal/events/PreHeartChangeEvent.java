package com.example.lifesteal.events;

import com.example.lifesteal.data.PlayerData;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PreHeartChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final PlayerData data;
    @Setter
    private int heartDelta;
    @Setter
    private boolean cancelled;

    public PreHeartChangeEvent(Player player, PlayerData data, int heartDelta) {
        this.player = player;
        this.data = data;
        this.heartDelta = heartDelta;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
