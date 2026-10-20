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

    @Override public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(
            Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        var cultivar = CultivarCatalog.byPlant(plant.toString());
        if (!flower || cultivar == null || !cultivar.placementAdapter().startsWith("water-")) {
            return super.use(level, player, hand);
        }
        var hit = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK
                || !level.mayInteract(player, hit.getBlockPos())
                || !player.mayUseItemAt(hit.getBlockPos().above(), hit.getDirection(), player.getItemInHand(hand))) {
            return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        // Match PlaceOnWaterBlockItem's native surface hit, without swapping the player's held stack.
        var result = useOn(new UseOnContext(player, hand, hit.withPosition(hit.getBlockPos().above())));
        return new net.minecraft.world.InteractionResultHolder<>(result, player.getItemInHand(hand));
    }

    private InteractionResult plantFlower(UseOnContext context, Block block) {
        var nativeItem = block.asItem();
        var cultivar = CultivarCatalog.byPlant(plant.toString());
        // Quark's water petals have no BlockItem of their own; their native recovered unit
        // is the ordinary pink petal. Use that unit for the surrogate, not an AIR stack.
        if (nativeItem == net.minecraft.world.item.Items.AIR && cultivar != null && !cultivar.produce().isEmpty()) {
            nativeItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(cultivar.produce().get(0)));
        }
        if (nativeItem == null || nativeItem == net.minecraft.world.item.Items.AIR) return InteractionResult.FAIL;
        // A surrogate native flower stack supplies native replacement/quantity checks. It is the
        // surrogate, never a full cluster or the player's flower stock, that native placement consumes.
        var placement = new net.minecraft.world.item.context.BlockPlaceContext(context.getLevel(), context.getPlayer(),
            context.getHand(), new net.minecraft.world.item.ItemStack(nativeItem),
            new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                context.getClickedPos(), context.isInside()));
        var item = nativeItem instanceof net.minecraft.world.item.BlockItem blockItem && blockItem.getBlock() == block
            ? blockItem : new net.minecraft.world.item.BlockItem(block, new Properties());
        if (cultivar != null && cultivar.placementAdapter().equals("water-flowerbed")) {
            // Helvola's native pad item places zero flowers. A cultivar seed instead starts
            // the pad with exactly one bloom; subsequent native flower additions stay native.
            item = new net.minecraft.world.item.BlockItem(block, new Properties()) {
                @Override protected BlockState getPlacementState(net.minecraft.world.item.context.BlockPlaceContext context) {
                    var state = super.getPlacementState(context);
                    if (state == null) return null;
                    for (var property : state.getProperties()) {
                        if (property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty amount
                                && amount.getName().equals("water_flower_amount")) return state.setValue(amount, 1);
                    }
                    return state;
                }
            };
        }
        var result = item.place(placement);
        if (result.consumesAction() && !context.getLevel().isClientSide()) {
            if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
                CultivarPlantings.planted(player, placement.getClickedPos(),
                    context.getLevel().getBlockState(placement.getClickedPos()), context.getItemInHand());
            }
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return result;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Block block = ForgeRegistries.BLOCKS.getValue(plant);
        if (block == null) return InteractionResult.FAIL;
        if (flower) return plantFlower(context, block);
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
