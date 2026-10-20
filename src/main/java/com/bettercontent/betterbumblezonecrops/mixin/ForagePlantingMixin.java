package com.bettercontent.betterbumblezonecrops.mixin;

import com.bettercontent.betterbumblezonecrops.WildCropForage;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.block.Block;

/** Reject edible crop planting at the item operation, not only a real player's interaction. */
@Mixin(BlockItem.class)
public abstract class ForagePlantingMixin {
    @Shadow public abstract Block getBlock();

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void rejectForagePlanting(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> callback) {
        if (WildCropForage.blocksPlanting(context.getItemInHand(), getBlock())) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }
}
