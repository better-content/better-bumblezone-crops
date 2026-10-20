package com.bettercontent.betterbumblezonecrops.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lotus stems can regenerate harvested flower heads without a bonemeal event. */
@Pseudo
@Mixin(targets = "net.hibiscus.naturespirit.blocks.LotusStemBlock", remap = false)
public abstract class LotusFlowerGrowthMixin {
    @Inject(method = "grow", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void bc$originGrowth(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!level.dimension().location().equals(new ResourceLocation("the_bumblezone", "the_bumblezone"))) ci.cancel();
    }

    @Inject(method = {"m_213898_", "randomTick"}, at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void bc$originRegrowth(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        // Cancel the flowering operation as a unit: letting it replace the old flower with
        // stem while suppressing only the new head would destroy an existing flower.
        if (!level.dimension().location().equals(new ResourceLocation("the_bumblezone", "the_bumblezone"))) ci.cancel();
    }
}
