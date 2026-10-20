package com.bettercontent.betterbumblezonecrops;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Set;

/** Finite wild food is separate from the imported, lineage-bearing cultivar system. */
@Mod.EventBusSubscriber(modid = BumblezoneCultivars.MOD_ID)
public final class WildCropForage {
    private WildCropForage() {}

    public static final Map<String, String> PRODUCE = Map.of(
        "farmersdelight:wild_carrots", "minecraft:carrot",
        "farmersdelight:wild_potatoes", "minecraft:potato",
        "farmersdelight:wild_beetroots", "minecraft:beetroot",
        "farmersdelight:wild_cabbages", "farmersdelight:cabbage",
        "farmersdelight:wild_onions", "farmersdelight:onion",
        "farmersdelight:wild_tomatoes", "farmersdelight:tomato",
        "farmersdelight:wild_rice", "farmersdelight:rice");

    private static final Set<String> EDIBLE_PLANTABLES = Set.of(
        "minecraft:carrot", "minecraft:potato", "farmersdelight:onion", "farmersdelight:rice");

    public static boolean isWild(String blockId) { return PRODUCE.containsKey(blockId); }

    /** The wild loot boundary is an allowlist: no sheared plants, seeds or companion flowers. */
    public static boolean allowsDrop(String blockId, String itemId) {
        return itemId.equals(PRODUCE.get(blockId));
    }

    public static boolean blocksPlanting(String itemId, String blockId) {
        if (isWild(blockId)) return true;
        if (!EDIBLE_PLANTABLES.contains(itemId)) return false;
        var cultivar = CultivarCatalog.byPlant(blockId);
        return cultivar != null && cultivar.produce().contains(itemId)
            && !cultivar.seedItem().equals(itemId);
    }

    /** Also covers BlockItem placement contexts supplied by machines/FakePlayers. */
    public static boolean blocksPlanting(ItemStack stack, Block block) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        return itemId != null && blockId != null && blocksPlanting(itemId.toString(), blockId.toString());
    }

    /** Forge posts this for both normal bone meal and dispenser bone meal. */
    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(event.getBlock().getBlock());
        if (id != null && isWild(id.toString())) event.setCanceled(true);
    }
}
