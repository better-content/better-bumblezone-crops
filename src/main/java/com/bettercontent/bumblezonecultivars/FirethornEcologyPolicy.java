package com.bettercontent.bumblezonecultivars;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Stable, sparse hazard flora selection independent of chunk visitation order. */
final class FirethornEcologyPolicy {
    private static final int SITE_DIVISOR = 12;

    private FirethornEcologyPolicy() {}

    static boolean shouldPlaceAtSite(long worldSeed, ResourceLocation dimension, BlockPos site) {
        return CultivarChunkFinalizer.siteSelectionIndex(worldSeed, dimension, site, SITE_DIVISOR) == 0;
    }

    static boolean mayPlace(int successfulPlacements, boolean firethornAvailable) {
        return firethornAvailable && successfulPlacements < 1;
    }

    static int cardinalStart(BlockPos site) {
        return (int) Math.floorMod(site.asLong() ^ (site.getX() * 0x9E3779B9L) ^ site.getZ(), 4L);
    }
}
