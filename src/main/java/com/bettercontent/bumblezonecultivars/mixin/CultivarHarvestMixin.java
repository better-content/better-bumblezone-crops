package com.bettercontent.bumblezonecultivars.mixin;
import com.bettercontent.bumblezonecultivars.CultivarHarvestBoundary;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Block.class)
public abstract class CultivarHarvestMixin {
 @Inject(method="dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",at=@At("HEAD")) private static void beginSimple(BlockState state,Level world,BlockPos pos,CallbackInfo ci){CultivarHarvestBoundary.begin(state,world,pos);}
 @Inject(method="dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",at=@At("HEAD")) private static void beginEntity(BlockState state,LevelAccessor world,BlockPos pos,BlockEntity block,CallbackInfo ci){CultivarHarvestBoundary.begin(state,world,pos);}
 @Inject(method="dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Z)V",at=@At("HEAD"),remap=false) private static void beginFull(BlockState state,Level world,BlockPos pos,BlockEntity block,Entity actor,ItemStack tool,boolean experience,CallbackInfo ci){CultivarHarvestBoundary.begin(state,world,pos,actor);}
 @Inject(method={"dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V","dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"},at=@At("RETURN")) private static void end(CallbackInfo ci){CultivarHarvestBoundary.end();}
 @Inject(method="dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Z)V",at=@At("RETURN"),remap=false) private static void endFull(CallbackInfo ci){CultivarHarvestBoundary.end();}
 @Redirect(method="popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z")) private static boolean spawned(Level world,Entity entity){boolean added=world.addFreshEntity(entity);if(added)CultivarHarvestBoundary.spawned(world,entity);return added;}
}
