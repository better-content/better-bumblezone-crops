package com.bettercontent.betterbumblezonecrops;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

/** Biome tags alone cannot exclude another dimension that reuses Overworld biomes. */
@Mod.EventBusSubscriber(modid = BumblezoneCultivars.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class OverworldForagePlacement extends PlacementFilter {
    private static final OverworldForagePlacement INSTANCE = new OverworldForagePlacement();
    public static final Codec<OverworldForagePlacement> CODEC = Codec.unit(() -> INSTANCE);
    public static final PlacementModifierType<OverworldForagePlacement> TYPE = () -> CODEC;

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.PLACEMENT_MODIFIER_TYPE,
            new ResourceLocation(BumblezoneCultivars.MOD_ID, "overworld_forage"), () -> TYPE);
    }

    @Override protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return context.getLevel().getLevel().dimension() == Level.OVERWORLD;
    }

    @Override public PlacementModifierType<?> type() { return TYPE; }
}
