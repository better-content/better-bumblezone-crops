package com.bettercontent.betterbumblezonecrops;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Stable, bounded Rootmin behavior and appearance variation for a world site. */
public final class RootminEcologyPolicy {
    public static final int SWIFT = 0;
    public static final int STEADY = 1;
    private static final long SALT = 0x524F4F544D494E31L; // "ROOTMIN1"
    private static final double SPEED_DELTA = 0.08D;

    private RootminEcologyPolicy() {}

    public static int variant(long worldSeed, ResourceLocation dimension, BlockPos spawnPos) {
        long value = mix64(worldSeed ^ SALT);
        value = mix64(value ^ dimension.toString().hashCode());
        value = mix64(value ^ spawnPos.asLong());
        return (int) Math.floorMod(value, 2L);
    }

    public static double movementSpeedModifier(int variant) {
        return switch (variant) {
            case SWIFT -> SPEED_DELTA;
            case STEADY -> -SPEED_DELTA;
            default -> throw new IllegalArgumentException("unknown Rootmin variant: " + variant);
        };
    }

    public static ResourceLocation flower(int variant) {
        return switch (variant) {
            case SWIFT -> new ResourceLocation("minecraft", "poppy");
            case STEADY -> new ResourceLocation("minecraft", "blue_orchid");
            default -> throw new IllegalArgumentException("unknown Rootmin variant: " + variant);
        };
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
