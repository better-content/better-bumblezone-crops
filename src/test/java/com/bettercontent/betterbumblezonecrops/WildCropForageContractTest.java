package com.bettercontent.betterbumblezonecrops;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class WildCropForageContractTest {
    @Test void allSevenWildCropsHaveOnlyTheirEdibleProduct() {
        assertEquals(7, WildCropForage.PRODUCE.size());
        WildCropForage.PRODUCE.forEach((plant, produce) -> {
            assertTrue(WildCropForage.isWild(plant));
            assertTrue(WildCropForage.allowsDrop(plant, produce));
            assertFalse(WildCropForage.allowsDrop(plant, plant), "Sheared wild plants are planting stock");
            assertFalse(WildCropForage.allowsDrop(plant, "minecraft:allium"));
            assertFalse(WildCropForage.allowsDrop(plant, "minecraft:beetroot_seeds"));
            assertFalse(WildCropForage.allowsDrop(plant, "farmersdelight:cabbage_seeds"));
            assertFalse(WildCropForage.allowsDrop(plant, "farmersdelight:tomato_seeds"));
        });
        assertFalse(WildCropForage.isWild("minecraft:carrots"));
        assertFalse(WildCropForage.isWild("farmersdelight:tomatoes"));
    }

    @Test void edibleItemsAndWildTransplantsCannotPlantButImportedSeedsCan() {
        assertTrue(WildCropForage.blocksPlanting("minecraft:carrot", "minecraft:carrots"));
        assertTrue(WildCropForage.blocksPlanting("minecraft:potato", "minecraft:potatoes"));
        assertTrue(WildCropForage.blocksPlanting("farmersdelight:onion", "farmersdelight:onions"));
        assertTrue(WildCropForage.blocksPlanting("farmersdelight:rice", "farmersdelight:rice"));
        WildCropForage.PRODUCE.keySet().forEach(plant -> assertTrue(WildCropForage.blocksPlanting(plant, plant)));
        assertFalse(WildCropForage.blocksPlanting("better_bumblezone_crops:minecraft_carrot_seeds", "minecraft:carrots"));
        assertFalse(WildCropForage.blocksPlanting("better_bumblezone_crops:farmersdelight_rice_seeds", "farmersdelight:rice"));
        assertFalse(WildCropForage.blocksPlanting("minecraft:wheat_seeds", "minecraft:wheat"));
        assertFalse(WildCropForage.blocksPlanting("minecraft:oak_sapling", "minecraft:oak_sapling"));
    }

    @Test void directNativeGrowthAndPlacementBoundariesAreInstalled() throws Exception {
        var resources = Files.readString(Path.of("src/main/resources/better_bumblezone_crops.mixins.json"));
        assertTrue(resources.contains("FiniteWildCropMixin"));
        assertTrue(resources.contains("ForagePlantingMixin"));
        var growth = Files.readString(Path.of("src/main/java/com/bettercontent/betterbumblezonecrops/mixin/FiniteWildCropMixin.java"));
        assertTrue(growth.contains("WildCropBlock"));
        assertTrue(growth.contains("WildRiceBlock"));
        assertTrue(growth.contains("m_214148_"), "Native SRG direct growth calls must be stopped");
        assertTrue(growth.contains("callback.cancel()"));
    }
}
