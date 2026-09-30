package com.bettercontent.betterbumblezonecrops;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;

public final class CultivarSeedItem extends Item {
    private final ResourceLocation plant;
    private final boolean flower;
    public CultivarSeedItem(String plant) { this(plant, false); }
    public CultivarSeedItem(String plant, boolean flower) {
        super(new Properties()); this.plant = new ResourceLocation(plant); this.flower = flower;
    }

    @Override public Component getName(net.minecraft.world.item.ItemStack stack) {
        if (!flower) return super.getName(stack);
        Block block = ForgeRegistries.BLOCKS.getValue(plant);
        return Component.translatable("item.better_bumblezone_crops.flower_seeds",
            block == null ? Component.literal(plant.toString()) : block.getName());
    }

    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, Level level,
                                          List<Component> tooltip, TooltipFlag flag) {
        String key = FirethornCuePolicy.tooltipKey(plant.toString());
        if (key != null) tooltip.add(Component.translatable(key));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Block block = ForgeRegistries.BLOCKS.getValue(plant);
        if (block == null) return InteractionResult.FAIL;
        var pos = context.getClickedPos().relative(context.getClickedFace());
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockState candidate = state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
                if (candidate.canSurvive(context.getLevel(), pos)) {
                    state = candidate;
                    break;
                }
            }
        }
        boolean tall = state.hasProperty(DoublePlantBlock.HALF);
        if (!context.getLevel().getBlockState(pos).canBeReplaced()
                || (tall && !context.getLevel().getBlockState(pos.above()).canBeReplaced())
                || !state.canSurvive(context.getLevel(), pos)) return InteractionResult.FAIL;
        if (!context.getLevel().isClientSide()) {
            if (tall) DoublePlantBlock.placeAt(context.getLevel(), state, pos, 3);
            else if(!context.getLevel().setBlock(pos, state, 3))return InteractionResult.FAIL;
            if(context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player)CultivarPlantings.planted(player,pos,state,context.getItemInHand());
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }
}
