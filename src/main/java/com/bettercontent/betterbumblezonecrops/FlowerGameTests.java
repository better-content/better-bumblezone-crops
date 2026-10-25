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
    @GameTest(template = "flower_lit", timeoutTicks = 20)
    public static void flowerRelocationConservesQuantity(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "flower-move"));
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var lamp = helper.absolutePos(new BlockPos(3, 1, 2));
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
        // The structure authors the lamp before execution, giving native lighting
        // preparation a head start without extending the 20-tick test deadline.
        helper.assertTrue(level.getBlockState(lamp).is(Blocks.GLOWSTONE), "Authored fixture lamp missing");
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos) == 14 && level.getRawBrightness(pos, 0) >= 8,
                "Native fixture illumination not ready at " + pos + ": " + level.getRawBrightness(pos, 0)))
            .thenExecute(() -> {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT));
                level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
                var cropEvent = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos.below(), hit);
                var target = cropEvent.getPos().relative(cropEvent.getFace());
                var heldId = ForgeRegistries.ITEMS.getKey(cropEvent.getItemStack().getItem());
                var matching = CultivarCatalog.ALL.stream().filter(c -> heldId != null && c.produce().contains(heldId.toString())).toList();
                var diagnostic = new com.google.gson.JsonObject();
                diagnostic.addProperty("held_id", heldId == null ? "<absent>" : heldId.toString());
                diagnostic.addProperty("dimension", level.dimension().location().toString());
                diagnostic.addProperty("target", target.toString());
                diagnostic.addProperty("face", cropEvent.getFace().toString());
                diagnostic.addProperty("soil", level.getBlockState(target.below()).toString());
                diagnostic.addProperty("target_state", level.getBlockState(target).toString());
                diagnostic.addProperty("headroom", level.getBlockState(target.above()).toString());
                diagnostic.addProperty("raw_brightness", level.getRawBrightness(target, 0));
                diagnostic.addProperty("can_see_sky", level.canSeeSky(target));
                boolean nativeSurvival = Blocks.CARROTS.defaultBlockState().canSurvive(level, target);
                diagnostic.addProperty("native_carrots_can_survive", nativeSurvival);
                var catalogue = new com.google.gson.JsonArray();
                for (var cultivar : matching) {
                    var row = new com.google.gson.JsonObject();
                    row.addProperty("id", cultivar.id()); row.addProperty("growth_form", cultivar.growthForm());
                    row.addProperty("seed_item", cultivar.seedItem()); row.addProperty("plant_blocks", cultivar.plantBlocks().toString());
                    catalogue.add(row);
                }
                diagnostic.add("matched_catalogue", catalogue);
                EdiblePlantingBlocker.onUseBlock(cropEvent);
                diagnostic.addProperty("event_canceled", cropEvent.isCanceled());
                com.mojang.logging.LogUtils.getLogger().info("flower-food-policy-fixture {}", diagnostic);
                helper.assertTrue(heldId != null && heldId.toString().equals("minecraft:carrot")
                        && matching.stream().anyMatch(c -> c.id().equals("carrot") && !c.growthForm().equals("flower")
                            && !c.seedItem().equals(heldId.toString())), "Food crop fixture lacks expected native held item/catalogue: " + diagnostic);
                helper.assertTrue(nativeSurvival, "Food crop fixture native planting precondition unsatisfied: " + diagnostic);
                helper.assertTrue(cropEvent.isCanceled(), "Flower exemption weakened food crop policy");
                // Same lamp, target, held item and observed native light; only soil changes.
                level.setBlockAndUpdate(target.below(), Blocks.DIRT.defaultBlockState());
                var dirtEvent = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos.below(), hit);
                boolean dirtSurvival = Blocks.CARROTS.defaultBlockState().canSurvive(level, target);
                int dirtBrightness = level.getRawBrightness(target, 0);
                EdiblePlantingBlocker.onUseBlock(dirtEvent);
                var negative = new com.google.gson.JsonObject();
                negative.addProperty("target", target.toString()); negative.addProperty("soil", level.getBlockState(target.below()).toString());
                negative.addProperty("raw_brightness", dirtBrightness); negative.addProperty("native_carrots_can_survive", dirtSurvival);
                negative.addProperty("event_canceled", dirtEvent.isCanceled());
                com.mojang.logging.LogUtils.getLogger().info("flower-food-policy-dirt-negative {}", negative);
                helper.assertTrue(level.getBlockState(lamp).is(Blocks.GLOWSTONE)
                        && dirtBrightness >= 8 && dirtBrightness == diagnostic.get("raw_brightness").getAsInt(), "DIRT negative did not retain actual native illumination: " + negative);
                helper.assertTrue(heldId.equals(ForgeRegistries.ITEMS.getKey(dirtEvent.getItemStack().getItem())), "DIRT negative held item drift");
                helper.assertTrue(!dirtSurvival, "DIRT negative unexpectedly permits native carrot survival: " + negative);
                helper.assertTrue(!dirtEvent.isCanceled(), "Conditional crop blocker canceled a native-unsurvivable DIRT target: " + negative);
            }).thenSucceed();
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
