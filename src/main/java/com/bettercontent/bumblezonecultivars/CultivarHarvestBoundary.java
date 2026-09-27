package com.bettercontent.bumblezonecultivars;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
/** A loot calculation alone never enters this boundary: the actual block drop call must spawn produce. */
public final class CultivarHarvestBoundary {
 private record Harvest(ServerLevel level,BlockPos position,CultivarDefinition cultivar,CultivarPlantings.Planting planting){}
 private static final ThreadLocal<Deque<Optional<Harvest>>> ACTIVE=ThreadLocal.withInitial(ArrayDeque::new);
 public static void begin(BlockState state,LevelAccessor world,BlockPos position){
  Harvest harvest=null;
  if(world instanceof ServerLevel level&&level.dimension()==Level.OVERWORLD){
   BlockPos anchor=state.hasProperty(DoublePlantBlock.HALF)&&state.getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.UPPER?position.below():position;
   BlockState base=anchor.equals(position)?state:level.getBlockState(anchor);
   var cultivar=CultivarCatalog.byPlant(base.getBlock());var planting=CultivarPlantings.lookup(level,anchor,base);
   if(cultivar!=null&&planting!=null&&CultivarLootModifier.isMature(base,cultivar.maturityRule()))harvest=new Harvest(level,anchor.immutable(),cultivar,planting);}
  ACTIVE.get().push(Optional.ofNullable(harvest));
 }
 public static void end(){var stack=ACTIVE.get();if(!stack.isEmpty())stack.pop();if(stack.isEmpty())ACTIVE.remove();}
 public static void spawned(Level world,Entity entity){
  var stack=ACTIVE.get();if(stack.isEmpty()||!(entity instanceof ItemEntity item))return;
  var harvest=stack.peek().orElse(null);if(harvest==null||world!=harvest.level())return;
  String itemId=ForgeRegistries.ITEMS.getKey(item.getItem().getItem()).toString();
  if(harvest.cultivar().produce().contains(itemId))CultivarPlantings.harvested(harvest.level(),harvest.position(),harvest.planting());
 }
}
