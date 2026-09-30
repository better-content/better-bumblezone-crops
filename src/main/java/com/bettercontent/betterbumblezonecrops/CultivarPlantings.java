package com.bettercontent.betterbumblezonecrops;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Persistent provenance belongs to the planted operation, independently of nearby players. */
@Mod.EventBusSubscriber(modid=BumblezoneCultivars.MOD_ID)
public final class CultivarPlantings extends SavedData {
 static final String ORIGIN="BumblezoneCultivarOrigin";
 record Planting(UUID owner,String cultivar,UUID operation){}
 private final Map<Long,Planting> plantings=new HashMap<>();
 public static CultivarPlantings get(ServerLevel level){return level.getDataStorage().computeIfAbsent(CultivarPlantings::load,CultivarPlantings::new,"bumblezone_cultivar_plantings");}
 public static CultivarPlantings load(CompoundTag root){var data=new CultivarPlantings();for(var value:root.getList("plants",Tag.TAG_COMPOUND)){var row=(CompoundTag)value;if(row.hasUUID("owner")&&row.hasUUID("operation"))data.plantings.put(row.getLong("pos"),new Planting(row.getUUID("owner"),row.getString("cultivar"),row.getUUID("operation")));}return data;}
 @Override public CompoundTag save(CompoundTag root){var rows=new ListTag();plantings.forEach((pos,plant)->{var row=new CompoundTag();row.putLong("pos",pos);row.putUUID("owner",plant.owner());row.putString("cultivar",plant.cultivar());row.putUUID("operation",plant.operation());rows.add(row);});root.put("plants",rows);return root;}
 public static void source(ItemStack seed,String dimension){if(dimension.equals("the_bumblezone:the_bumblezone"))seed.getOrCreateTag().putString(ORIGIN,dimension);}
 public static void planted(ServerPlayer player,BlockPos pos,BlockState state,ItemStack seed){
  var existing=get(player.serverLevel());if(existing.plantings.remove(pos.asLong())!=null)existing.setDirty();
  if(player.isCreative()||player.isSpectator()||player.level().dimension()!=Level.OVERWORLD)return;
  var cultivar=CultivarCatalog.byPlant(state.getBlock());
  if(cultivar==null||seed.getTag()==null||!seed.getTag().getString(ORIGIN).equals("the_bumblezone:the_bumblezone")
    ||!cultivar.seedItem().equals(ForgeRegistries.ITEMS.getKey(seed.getItem()).toString()))return;
  var data=get(player.serverLevel());data.plantings.put(pos.asLong(),new Planting(player.getUUID(),cultivar.id(),UUID.randomUUID()));data.setDirty();
 }
 @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event){if(event.getEntity() instanceof ServerPlayer player){var main=player.getMainHandItem();var seed=main.getTag()!=null&&main.getTag().contains(ORIGIN)?main:player.getOffhandItem();planted(player,event.getPos(),event.getPlacedBlock(),seed);}}
 static Planting lookup(ServerLevel level,BlockPos pos,BlockState state){var cultivar=CultivarCatalog.byPlant(state.getBlock());var data=get(level);var planting=data.plantings.get(pos.asLong());if(planting!=null&&(cultivar==null||!cultivar.id().equals(planting.cultivar()))){data.plantings.remove(pos.asLong());data.setDirty();return null;}return planting;}
 static void harvested(ServerLevel level,BlockPos pos,Planting expected){var data=get(level);if(data.plantings.remove(pos.asLong(),expected)){data.setDirty();net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new com.bettercontent.betterbumblezonecrops.api.event.CultivarHarvestEvent(level,expected.owner(),pos,expected.cultivar(),expected.operation()));}}
}
