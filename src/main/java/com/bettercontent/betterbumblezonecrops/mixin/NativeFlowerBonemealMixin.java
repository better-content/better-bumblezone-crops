package com.bettercontent.betterbumblezonecrops.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Guard native direct calls too, including magic/automation which bypasses Forge bonemeal events. */
@Mixin({TallFlowerBlock.class, PinkPetalsBlock.class})
public abstract class NativeFlowerBonemealMixin {
    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true, require = 1)
    private void bc$onlyMultiplyAtOrigin(ServerLevel level, RandomSource random, BlockPos pos,
                                         BlockState state, CallbackInfo ci) {
        if (!level.dimension().location().equals(new ResourceLocation("the_bumblezone", "the_bumblezone"))) ci.cancel();
    }
}
