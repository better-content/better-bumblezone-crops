package com.bettercontent.betterbumblezonecrops.mixin;

import com.bettercontent.betterbumblezonecrops.CultivarCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Ars ritual-generated flower placements, not player flower relocation or worldgen. */
@Pseudo
@Mixin(targets = "com.hollingsworth.arsnouveau.api.ritual.features.PlaceBlockFeature", remap = false)
public abstract class ArsFlowerPlacementMixin {
    @Redirect(method = "onPlace", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/BlockItem;place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
        remap = true), require = 1, remap = false)
    private InteractionResult bc$filterGeneratedFlowers(BlockItem item, BlockPlaceContext context) {
        var id = ForgeRegistries.BLOCKS.getKey(item.getBlock());
        var cultivar = id == null ? null : CultivarCatalog.byPlant(id.toString());
        if (cultivar != null && cultivar.growthForm().equals("flower")
                && !context.getLevel().dimension().location().equals(new ResourceLocation("the_bumblezone", "the_bumblezone"))) {
            return InteractionResult.FAIL;
        }
        return item.place(context);
    }
}
