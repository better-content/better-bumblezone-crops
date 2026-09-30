package com.bettercontent.betterbumblezonecrops.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** A player's wild Overworld vegetable actually spawned its seedless produce. */
public final class WildVegetableDropEvent extends Event {
    public final ServerPlayer player;
    public final BlockPos position;

    public WildVegetableDropEvent(ServerPlayer player, BlockPos position) {
        this.player = player;
        this.position = position.immutable();
    }
}
