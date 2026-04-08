package com.example.lifesteal.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class PlayerReviveEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final CommandSender reviver;
    private final UUID targetUUID;
    private final String targetName;
    @Setter
    private boolean cancelled;

    public PlayerReviveEvent(CommandSender reviver, UUID targetUUID, String targetName) {
        this.reviver = reviver;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
