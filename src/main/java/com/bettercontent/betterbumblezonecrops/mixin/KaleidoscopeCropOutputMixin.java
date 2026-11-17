package com.bettercontent.betterbumblezonecrops.mixin;

import com.bettercontent.betterbumblezonecrops.NativeCultivarAliases;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Exact KC BaseCropBlock.use popResource callback, AFTER native output selection. */
@Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.block.crop.BaseCropBlock", remap = false)
public abstract class KaleidoscopeCropOutputMixin {
    @Redirect(method = {"use(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
            "m_6227_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V", remap = true), require = 1)
    private void bcc$nativeActualOutput(Level level, BlockPos pos, ItemStack nativeOutput,
                                         BlockState state, Level world, BlockPos at, Player actor,
                                         InteractionHand hand, BlockHitResult hit) {
        Block.popResource(level, pos, NativeCultivarAliases.newOutput(level, nativeOutput));
    }
}
