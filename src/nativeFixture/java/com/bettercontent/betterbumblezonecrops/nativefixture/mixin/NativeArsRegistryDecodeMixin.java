package com.bettercontent.betterbumblezonecrops.nativefixture.mixin;

import com.bettercontent.betterbumblezonecrops.nativefixture.ArsDispatchProof;
import com.mojang.serialization.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Private implementation name, PUBLIC original Codec.decode only; no private members accessed. */
@Mixin(targets = "net.minecraftforge.registries.ForgeRegistry$RegistryCodec", remap = false)
public abstract class NativeArsRegistryDecodeMixin {
    @Inject(method = "decode(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;",
        at = @At("RETURN"), require = 2, expect = 2, allow = 2, remap = false)
    void bcRegistered(DynamicOps<?> ops, Object input, CallbackInfoReturnable<DataResult<?>> callback) {
        if (!ArsDispatchProof.active()) return;
        ArsDispatchProof.registered((Object)this, ops, input, callback.getReturnValue());
    }
}
