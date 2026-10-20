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

/** Explicit native overrides in pinned optional mods; no global placement interception. */
@Pseudo
@Mixin(targets = {"net.hibiscus.naturespirit.blocks.LotusFlowerBlock",
    "net.hibiscus.naturespirit.blocks.WaterFlowerbedBlock",
    "net.mcreator.thedeepvoid.block.VoidBlossomBlock",
    "com.Polarice3.Goety.common.blocks.CorpseBlossomBlock"}, remap = false)
public abstract class ModFlowerBonemealMixin {
    @Inject(method = {"m_214148_", "performBonemeal"}, at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void bc$onlyMultiplyAtOrigin(ServerLevel level, RandomSource random, BlockPos pos,
                                         BlockState state, CallbackInfo ci) {
        if (!level.dimension().location().equals(new ResourceLocation("the_bumblezone", "the_bumblezone"))) ci.cancel();
    }
}
