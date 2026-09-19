package com.bettercontent.bumblezonecultivars;

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
    @Test void immaturePlantsYieldOneSeedInEveryDimension() {
        assertEquals(1, CultivarLootModifier.seedCount(false, false, false, 0, 0.0F));
        assertEquals(1, CultivarLootModifier.seedCount(false, true, false, 2, 0.0F));
    }

    @Test void matureOriginPlantsKeepTheTwoToFourSeedRange() {
        assertEquals(2, CultivarLootModifier.seedCount(true, true, false, 0, 1.0F));
        assertEquals(4, CultivarLootModifier.seedCount(true, true, false, 2, 1.0F));
        assertEquals(2, CultivarLootModifier.seedCount(true, true, true, 0, 1.0F));
    }

    @Test void matureForeignPlantsUseTenPercentSecondSeedBoundary() {
        assertEquals(2, CultivarLootModifier.seedCount(true, false, false, 0, 0.099999F));
        assertEquals(1, CultivarLootModifier.seedCount(true, false, false, 0, 0.10F));
        assertEquals(1, CultivarLootModifier.seedCount(true, false, false, 0, 0.999999F));
    }

    @Test void persistentHarvestPlantsNeverCreateSeedsOutsideTheirOrigin() {
        assertEquals(0, CultivarLootModifier.seedCount(false, false, true, 0, 0.0F));
        assertEquals(0, CultivarLootModifier.seedCount(true, false, true, 0, 0.0F));
    }
}
