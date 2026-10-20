package com.bettercontent.betterbumblezonecrops;

import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Quantity accounting uses native recovered flower items, not halves or block-state maxima. */
public final class FlowerPolicy {
    private FlowerPolicy() {}

    public static boolean isFlowerItem(ItemStack stack) {
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return false;
        return CultivarCatalog.ALL.stream().anyMatch(c -> c.growthForm().equals("flower")
            && c.produce().contains(id.toString()));
    }

    public static boolean isSeed(ItemStack stack) {
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        var cultivar = id == null ? null : CultivarCatalog.bySeed(id.toString());
        return cultivar != null && cultivar.growthForm().equals("flower");
    }

    static int harvestSeeds(CultivarDefinition cultivar, List<ItemStack> loot, boolean inOrigin, RandomSource random) {
        if (!inOrigin) return 0;
        int units = 0;
        for (var stack : loot) {
            var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null && cultivar.produce().contains(id.toString())) units += stack.getCount();
        }
        int seeds = 0;
        for (int unit = 0; unit < units; unit++) {
            seeds += CultivarLootModifier.flowerSeedCount(true, random.nextFloat());
        }
        return seeds;
    }
}
