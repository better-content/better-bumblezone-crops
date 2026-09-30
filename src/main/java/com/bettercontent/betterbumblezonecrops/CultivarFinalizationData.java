package com.bettercontent.betterbumblezonecrops;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Dimension-local record ensuring fresh-chunk origin processing runs exactly once. */
final class CultivarFinalizationData extends SavedData {
    private static final String NAME = "better_bumblezone_crops_finalized";
    private final LongOpenHashSet chunks = new LongOpenHashSet();

    static CultivarFinalizationData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(CultivarFinalizationData::load, CultivarFinalizationData::new, NAME);
    }

    static CultivarFinalizationData load(CompoundTag tag) {
        CultivarFinalizationData data = new CultivarFinalizationData();
        data.chunks.addAll(LongOpenHashSet.of(tag.getLongArray("Chunks")));
        return data;
    }

    boolean contains(long chunk) {
        return chunks.contains(chunk);
    }

    void add(long chunk) {
        if (chunks.add(chunk)) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("Chunks", chunks.toLongArray());
        return tag;
    }
}
