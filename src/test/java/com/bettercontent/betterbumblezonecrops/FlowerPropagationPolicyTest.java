package com.bettercontent.betterbumblezonecrops;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlowerPropagationPolicyTest {
    @Test void noOffOriginRollCanProduceASeed() {
        for (int roll = 0; roll <= 1000; roll++) {
            assertEquals(0, CultivarLootModifier.flowerSeedCount(false, roll / 1000.0F));
        }
    }

    @Test void originKeepsOneOrTwoSeedsAndTheExactBoundary() {
        assertEquals(2, CultivarLootModifier.flowerSeedCount(true, 0));
        assertEquals(2, CultivarLootModifier.flowerSeedCount(true, Math.nextDown(0.5F)));
        assertEquals(1, CultivarLootModifier.flowerSeedCount(true, 0.5F));
        assertEquals(1, CultivarLootModifier.flowerSeedCount(true, Math.nextDown(1.0F)));
    }

    @Test void decorativeCoverageDoesNotReclassifyFoodCrops() {
        for (String plant : new String[]{"minecraft:dandelion", "minecraft:sunflower", "minecraft:pink_petals",
                "projectvibrantjourneys:pink_lotus", "upgrade_aquatic:flowering_rush",
                "hexerei:flowering_lily_pad", "the_deep_void:pus_flower"}) {
            assertEquals("flower", CultivarCatalog.byPlant(plant).growthForm(), plant);
        }
        assertEquals("annual", CultivarCatalog.byPlant("minecraft:carrots").growthForm());
        assertEquals("minecraft:pink_petals", CultivarCatalog.byPlant("quark:water_pink_petals").produce().get(0));
    }
}
