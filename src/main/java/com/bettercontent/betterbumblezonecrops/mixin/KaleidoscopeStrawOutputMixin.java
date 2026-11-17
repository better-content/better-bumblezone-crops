package com.bettercontent.betterbumblezonecrops.mixin;

import com.bettercontent.betterbumblezonecrops.NativeCultivarAliases;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** KC StrawBlocks.fallOn second popResource is EXACTLY the four native rice seeds;
 * first popResource still emits its five unchanged panicles. */
@Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.block.misc.StrawBlocks", remap = false)
public abstract class KaleidoscopeStrawOutputMixin {
    @Redirect(method = {"fallOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;F)V",
            "m_142072_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;F)V"},
            at = @At(value = "INVOKE", target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/block/misc/StrawBlocks;m_49840_(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V", ordinal = 1, remap = false), require = 1)
    private void bcc$nativeRiceSeeds(Level level, BlockPos pos, ItemStack nativeOutput,
                                      Level world, BlockState state, BlockPos at, Entity falling, float distance) {
        Block.popResource(level, pos, NativeCultivarAliases.newOutput(level, nativeOutput));
    }
}
