package com.bettercontent.betterbumblezonecrops.nativefixture.mixin;

import com.bettercontent.betterbumblezonecrops.nativefixture.ArsDispatchProof;
import com.mojang.serialization.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The ORIGINAL DIRECT_CODEC wrapper, not its lazy registry KEY decoder. */
@Mixin(value = MapCodec.MapCodecCodec.class, remap = false)
public abstract class NativeArsRootDecodeMixin {
    @Inject(method = "decode(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;",
        at = @At("RETURN"), require = 1, expect = 1, allow = 1, remap = false)
    void bcBody(DynamicOps<?> ops, Object input, CallbackInfoReturnable<DataResult<?>> callback) {
        if (!ArsDispatchProof.active()) return;
        ArsDispatchProof.body((Object)this, ops, input, callback.getReturnValue());
    }
}
