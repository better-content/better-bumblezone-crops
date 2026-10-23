package com.bettercontent.betterbumblezonecrops.nativefixture.mixin;

import com.bettercontent.betterbumblezonecrops.nativefixture.ArsDispatchProof;
import com.bettercontent.betterbumblezonecrops.nativefixture.OriginalParseObserver;
import com.mojang.serialization.*;
import net.minecraftforge.common.loot.LootModifierManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Observe the original root parse at its Forge call site, not an already-loaded DFU library class. */
@Mixin(value = LootModifierManager.class, remap = false)
public abstract class NativeArsRootDecodeMixin {
    @Redirect(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"),
        require = 1, expect = 1, allow = 1, remap = false)
    private <A, T> DataResult<A> bcOriginalRootParse(Codec<A> receiver, DynamicOps<T> ops, T input) {
        return OriginalParseObserver.parse(receiver, ops, input,
            result -> ArsDispatchProof.bodyParse(receiver, ops, input, result));
    }
}
