package com.bettercontent.betterbumblezonecrops.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Block native multiplication even when a growth machine calls BonemealableBlock directly.
 * Official and SRG names are explicit because Farmer's Delight is an optional string target;
 * both signatures were inspected in the pinned 1.3.1 artifact. No FD compile dependency needed.
 */
@Pseudo
@Mixin(targets = {
    "vectorwing.farmersdelight.common.block.WildCropBlock",
    "vectorwing.farmersdelight.common.block.WildRiceBlock"
}, remap = false)
public abstract class FiniteWildCropMixin {
    @Inject(method = {"isValidBonemealTarget", "m_7370_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void noMultiplicationTarget(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }

    @Inject(method = {"performBonemeal", "m_214148_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void noDirectMultiplication(CallbackInfo callback) {
        callback.cancel();
    }
}
