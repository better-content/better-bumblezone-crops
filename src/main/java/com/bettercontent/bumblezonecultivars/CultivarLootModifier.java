package com.bettercontent.bumblezonecultivars;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
        int count = !mature
                ? seedCount(false, inOrigin, persistentHarvest, 0, 1.0F)
                : inOrigin
                        ? seedCount(true, true, persistentHarvest, context.getRandom().nextInt(3), 1.0F)
                        : seedCount(true, false, persistentHarvest, 0, context.getRandom().nextFloat());
        if (count > 0) {var stack=new ItemStack(seed,count);CultivarPlantings.source(stack,dimension.toString());loot.add(stack);}
        return loot;
    }

    static int seedCount(boolean mature, boolean inOrigin, boolean persistentHarvest, int originBonus, float offOriginRoll) {
        if (!inOrigin && persistentHarvest) return 0;
        if (!mature) return 1;
        if (inOrigin) return 2 + originBonus;
        return offOriginRoll < 0.10F ? 2 : 1;
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
