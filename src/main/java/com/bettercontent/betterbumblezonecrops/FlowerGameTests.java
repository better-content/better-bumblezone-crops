package com.bettercontent.betterbumblezonecrops;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(BumblezoneCultivars.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FlowerGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void flowerRelocationConservesQuantity(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "flower-move"));
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
        var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false);
        for (var hand : InteractionHand.values()) {
            var flower = new ItemStack(Items.DANDELION);
            player.setItemInHand(hand, flower);
            var event = new PlayerInteractEvent.RightClickBlock(player, hand, pos.below(), hit);
            EdiblePlantingBlocker.onUseBlock(event);
            helper.assertTrue(!event.isCanceled(), "Flower hit crop planting blocker: " + hand);
        }
        for (int cycle = 0; cycle < 20; cycle++) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DANDELION));
            helper.assertTrue(Items.DANDELION.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                "Native flower item did not relocate");
            var drops = Block.getDrops(level.getBlockState(pos), level, pos, null);
            helper.assertTrue(drops.stream().filter(s -> s.is(Items.DANDELION)).mapToInt(ItemStack::getCount).sum() == 1,
                "Relocation changed flower quantity");
            helper.assertTrue(drops.stream().noneMatch(FlowerPolicy::isSeed), "Off-origin relocation generated seed");
            level.removeBlock(pos, false);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT));
        level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        var cropEvent = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos.below(), hit);
        EdiblePlantingBlocker.onUseBlock(cropEvent);
        helper.assertTrue(cropEvent.isCanceled(), "Flower exemption weakened food crop policy");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void seedsPlaceOnePetalUnitAndNativeBonemealCannotMultiplyIt(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "petal-seed"));
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
        var seed = new ItemStack(BumblezoneCultivars.DEDICATED_SEEDS.get("minecraft_pink_petals_seeds").get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, seed);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false);
        helper.assertTrue(seed.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "Seed did not place petals");
        helper.assertTrue(seed.getCount() == 1 && level.getBlockState(pos).getValue(PinkPetalsBlock.AMOUNT) == 1,
            "One seed must place exactly one petal unit");
        var petals = level.getBlockState(pos);
        ((PinkPetalsBlock) Blocks.PINK_PETALS).performBonemeal(level, level.random, pos, petals);
        helper.assertTrue(level.getBlockState(pos).getValue(PinkPetalsBlock.AMOUNT) == 1, "Direct native bonemeal multiplied petals outside Bumblezone");
        var drops = java.util.List.of(new ItemStack(Items.PINK_PETALS, 2));
        var cultivar = CultivarCatalog.byPlant("minecraft:pink_petals");
        int count = FlowerPolicy.harvestSeeds(cultivar, drops, true, RandomSource.create(17));
        helper.assertTrue(count >= 2 && count <= 4, "Two recovered petal units need two origin rolls");
        helper.assertTrue(FlowerPolicy.harvestSeeds(cultivar, drops, false, RandomSource.create(17)) == 0, "Quantity-bearing flower leaked seeds");
        helper.assertTrue(FlowerPolicy.harvestSeeds(cultivar, java.util.List.of(), true, RandomSource.create(17)) == 0, "No recovered flower must mean no propagation");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void tallHalfLootAndSnifferDiggingCannotLeakOffOriginSeeds(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
        net.minecraft.world.level.block.DoublePlantBlock.placeAt(level, Blocks.SUNFLOWER.defaultBlockState(), pos, 3);
        for (var half : new BlockPos[]{pos, pos.above()}) {
            var drops = Block.getDrops(level.getBlockState(half), level, half, null);
            helper.assertTrue(drops.stream().noneMatch(FlowerPolicy::isSeed), "Tall flower half leaked off-origin seed");
        }
        var sniffer = net.minecraft.world.entity.EntityType.SNIFFER.create(level);
        helper.assertTrue(sniffer != null, "Native sniffer unavailable");
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, sniffer)
            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.GIFT);
        var table = level.getServer().getLootData().getLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.SNIFFER_DIGGING);
        for (int roll = 0; roll < 10; roll++) {
            helper.assertTrue(table.getRandomItems(params).stream().noneMatch(s -> s.is(Items.TORCHFLOWER_SEEDS) || s.is(Items.PITCHER_POD)),
                "Sniffer created new flower planting resources outside Bumblezone");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void installedFlowerTagsHaveExplicitClassification(GameTestHelper helper) throws Exception {
        try (var stream = FlowerGameTests.class.getResourceAsStream("/defaults/flower_tag_exclusions.json")) {
            if (stream == null) throw new IllegalStateException("Missing authored flower exclusions");
            var exclusions = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var block : ForgeRegistries.BLOCKS.getValues()) {
                var state = block.defaultBlockState();
                // Some generated mods use FlowerBlock even for bones, corpse spawners and
                // pebbles. Inheritance is not semantic flower membership; untagged flowers
                // have a separate reviewed inventory in check-flower-contract.py.
                if (!state.is(BlockTags.FLOWERS)) continue;
                String id = ForgeRegistries.BLOCKS.getKey(block).toString();
                var cultivar = CultivarCatalog.byPlant(id);
                helper.assertTrue(exclusions.has(id) || cultivar != null && cultivar.growthForm().equals("flower"),
                    "Installed flower lacks explicit classification: " + id);
            }
        }
        helper.succeed();
    }
}
