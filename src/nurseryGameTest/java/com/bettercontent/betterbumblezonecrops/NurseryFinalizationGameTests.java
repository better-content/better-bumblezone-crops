package com.bettercontent.betterbumblezonecrops;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Detached native chunks model the unpublished ChunkEvent.Load boundary without world backfill. */
@GameTestHolder("better_bumblezone_crops_nursery")
@PrefixGameTestTemplate(false)
public final class NurseryFinalizationGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void onlyNewBumblezoneChunksFinalizeOnceWithBoundedPlantingRewards(GameTestHelper helper) {
        var dimension = new ResourceLocation("the_bumblezone", "the_bumblezone");
        var level = helper.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        helper.assertTrue(level != null, "Pinned native Bumblezone dimension must be loaded");
        var chunk = new LevelChunk(level, new ChunkPos(100_000, 100_000));
        var pollen = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("the_bumblezone", "pile_of_pollen"));
        helper.assertTrue(pollen != null && pollen != Blocks.AIR, "Pinned pollen block must resolve");
        int y = level.getMinBuildHeight() + 20;
        var sites = new java.util.ArrayList<BlockPos>();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            var pos = new BlockPos(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
            // Seed an unpublished section as worldgen does, without pollen placement callbacks into the level.
            chunk.getSection(chunk.getSectionIndex(y)).setBlockState(x, y & 15, z, pollen.defaultBlockState());
            sites.add(pos.above());
        }
        CultivarChunkFinalizer.onChunkLoad(new ChunkEvent.Load(chunk, false));
        helper.assertTrue(sites.stream().allMatch(pos -> chunk.getBlockState(pos).isAir()),
                "Previously generated chunks must never be backfilled");
        CultivarChunkFinalizer.onChunkLoad(new ChunkEvent.Load(chunk, true));
        var nurseries = sites.stream().filter(pos -> chunk.getBlockState(pos).is(BumblezoneCultivars.LIVING_POLLEN_NURSERY.get())).toList();
        helper.assertTrue(nurseries.size() == 24, "Abundant native sites must stop at the 24-nursery bound");
        for (var pos : nurseries) {
            var blockEntity = (LivingPollenNurseryBlockEntity) chunk.getBlockEntity(pos);
            helper.assertTrue(blockEntity != null, "Each placement must initialize its native block entity");
            var drops = Block.getDrops(chunk.getBlockState(pos), level, pos, blockEntity);
            helper.assertTrue(drops.size() == 1 && drops.get(0).getCount() == 4,
                    "Each nursery must return exactly four planting items");
            helper.assertTrue(ForgeRegistries.ITEMS.getKey(drops.get(0).getItem()).toString().equals(blockEntity.seedId()),
                    "Reward must match the initialized cultivar, not the fallback seed");
            helper.assertTrue(drops.get(0).getTag() != null
                            && dimension.toString().equals(drops.get(0).getTag().getString(CultivarPlantings.ORIGIN)),
                    "Planting rewards must carry Bumblezone origin");
        }
        helper.assertTrue(CultivarFinalizationData.get(level).contains(chunk.getPos().toLong()), "Finalization must persist its chunk key");
        CultivarChunkFinalizer.onChunkLoad(new ChunkEvent.Load(chunk, true));
        helper.assertTrue(sites.stream().filter(pos -> chunk.getBlockState(pos).is(BumblezoneCultivars.LIVING_POLLEN_NURSERY.get())).toList().equals(nurseries),
                "Even a repeated new-chunk event must not add nurseries to the remaining 232 eligible sites");
        var overworldChunk = new LevelChunk(helper.getLevel(), new ChunkPos(100_001, 100_001));
        CultivarChunkFinalizer.onChunkLoad(new ChunkEvent.Load(overworldChunk, true));
        helper.assertTrue(!CultivarFinalizationData.get(helper.getLevel()).contains(overworldChunk.getPos().toLong()),
                "Other dimensions must not be finalized");
        helper.succeed();
    }
}
