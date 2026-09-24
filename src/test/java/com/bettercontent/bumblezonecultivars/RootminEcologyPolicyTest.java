package com.bettercontent.bumblezonecultivars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RootminEcologyPolicyTest {
    private static final ResourceLocation BUMBLEZONE = new ResourceLocation("the_bumblezone", "the_bumblezone");

    @Test
    void sameWorldDimensionAndSpawnSiteAlwaysRecreateTheSameVariant() {
        BlockPos site = new BlockPos(41, 72, -19);
        int first = RootminEcologyPolicy.variant(123456789L, BUMBLEZONE, site);

        assertEquals(first, RootminEcologyPolicy.variant(123456789L, BUMBLEZONE, site));
        boolean dimensionChangesObservedIdentity = false;
        for (int x = -32; x <= 32; x++) {
            BlockPos sample = new BlockPos(x, 72, x * 3);
            dimensionChangesObservedIdentity |= RootminEcologyPolicy.variant(123456789L, BUMBLEZONE, sample)
                    != RootminEcologyPolicy.variant(123456789L,
                    new ResourceLocation("minecraft", "overworld"), sample);
        }
        assertTrue(dimensionChangesObservedIdentity);
        assertTrue(first == RootminEcologyPolicy.SWIFT || first == RootminEcologyPolicy.STEADY);
    }

    @Test
    void sitesReceiveBothBoundedBehaviorAndAppearanceProfiles() {
        boolean swift = false;
        boolean steady = false;
        for (int x = -64; x <= 64; x++) {
            int variant = RootminEcologyPolicy.variant(991L, BUMBLEZONE, new BlockPos(x, 70, x * 3));
            swift |= variant == RootminEcologyPolicy.SWIFT;
            steady |= variant == RootminEcologyPolicy.STEADY;
            assertTrue(Math.abs(RootminEcologyPolicy.movementSpeedModifier(variant)) <= 0.08D);
            assertEquals(variant == RootminEcologyPolicy.SWIFT ? 0.08D : -0.08D,
                    RootminEcologyPolicy.movementSpeedModifier(variant));
        }

        assertTrue(swift && steady);
        assertEquals(new ResourceLocation("minecraft", "poppy"),
                RootminEcologyPolicy.flower(RootminEcologyPolicy.SWIFT));
        assertEquals(new ResourceLocation("minecraft", "blue_orchid"),
                RootminEcologyPolicy.flower(RootminEcologyPolicy.STEADY));
        assertThrows(IllegalArgumentException.class, () -> RootminEcologyPolicy.flower(2));
        assertThrows(IllegalArgumentException.class, () -> RootminEcologyPolicy.movementSpeedModifier(-1));
    }
}
