package com.bettercontent.betterbumblezonecrops;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
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
 private static final class WildDrop {final ServerPlayer player;final BlockPos position;final String produce;boolean emitted;WildDrop(ServerPlayer player,BlockPos position,String produce){this.player=player;this.position=position.immutable();this.produce=produce;}}
 private static final ThreadLocal<Deque<Optional<Harvest>>> ACTIVE=ThreadLocal.withInitial(ArrayDeque::new);
 private static final ThreadLocal<Deque<Optional<WildDrop>>> WILD=ThreadLocal.withInitial(ArrayDeque::new);
 public static void begin(BlockState state,LevelAccessor world,BlockPos position){
  begin(state,world,position,null);
 }
 public static void begin(BlockState state,LevelAccessor world,BlockPos position,Entity actor){
  Harvest harvest=null;
  WildDrop wild=null;
  if(world instanceof ServerLevel level&&level.dimension()==Level.OVERWORLD){
   var id=ForgeRegistries.BLOCKS.getKey(state.getBlock());
   if(actor instanceof ServerPlayer player&&id!=null){
    String produce=switch(id.toString()){
     case "farmersdelight:wild_carrots" -> "minecraft:carrot";
     case "farmersdelight:wild_potatoes" -> "minecraft:potato";
     default -> null;
    };
    if(produce!=null)wild=new WildDrop(player,position,produce);
   }
   BlockPos anchor=state.hasProperty(DoublePlantBlock.HALF)&&state.getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.UPPER?position.below():position;
   BlockState base=anchor.equals(position)?state:level.getBlockState(anchor);
   var cultivar=CultivarCatalog.byPlant(base.getBlock());var planting=CultivarPlantings.lookup(level,anchor,base);
   if(cultivar!=null&&planting!=null&&CultivarLootModifier.isMature(base,cultivar.maturityRule()))harvest=new Harvest(level,anchor.immutable(),cultivar,planting);}
  ACTIVE.get().push(Optional.ofNullable(harvest));
  WILD.get().push(Optional.ofNullable(wild));
 }
 public static void end(){var stack=ACTIVE.get();if(!stack.isEmpty())stack.pop();if(stack.isEmpty())ACTIVE.remove();var wild=WILD.get();if(!wild.isEmpty())wild.pop();if(wild.isEmpty())WILD.remove();}
 public static void spawned(Level world,Entity entity){
  if(!(entity instanceof ItemEntity item))return;
  var wild=WILD.get();if(!wild.isEmpty()){
   var observed=wild.peek().orElse(null);
   if(observed!=null&&!observed.emitted&&world==observed.player.serverLevel()
      &&observed.produce.equals(ForgeRegistries.ITEMS.getKey(item.getItem().getItem()).toString())){
    observed.emitted=true;
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new com.bettercontent.betterbumblezonecrops.api.event.WildVegetableDropEvent(observed.player,observed.position));
   }
  }
  var stack=ACTIVE.get();if(stack.isEmpty())return;
  var harvest=stack.peek().orElse(null);if(harvest==null||world!=harvest.level())return;
  String itemId=ForgeRegistries.ITEMS.getKey(item.getItem().getItem()).toString();
  if(harvest.cultivar().produce().contains(itemId))CultivarPlantings.harvested(harvest.level(),harvest.position(),harvest.planting());
 }
}
