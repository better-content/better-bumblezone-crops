package com.bettercontent.betterbumblezonecrops;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public final class CultivarLootModifier extends LootModifier {
    public static final Codec<CultivarLootModifier> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance).apply(instance, CultivarLootModifier::new));
    public CultivarLootModifier(LootItemCondition[] conditions) { super(conditions); }

    @Override protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state != null && state.hasProperty(DoublePlantBlock.HALF)
                && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) return loot;
        ResourceLocation dimension = context.getLevel().dimension().location();
        ResourceLocation plantId = state == null ? null : ForgeRegistries.BLOCKS.getKey(state.getBlock());
        CultivarDefinition cultivar = plantId == null ? null : CultivarCatalog.byPlant(plantId.toString());
        if (cultivar == null) {
            loot.removeIf(stack -> {
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
                CultivarDefinition seed = itemId == null ? null : CultivarCatalog.bySeed(itemId.toString());
                return seed != null && !seed.originDimensions().contains(dimension.toString());
            });
            return loot;
        }
        var seed = ForgeRegistries.ITEMS.getValue(new ResourceLocation(cultivar.seedItem()));
        if (seed == null) return loot;
        loot.removeIf(stack -> stack.is(seed));
        boolean mature = isMature(state, cultivar.maturityRule());
        // A kelp column has one propagating head.  Its body segments can be removed in either
        // direction by players or automation, so treating them as immature crops would create a
        // propagation decision for every segment instead of one for the plant.
        if (cultivar.maturityRule().equals("top-segment") && !mature) return loot;
        boolean inOrigin = cultivar.originDimensions().contains(dimension.toString());
        boolean persistentHarvest = cultivar.growthForm().equals("persistent-harvest");
        BlockPos origin = context.getParamOrNull(LootContextParams.ORIGIN) == null ? null
                : BlockPos.containing(context.getParamOrNull(LootContextParams.ORIGIN));
        boolean sourcedPlanting = !inOrigin && origin != null
                && CultivarPlantings.lookup(context.getLevel(), origin, state) != null;
        int count = cultivar.growthForm().equals("flower")
                ? flowerSeedCount(inOrigin, context.getRandom().nextFloat())
                : !mature
                ? seedCount(false, inOrigin, persistentHarvest, sourcedPlanting, 0)
                : inOrigin
                        ? seedCount(true, true, persistentHarvest, false, context.getRandom().nextInt(3))
                        : seedCount(true, false, persistentHarvest, sourcedPlanting, 0);
        if (count > 0) {
            var stack = new ItemStack(seed, count);
            if (inOrigin || sourcedPlanting) CultivarPlantings.source(stack, "the_bumblezone:the_bumblezone");
            loot.add(stack);
        }
        return loot;
    }

    /** Instant-mature flowers need subcritical off-origin propagation to avoid harvest loops. */
    static int flowerSeedCount(boolean inOrigin, float roll) {
        return inOrigin ? (roll < 0.5F ? 2 : 1) : (roll < 0.2F ? 1 : 0);
    }

    static int seedCount(boolean mature, boolean inOrigin, boolean persistentHarvest, boolean sourcedPlanting, int originBonus) {
        if (!inOrigin && persistentHarvest) return 0;
        if (!mature) return inOrigin ? 1 : 0;
        if (inOrigin) return 2 + originBonus;
        return sourcedPlanting ? 1 : 0;
    }

    static boolean isMature(BlockState state, String rule) {
        if (rule.equals("always-mature")) return true;
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        String blockName = blockId == null ? "" : blockId.toString();
        if (rule.equals("top-segment")) return isKelpHead(blockName);
        if (rule.equals("fruit-block")) {
            // Attached melon and pumpkin stems have already produced fruit.  An ordinary stem
            // only earns the mature propagation range at its own maximum age.
            if (isAttachedGourdStem(blockName)) return true;
            if (isOrdinaryGourdStem(blockName)) return maximumAge(state);
            return true;
        }
        return maximumAgeOrRuleFallback(state, rule);
    }

    private static boolean maximumAgeOrRuleFallback(BlockState state, String rule) {
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty integer && property.getName().equals("age")) {
                return isAtMaximumAge(state.getValue(integer), integer.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0));
            }
            if (property instanceof BooleanProperty booleanProperty && property.getName().equals("berries")) {
                return state.getValue(booleanProperty);
            }
        }
        return !rule.contains("immature");
    }

    private static boolean maximumAge(BlockState state) {
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty integer && property.getName().equals("age")) {
                return isAtMaximumAge(state.getValue(integer), integer.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0));
            }
        }
        return false;
    }

    static boolean isKelpHead(String blockId) { return blockId.equals("minecraft:kelp"); }
    static boolean isAttachedGourdStem(String blockId) {
        return blockId.equals("minecraft:attached_melon_stem") || blockId.equals("minecraft:attached_pumpkin_stem");
    }
    static boolean isOrdinaryGourdStem(String blockId) {
        return blockId.equals("minecraft:melon_stem") || blockId.equals("minecraft:pumpkin_stem");
    }
    static boolean isAtMaximumAge(int age, int maximumAge) { return age >= maximumAge; }

    @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
