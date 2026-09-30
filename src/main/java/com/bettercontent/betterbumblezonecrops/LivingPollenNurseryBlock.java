package com.bettercontent.betterbumblezonecrops;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public final class LivingPollenNurseryBlock extends BaseEntityBlock {
    public LivingPollenNurseryBlock(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LivingPollenNurseryBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity raw = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        String seed = raw instanceof LivingPollenNurseryBlockEntity nursery ? nursery.seedId() : "minecraft:wheat_seeds";
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(seed));
        if(item==null)return List.of();
        var stack=new ItemStack(item,4);CultivarPlantings.source(stack,builder.getLevel().dimension().location().toString());return List.of(stack);
    }
    @Override public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
}
