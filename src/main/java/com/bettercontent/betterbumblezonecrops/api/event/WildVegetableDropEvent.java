package com.bettercontent.betterbumblezonecrops.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** A real player's wild Overworld crop (including rice) actually spawned its seedless produce.
 * The historical public event name is retained for consumer compatibility.
 */
public final class WildVegetableDropEvent extends Event {
    public final ServerPlayer player;
    public final BlockPos position;

    public WildVegetableDropEvent(ServerPlayer player, BlockPos position) {
        this.player = player;
        this.position = position.immutable();
    }
}
