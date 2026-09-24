package com.bettercontent.bumblezonecultivars;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import com.mojang.serialization.Codec;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod(BumblezoneCultivars.MOD_ID)
public final class BumblezoneCultivars {
    public static final String MOD_ID = "bumblezone_cultivars";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);
    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> PROPAGATION = LOOT_MODIFIERS.register("propagation", () -> CultivarLootModifier.CODEC);
    public static final Map<String, RegistryObject<Item>> DEDICATED_SEEDS = new LinkedHashMap<>();

    public static final RegistryObject<Block> LIVING_POLLEN_NURSERY = BLOCKS.register("living_pollen_nursery", () ->
        new LivingPollenNurseryBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    public static final RegistryObject<BlockEntityType<LivingPollenNurseryBlockEntity>> NURSERY_BE = BLOCK_ENTITIES.register("living_pollen_nursery", () ->
        BlockEntityType.Builder.of(LivingPollenNurseryBlockEntity::new, LIVING_POLLEN_NURSERY.get()).build(null));

    static {
        seed("minecraft_carrot_seeds", "minecraft:carrots");
        seed("minecraft_potato_seeds", "minecraft:potatoes");
        seed("minecraft_sweet_berry_seeds", "minecraft:sweet_berry_bush");
        seed("minecraft_glow_berry_seeds", "minecraft:cave_vines");
        seed("minecraft_brown_mushroom_spores", "minecraft:brown_mushroom");
        seed("minecraft_red_mushroom_spores", "minecraft:red_mushroom");
        seed("minecraft_kelp_spores", "minecraft:kelp");
        seed("minecraft_cocoa_cutting", "minecraft:cocoa");
        seed("aether_berry_bush_seeds", "aether:berry_bush");
        seed("farmersrespite_coffee_seeds", "farmersrespite:coffee_bush");
        seed("farmersdelight_onion_seeds", "farmersdelight:onions");
        seed("farmersdelight_rice_seeds", "farmersdelight:rice");
        seed("ubesdelight_garlic_seeds", "ubesdelight:garlic_crop");
        seed("ubesdelight_ginger_seeds", "ubesdelight:ginger_crop");
        seed("ubesdelight_ube_seeds", "ubesdelight:ube_crop");
        seed("natures_spirit_shiitake_spores", "natures_spirit:shiitake_mushroom");
        seed("minecraft_nether_wart_spores", "minecraft:nether_wart");
        seed("minecraft_sugar_cane_cutting", "minecraft:sugar_cane");
        seed("minecraft_cactus_cutting", "minecraft:cactus");
        seed("minecraft_bamboo_shoots", "minecraft:bamboo");
        seed("ars_nouveau_sourceberry_seeds", "ars_nouveau:sourceberry_bush");
        seed("hexerei_belladonna_seeds", "hexerei:belladonna_plant");
        seed("hexerei_mandrake_seeds", "hexerei:mandrake_plant");
        seed("hexerei_mugwort_seeds", "hexerei:mugwort_bush");
        seed("hexerei_yellow_dock_seeds", "hexerei:yellow_dock_bush");
        seed("natures_spirit_green_bearberry_seeds", "natures_spirit:green_bearberries");
        seed("natures_spirit_purple_bearberry_seeds", "natures_spirit:purple_bearberries");
        seed("natures_spirit_red_bearberry_seeds", "natures_spirit:red_bearberries");
        seed("twilightforest_torchberry_seeds", "twilightforest:torchberry_plant");
        seed("goety_firethorn_seeds", "goety:firethorn");
    }

    private static void seed(String id, String plant) { DEDICATED_SEEDS.put(id, ITEMS.register(id, () -> new CultivarSeedItem(plant))); }

    public BumblezoneCultivars() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus); BLOCKS.register(bus); BLOCK_ENTITIES.register(bus); LOOT_MODIFIERS.register(bus);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            bus.addListener(CultivarClient::registerRenderers);
            bus.addListener(CultivarClient::registerItemColors);
        });
        MinecraftForge.EVENT_BUS.register(CultivarChunkFinalizer.class);
        MinecraftForge.EVENT_BUS.register(RootminEcologyHandler.class);
        MinecraftForge.EVENT_BUS.register(EdiblePlantingBlocker.class);
        MinecraftForge.EVENT_BUS.register(SeedTradeBlocker.class);
    }
}
