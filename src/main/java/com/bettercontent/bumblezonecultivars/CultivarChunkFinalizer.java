package com.bettercontent.bumblezonecultivars;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public final class CultivarChunkFinalizer {
    private static final ResourceLocation BUMBLEZONE = new ResourceLocation("the_bumblezone", "the_bumblezone");
    private static final ResourceLocation POLLEN = new ResourceLocation("the_bumblezone", "pile_of_pollen");
    private static final long NURSERY_SELECTION_SALT = 0x43554C5449564152L; // "CULTIVAR"
    private CultivarChunkFinalizer() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        // This is worldgen finalization, never a scan or backfill of an ordinary loaded chunk.
        if (!event.isNewChunk()) return;
        if (!level.dimension().location().equals(BUMBLEZONE)) return;
        CultivarFinalizationData finalized = CultivarFinalizationData.get(level);
        long chunkKey = chunk.getPos().toLong();
        if (finalized.contains(chunkKey)) return;
        placeNurseries(chunk, level.getSeed(), level.dimension().location(), level.getMinBuildHeight(), level.getMaxBuildHeight());
        finalized.add(chunkKey);
    }

    private static void placeNurseries(LevelChunk chunk, long worldSeed, ResourceLocation dimension, int minBuildHeight, int maxBuildHeight) {
        List<CultivarDefinition> choices = CultivarCatalog.ALL.stream().filter(c -> c.originDimensions().contains(BUMBLEZONE.toString()) && CultivarCatalog.resolves(c)).toList();
        if (choices.isEmpty()) return;
        int placed = 0, minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        for (int x = minX; x <= minX + 15 && placed < 24; x++) for (int z = minZ; z <= minZ + 15 && placed < 24; z++) for (int y = maxBuildHeight - 2; y >= minBuildHeight && placed < 24; y--) {
            BlockPos pollenPos = new BlockPos(x, y, z), hostPos = pollenPos.above();
            ResourceLocation below = ForgeRegistries.BLOCKS.getKey(chunk.getBlockState(pollenPos).getBlock());
            if (!POLLEN.equals(below) || !chunk.getBlockState(hostPos).isAir()) continue;
            CultivarDefinition chosen = choices.get(siteSelectionIndex(worldSeed, dimension, hostPos, choices.size()));
            chunk.setBlockState(hostPos, BumblezoneCultivars.LIVING_POLLEN_NURSERY.get().defaultBlockState(), false);
            if (chunk.getBlockEntity(hostPos) instanceof LivingPollenNurseryBlockEntity nursery) {
                // BlockEntity#setChanged asks the level for this chunk again. During ChunkEvent.Load
                // that re-enters ServerChunkCache's incomplete future and can stall the server thread.
                nursery.initializeSeedId(chosen.seedItem());
                chunk.setUnsaved(true);
            }
            placed++;
        }
    }

    /** Stable per-site selection: generation order and unrelated world RNG use cannot alter it. */
    static int siteSelectionIndex(long worldSeed, ResourceLocation dimension, BlockPos site, int choices) {
        if (choices <= 0) throw new IllegalArgumentException("choices must be positive");
        long value = mix64(worldSeed ^ NURSERY_SELECTION_SALT);
        value = mix64(value ^ dimension.toString().hashCode());
        value = mix64(value ^ site.asLong());
        return (int) Math.floorMod(value, (long) choices);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
