package com.bettercontent.bumblezonecultivars;

import com.bettercontent.bumblezonecultivars.internal.RootminEcologyAccess;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Applies one reproducible ecology profile to each newly spawned Bumblezone Rootmin. */
public final class RootminEcologyHandler {
    private static final ResourceLocation BUMBLEZONE = new ResourceLocation("the_bumblezone", "the_bumblezone");
    private static final ResourceLocation ROOTMIN = new ResourceLocation("the_bumblezone", "rootmin");
    private static final String PROFILE_KEY = "bumblezone_cultivars_rootmin_profile";
    private static final UUID SPEED_MODIFIER_ID = UUID.fromString("8f882638-4123-4cc4-a5f3-338ef871c81b");

    private RootminEcologyHandler() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.loadedFromDisk()) return;
        if (!BUMBLEZONE.equals(level.dimension().location())) return;

        Entity entity = event.getEntity();
        if (!ROOTMIN.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()))) return;
        if (!(entity instanceof LivingEntity rootmin)) return;
        var persistent = entity.getPersistentData();
        if (persistent.contains(PROFILE_KEY)) return;

        BlockPos spawnPos = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
        int variant = RootminEcologyPolicy.variant(level.getSeed(), level.dimension().location(), spawnPos);
        AttributeInstance speed = rootmin.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        speed.addPermanentModifier(new AttributeModifier(SPEED_MODIFIER_ID,
                "Bumblezone Rootmin ecology profile", RootminEcologyPolicy.movementSpeedModifier(variant),
                AttributeModifier.Operation.MULTIPLY_BASE));
        Block flower = ForgeRegistries.BLOCKS.getValue(RootminEcologyPolicy.flower(variant));
        if (flower != null) ((RootminEcologyAccess) rootmin).bc$setFlowerBlock(flower.defaultBlockState());
        persistent.putInt(PROFILE_KEY, variant);
    }
}
