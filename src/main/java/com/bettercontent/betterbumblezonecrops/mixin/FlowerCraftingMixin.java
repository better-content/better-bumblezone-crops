package com.bettercontent.betterbumblezonecrops.mixin;

import com.bettercontent.betterbumblezonecrops.FlowerPolicy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Dimension-gate flower creation at recipe matching, before ingredients can be consumed. */
@Mixin({ShapedRecipe.class, ShapelessRecipe.class})
public abstract class FlowerCraftingMixin {
    @Inject(method = "matches(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/level/Level;)Z",
        at = @At("RETURN"), cancellable = true, require = 1)
    private void bc$noOffOriginFlowerCreation(CraftingContainer grid, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || level.dimension().location().equals(new ResourceLocation("the_bumblezone", "the_bumblezone"))) return;
        var output = ((CraftingRecipe) this).getResultItem(level.registryAccess());
        if (!FlowerPolicy.isFlowerItem(output)) return;
        int flowerInputs = 0;
        // Vanilla shaped/shapeless recipes consume one item from each occupied ingredient slot.
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            if (FlowerPolicy.isFlowerItem(grid.getItem(slot))) flowerInputs++;
        }
        if (output.getCount() > flowerInputs) cir.setReturnValue(false);
    }
}
