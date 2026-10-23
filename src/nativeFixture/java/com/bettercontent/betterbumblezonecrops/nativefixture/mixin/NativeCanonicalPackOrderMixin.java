package com.bettercontent.betterbumblezonecrops.nativefixture.mixin;

import com.bettercontent.betterbumblezonecrops.nativefixture.CanonicalPackOrder;
import java.util.Collection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Required native-only admission hook. Original repository rebuilding and all original decoders run unchanged. */
@Mixin(value = PackRepository.class, remap = false)
public abstract class NativeCanonicalPackOrderMixin {
    @Shadow public abstract Pack getPack(String id);

    @ModifyVariable(method = "rebuildSelected", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private Collection<String> bc$requiredCanonicalPriority(Collection<String> requested) {
        Pack canonical = getPack(CanonicalPackOrder.PACK_ID);
        if (canonical == null) return requested;
        if (!canonical.isRequired() || !canonical.isFixedPosition() || canonical.getDefaultPosition() != Pack.Position.TOP)
            throw new IllegalStateException("Canonical native pack lost required/fixed/TOP admission");
        return CanonicalPackOrder.highestPriority(requested, true);
    }
}
