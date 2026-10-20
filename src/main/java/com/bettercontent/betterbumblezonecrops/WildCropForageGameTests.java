package com.bettercontent.betterbumblezonecrops;

import com.bettercontent.betterbumblezonecrops.api.event.WildVegetableDropEvent;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;
import java.util.UUID;

/** Native FD behavior tests; these do not establish full-pack placement/density acceptance. */
@GameTestHolder(BumblezoneCultivars.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WildCropForageGameTests {
    public static final class Probe {
        int count;
        @SubscribeEvent public void drop(WildVegetableDropEvent event) { count++; }
    }

    private static BlockState plant(GameTestHelper helper, BlockPos pos, String id) {
        var level = helper.getLevel();
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        helper.assertTrue(block != null && block != Blocks.AIR, "Pinned FD crop missing: " + id);
        level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        if (id.equals("farmersdelight:wild_rice")) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            DoublePlantBlock.placeAt(level, block.defaultBlockState(), pos, 3);
        } else {
            level.setBlock(pos, block.defaultBlockState(), 3);
        }
        return level.getBlockState(pos);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nativeWildLootNeverYieldsPropagules(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var fortune = new ItemStack(Items.DIAMOND_AXE);
        fortune.enchant(Enchantments.BLOCK_FORTUNE, 3);
        var silk = new ItemStack(Items.DIAMOND_AXE);
        silk.enchant(Enchantments.SILK_TOUCH, 1);
        var tools = List.of(ItemStack.EMPTY, new ItemStack(Items.SHEARS), fortune, silk);
        var fake = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "forage-machine"));
        for (var id : WildCropForage.PRODUCE.keySet()) {
            var state = plant(helper, pos, id);
            int edible = 0;
            for (var tool : tools) {
                for (int attempt = 0; attempt < 64; attempt++) {
                    for (var stack : Block.getDrops(state, level, pos, null, fake, tool)) {
                        var itemId = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                        helper.assertTrue(WildCropForage.allowsDrop(id, itemId), id + " leaked " + itemId);
                        edible += stack.getCount();
                    }
                }
            }
            helper.assertTrue(edible > 0, id + " never yielded its native edible produce");
            if (state.getBlock() instanceof BonemealableBlock growth) {
                helper.assertTrue(!growth.isValidBonemealTarget(level, pos, state, false), id + " accepts bone meal");
                var before = level.getBlockState(pos);
                var bounds = new net.minecraft.world.phys.AABB(pos).inflate(5);
                int itemsBefore = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).size();
                growth.performBonemeal(level, level.random, pos, state);
                helper.assertTrue(level.getBlockState(pos).equals(before), "Direct growth changed the wild plant");
                helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).size() == itemsBefore,
                    "Direct growth spawned planting stock from " + id);
                for (var nearby : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
                    if (!nearby.equals(pos) && !nearby.equals(pos.above())) {
                        helper.assertTrue(!level.getBlockState(nearby).is(state.getBlock()), "Direct growth multiplied " + id);
                    }
                }
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void wildDiscoveryNeedsActualRealPlayerProduce(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var real = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "forage-player"));
        var fake = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "forage-machine"));
        var probe = new Probe();
        MinecraftForge.EVENT_BUS.register(probe);
        boolean oldDrops = level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
        try {
            for (var id : WildCropForage.PRODUCE.keySet()) {
                var state = plant(helper, pos, id);
                int before = probe.count;
                Block.getDrops(state, level, pos, null, real, ItemStack.EMPTY);
                helper.assertTrue(probe.count == before, "Preview credited a wild harvest");
                level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(false, level.getServer());
                Block.dropResources(state, level, pos, null, real, ItemStack.EMPTY);
                helper.assertTrue(probe.count == before, "Disabled drops credited a wild harvest");
                level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(true, level.getServer());
                Block.dropResources(state, level, pos, null, fake, ItemStack.EMPTY);
                helper.assertTrue(probe.count == before, "FakePlayer credited a wild harvest");
                // Repeated independent harvest/drop invocations accommodate native 20% edible rolls.
                for (int attempt = 0; attempt < 64 && probe.count == before; attempt++) {
                    int previous = probe.count;
                    Block.dropResources(state, level, pos, null, real, ItemStack.EMPTY);
                    helper.assertTrue(probe.count - previous <= 1, "One harvest emitted more than once");
                }
                helper.assertTrue(probe.count == before + 1, "Actual produce failed to credit " + id);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
            }
        } finally {
            level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(oldDrops, level.getServer());
            MinecraftForge.EVENT_BUS.unregister(probe);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void edibleAndShearedStockCannotUseBlockItemPlacement(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "forage-planter"));
        var items = new java.util.ArrayList<>(WildCropForage.PRODUCE.keySet());
        items.addAll(List.of("minecraft:carrot", "minecraft:potato", "farmersdelight:onion", "farmersdelight:rice"));
        for (var id : items) {
            var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));
            helper.assertTrue(stack.getItem() instanceof net.minecraft.world.item.BlockItem, "Expected native plantable item: " + id);
            level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 3);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos.below()),
                net.minecraft.core.Direction.UP, pos.below(), false);
            var context = new net.minecraft.world.item.context.BlockPlaceContext(
                new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
            var result = ((net.minecraft.world.item.BlockItem) stack.getItem()).place(context);
            helper.assertTrue(result == net.minecraft.world.InteractionResult.FAIL, "Food/transplant placement was not rejected: " + id);
            helper.assertTrue(level.getBlockState(pos).isAir(), "Food/transplant created a plant: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void eitherRiceHalfIsConsumedWithOneNativeDrop(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var bounds = new net.minecraft.world.phys.AABB(pos).inflate(3);
        var fake = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "rice-machine"));
        for (boolean upper : new boolean[] {false, true}) {
            plant(helper, pos, "farmersdelight:wild_rice");
            var previous = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds);
            helper.assertTrue(level.destroyBlock(upper ? pos.above() : pos, true, fake), "Rice break failed");
            int yield = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream()
                .filter(entity -> !previous.contains(entity))
                .mapToInt(entity -> {
                    helper.assertTrue(entity.getItem().is(ForgeRegistries.ITEMS.getValue(new ResourceLocation("farmersdelight:rice"))),
                        "Rice break spawned planting stock");
                    return entity.getItem().getCount();
                }).sum();
            helper.assertTrue(yield == 1, "Either rice half must yield exactly one rice, got " + yield);
            var rice = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("farmersdelight:wild_rice"));
            helper.assertTrue(!level.getBlockState(pos).is(rice) && !level.getBlockState(pos.above()).is(rice),
                "Rice harvest left a regrowing half");
        }
        helper.succeed();
    }
}
