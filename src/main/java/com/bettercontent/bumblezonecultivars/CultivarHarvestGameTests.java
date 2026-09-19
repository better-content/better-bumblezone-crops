package com.bettercontent.bumblezonecultivars;
import com.bettercontent.bumblezonecultivars.api.event.CultivarHarvestEvent;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.UUID;
@GameTestHolder(BumblezoneCultivars.MOD_ID) @PrefixGameTestTemplate(false)
public final class CultivarHarvestGameTests {
 public static final class Probe {final UUID owner;int count;Probe(UUID owner){this.owner=owner;}@SubscribeEvent public void harvest(CultivarHarvestEvent event){if(event.owner.equals(owner))count++;}}
 @GameTest(template="empty",timeoutTicks=20) public static void maturityUsesActualKelpAndGourdStates(GameTestHelper helper){
  helper.assertTrue(CultivarLootModifier.isMature(Blocks.KELP.defaultBlockState(),"top-segment"),"Kelp head was not eligible for its one propagation decision");
  helper.assertTrue(!CultivarLootModifier.isMature(Blocks.KELP_PLANT.defaultBlockState(),"top-segment"),"Kelp body segment made an extra propagation decision");
  helper.assertTrue(!CultivarLootModifier.isMature(Blocks.MELON_STEM.defaultBlockState(),"fruit-block"),"Young ordinary stem was mature");
  helper.assertTrue(CultivarLootModifier.isMature(Blocks.MELON_STEM.defaultBlockState().setValue(StemBlock.AGE,StemBlock.MAX_AGE),"fruit-block"),"Maximum-age ordinary stem was not mature");
  helper.assertTrue(CultivarLootModifier.isMature(Blocks.ATTACHED_MELON_STEM.defaultBlockState(),"fruit-block"),"Attached stem was not explicitly mature");
  helper.succeed();
 }
 @GameTest(template="empty",timeoutTicks=100) public static void harvestRequiresSourcedPlantingAndRealProduce(GameTestHelper helper){
  var level=helper.getLevel();var player=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"cultivar-harvest"));var pos=helper.absolutePos(new BlockPos(2,1,2));
  var young=Blocks.WHEAT.defaultBlockState();var mature=young.setValue(CropBlock.AGE,7);
  level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),3);level.setBlock(pos,young,3);
  var seed=new ItemStack(Items.WHEAT_SEEDS);CultivarPlantings.source(seed,"the_bumblezone:the_bumblezone");CultivarPlantings.planted(player,pos,young,seed);
  var saved=CultivarPlantings.get(level).save(new net.minecraft.nbt.CompoundTag());helper.assertTrue(CultivarPlantings.load(saved).save(new net.minecraft.nbt.CompoundTag()).equals(saved),"Plant author/provenance failed persistence roundtrip");
  var probe=new Probe(player.getUUID());MinecraftForge.EVENT_BUS.register(probe);
  boolean drops=level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
  try {
   level.setBlock(pos,mature,3);
   Block.getDrops(mature,level,pos,null);helper.assertTrue(probe.count==0,"Loot simulation counted as harvest");
   level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(false,level.getServer());
   Block.dropResources(mature,level,pos);helper.assertTrue(probe.count==0,"Disabled drops counted as harvest");
   level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(true,level.getServer());
   Block.dropResources(mature,level,pos);helper.assertTrue(probe.count==1,"Actual sourced crop harvest failed to emit");
   Block.dropResources(mature,level,pos);helper.assertTrue(probe.count==1,"Repeated drops reused planting provenance");
   CultivarPlantings.planted(player,pos,young,new ItemStack(Items.WHEAT_SEEDS));Block.dropResources(mature,level,pos);helper.assertTrue(probe.count==1,"Unsourced seed claimed import history");
  } finally {level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(drops,level.getServer());MinecraftForge.EVENT_BUS.unregister(probe);}
  helper.succeed();
 }
}
