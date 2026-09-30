package com.bettercontent.betterbumblezonecrops;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class SeedTradeBlocker {
    private SeedTradeBlocker() {}

    @SubscribeEvent
    public static void onVillagerInteraction(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getTarget() instanceof AbstractVillager villager)) return;
        String dimension = event.getLevel().dimension().location().toString();
        villager.getOffers().removeIf(offer -> {
            var resultId = ForgeRegistries.ITEMS.getKey(offer.getResult().getItem());
            CultivarDefinition cultivar = resultId == null ? null : CultivarCatalog.bySeed(resultId.toString());
            return cultivar != null && !cultivar.originDimensions().contains(dimension);
        });
    }
}
