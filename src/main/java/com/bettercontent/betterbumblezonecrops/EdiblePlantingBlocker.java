package com.bettercontent.betterbumblezonecrops;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class EdiblePlantingBlocker {
    private EdiblePlantingBlocker() {}
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        var heldId = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (heldId == null) return;
        for (CultivarDefinition cultivar : CultivarCatalog.ALL) {
            if (cultivar.seedItem().equals(heldId.toString()) || !cultivar.produce().contains(heldId.toString())) continue;
            var pos = event.getPos().relative(event.getFace());
            for (String plantId : cultivar.plantBlocks()) {
                var block = ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation(plantId));
                if (block != null && block.defaultBlockState().canSurvive(event.getLevel(), pos)) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }
}
