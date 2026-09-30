package com.bettercontent.betterbumblezonecrops;

import org.junit.jupiter.api.Test;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import static org.junit.jupiter.api.Assertions.*;

class CultivarPropagationPolicyTest {
    @Test void kelpMakesOnePropagationDecisionAtItsHeadOnly() {
        assertTrue(CultivarLootModifier.isKelpHead("minecraft:kelp"));
        assertFalse(CultivarLootModifier.isKelpHead("minecraft:kelp_plant"));
    }

    @Test void ordinaryGourdStemsNeedAgeWhileAttachedStemsAreMature() {
        assertTrue(CultivarLootModifier.isOrdinaryGourdStem("minecraft:melon_stem"));
        assertFalse(CultivarLootModifier.isAttachedGourdStem("minecraft:melon_stem"));
        assertTrue(CultivarLootModifier.isAttachedGourdStem("minecraft:attached_melon_stem"));
        assertFalse(CultivarLootModifier.isOrdinaryGourdStem("minecraft:attached_melon_stem"));
        assertFalse(CultivarLootModifier.isAtMaximumAge(6, 7));
        assertTrue(CultivarLootModifier.isAtMaximumAge(7, 7));
    }

    @Test void nurseryChoiceIsStablePerSeedDimensionAndSite() {
        ResourceLocation bumblezone = new ResourceLocation("the_bumblezone", "the_bumblezone");
        BlockPos first = new BlockPos(13, 78, -21);
        int selected = CultivarChunkFinalizer.siteSelectionIndex(913579L, bumblezone, first, 1_000_003);
        assertEquals(selected, CultivarChunkFinalizer.siteSelectionIndex(913579L, bumblezone, first, 1_000_003));
        assertNotEquals(selected, CultivarChunkFinalizer.siteSelectionIndex(913580L, bumblezone, first, 1_000_003));
        assertNotEquals(selected, CultivarChunkFinalizer.siteSelectionIndex(913579L, new ResourceLocation("minecraft", "overworld"), first, 1_000_003));
    }

    @Test void firethornEcologySitesAreSparseAndReproducible() {
        ResourceLocation bumblezone = new ResourceLocation("the_bumblezone", "the_bumblezone");
        BlockPos site = new BlockPos(13, 78, -21);
        boolean selected = FirethornEcologyPolicy.shouldPlaceAtSite(913579L, bumblezone, site);
        assertEquals(selected, FirethornEcologyPolicy.shouldPlaceAtSite(913579L, bumblezone, site));

        int selectedSites = 0;
        int otherWorldSites = 0;
        for (int x = 0; x < 256; x++) {
            if (FirethornEcologyPolicy.shouldPlaceAtSite(913579L, bumblezone, new BlockPos(x, 78, -21))) selectedSites++;
            if (FirethornEcologyPolicy.shouldPlaceAtSite(913580L, bumblezone, new BlockPos(x, 78, -21))) otherWorldSites++;
        }
        assertTrue(selectedSites > 8 && selectedSites < 40, "sparse deterministic policy selected " + selectedSites + " of 256 sites");
        assertTrue(otherWorldSites > 8 && otherWorldSites < 40, "sparse deterministic policy selected " + otherWorldSites + " of 256 sites in the other world");
        assertNotEquals(selectedSites, otherWorldSites, "world seed should change the stable site pattern");
        assertTrue(FirethornEcologyPolicy.cardinalStart(site) >= 0 && FirethornEcologyPolicy.cardinalStart(site) < 4);
        assertTrue(FirethornEcologyPolicy.mayPlace(0, true));
        assertFalse(FirethornEcologyPolicy.mayPlace(0, false), "Goety is optional");
        assertFalse(FirethornEcologyPolicy.mayPlace(1, true), "successful hostile planting is capped per chunk");
    }
    @Test void immaturePlantsDoNotPropagateOutsideTheirOrigin() {
        assertEquals(0, CultivarLootModifier.seedCount(false, false, false, true, 0));
        assertEquals(1, CultivarLootModifier.seedCount(false, true, false, false, 2));
    }

    @Test void matureOriginPlantsKeepTheTwoToFourSeedRange() {
        assertEquals(2, CultivarLootModifier.seedCount(true, true, false, false, 0));
        assertEquals(4, CultivarLootModifier.seedCount(true, true, false, false, 2));
        assertEquals(2, CultivarLootModifier.seedCount(true, true, true, false, 0));
    }

    @Test void matureForeignPlantsReturnExactlyOneSeedOnlyWithSourceProvenance() {
        assertEquals(0, CultivarLootModifier.seedCount(true, false, false, false, 0));
        assertEquals(1, CultivarLootModifier.seedCount(true, false, false, true, 0));
    }

    @Test void persistentHarvestPlantsNeverCreateSeedsOutsideTheirOrigin() {
        assertEquals(0, CultivarLootModifier.seedCount(false, false, true, true, 0));
        assertEquals(0, CultivarLootModifier.seedCount(true, false, true, true, 0));
    }
}
