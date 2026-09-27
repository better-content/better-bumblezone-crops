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
import net.minecraft.resources.ResourceLocation;

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
        CultivarCatalog.ALL.stream().filter(c -> c.seedItem().startsWith(MOD_ID + ":"))
            .forEach(c -> seed(new ResourceLocation(c.seedItem()).getPath(), c.plantBlocks().get(0),
                c.growthForm().equals("flower")));
    }

    private static void seed(String id, String plant, boolean flower) {
        DEDICATED_SEEDS.put(id, ITEMS.register(id, () -> new CultivarSeedItem(plant, flower)));
    }

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
