package com.bettercontent.betterbumblezonecrops.mixin;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Filter only the flower branch of bonemeal; native grass growth and worldgen stay untouched. */
@Mixin(GrassBlock.class)
public abstract class FlowerGrassBonemealMixin {
    @Redirect(method = "performBonemeal", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/biome/BiomeGenerationSettings;getFlowerFeatures()Ljava/util/List;"), require = 1)
    private List<ConfiguredFeature<?, ?>> bc$flowersOnlyAtOrigin(BiomeGenerationSettings settings,
            ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        return level.dimension().location().equals(new ResourceLocation("the_bumblezone", "the_bumblezone"))
            ? settings.getFlowerFeatures() : List.of();
    }
}
