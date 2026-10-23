package com.bettercontent.betterbumblezonecrops.nativefixture.mixin;

import com.bettercontent.betterbumblezonecrops.nativefixture.CanonicalDataFixture;
import com.bettercontent.betterbumblezonecrops.nativefixture.ArsDispatchProof;
import com.google.common.collect.ImmutableMap;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import java.util.List;
import java.util.Iterator;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.loot.LootModifierManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

/** Required nonshipping data proof. Original apply executes once; only its successful RETURN is observed. */
@Mixin(value = LootModifierManager.class, remap = false)
public abstract class NativeLootLoadProofMixin {
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At("HEAD"), require = 1, expect = 1, remap = false)
    void bcBegin(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler, CallbackInfo callback) {
        ArsDispatchProof.begin();
    }
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;", shift = At.Shift.BEFORE),
        locals = LocalCapture.CAPTURE_FAILHARD, require = 1, expect = 1, remap = false)
    void bcBefore(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler, CallbackInfo callback,
            ImmutableMap.Builder<ResourceLocation, IGlobalLootModifier> builder, List<ResourceLocation> ids,
            ResourceLocation indexId, Iterator<ResourceLocation> iterator, ResourceLocation row, JsonElement definition) {
        ArsDispatchProof.before(resources, row, definition);
    }
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;", shift = At.Shift.AFTER),
        locals = LocalCapture.CAPTURE_FAILHARD, require = 1, expect = 1, remap = false)
    void bcAfter(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler, CallbackInfo callback,
            ImmutableMap.Builder<ResourceLocation, IGlobalLootModifier> builder, List<ResourceLocation> ids,
            ResourceLocation indexId, Iterator<ResourceLocation> iterator, ResourceLocation row, JsonElement definition) {
        ArsDispatchProof.after(row);
    }
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At("RETURN"), require = 1, expect = 1, remap = false)
    private void bcCanonicalDataLoaded(Map<ResourceLocation, JsonElement> json, ResourceManager resources,
            ProfilerFiller profiler, CallbackInfo callback) {
        CanonicalDataFixture.atNativeApplyReturn((LootModifierManager)(Object)this, resources);
    }
}
