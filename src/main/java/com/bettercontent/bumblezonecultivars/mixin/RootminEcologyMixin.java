package com.bettercontent.bumblezonecultivars.mixin;

import com.bettercontent.bumblezonecultivars.internal.RootminEcologyAccess;
import net.minecraft.world.level.block.state.BlockState;
import com.telepathicgrunt.the_bumblezone.entities.mobs.RootminEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Exposes only Rootmin's synchronized flower setter to the ecology handler. */
@Mixin(RootminEntity.class)
public abstract class RootminEcologyMixin implements RootminEcologyAccess {
    @Shadow(remap = false) public abstract void setFlowerBlock(BlockState state);

    @Override
    public void bc$setFlowerBlock(BlockState state) {
        setFlowerBlock(state);
    }
}
