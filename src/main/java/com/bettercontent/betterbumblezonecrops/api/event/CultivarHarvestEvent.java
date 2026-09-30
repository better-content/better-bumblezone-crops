package com.bettercontent.betterbumblezonecrops.api.event;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraftforge.eventbus.api.Event;
import java.util.UUID;
/** A sourced, player-planted cultivar actually spawned its produce in the Overworld. */
public final class CultivarHarvestEvent extends Event {
 public final ServerLevel level;public final UUID owner;public final BlockPos position;public final String cultivar;public final UUID planting;
 public CultivarHarvestEvent(ServerLevel level,UUID owner,BlockPos position,String cultivar,UUID planting){this.level=level;this.owner=owner;this.position=position.immutable();this.cultivar=cultivar;this.planting=planting;}
}
