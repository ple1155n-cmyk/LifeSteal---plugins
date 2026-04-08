package com.example.lifesteal.events;

import com.example.lifesteal.data.PlayerData;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PostHeartChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final PlayerData data;
    private final int heartDelta;
    private final int newHearts;

    public PostHeartChangeEvent(Player player, PlayerData data, int heartDelta, int newHearts) {
        this.player = player;
        this.data = data;
        this.heartDelta = heartDelta;
        this.newHearts = newHearts;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
