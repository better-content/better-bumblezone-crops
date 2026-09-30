package com.bettercontent.betterbumblezonecrops;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class LivingPollenNurseryBlockEntity extends BlockEntity {
    private String seedId = "minecraft:wheat_seeds";
    public LivingPollenNurseryBlockEntity(BlockPos pos, BlockState state) { super(BumblezoneCultivars.NURSERY_BE.get(), pos, state); }
    public String seedId() { return seedId; }
    /** Initializes worldgen data while the containing chunk is still completing its load event. */
    void initializeSeedId(String value) { seedId = value; }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putString("Seed", seedId); }
    @Override public void load(CompoundTag tag) { super.load(tag); if (tag.contains("Seed")) seedId = tag.getString("Seed"); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) { if (packet.getTag() != null) load(packet.getTag()); }
}
