package com.bettercontent.betterbumblezonecrops;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

@OnlyIn(Dist.CLIENT)
public final class CultivarClient {
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        BlockEntityRenderers.register(BumblezoneCultivars.NURSERY_BE.get(), NurseryRenderer::new);
    }

    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        BumblezoneCultivars.DEDICATED_SEEDS.forEach((id, seed) ->
            event.register((stack, tintIndex) -> tintIndex == 0 ? SeedVisuals.color(id) : 0xFFFFFF, seed.get()));
    }
    private static final class NurseryRenderer implements BlockEntityRenderer<LivingPollenNurseryBlockEntity> {
        private NurseryRenderer(BlockEntityRendererProvider.Context context) {}
        @Override public void render(LivingPollenNurseryBlockEntity nursery, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(nursery.seedId()));
            if (item == null) return;
            pose.pushPose(); pose.translate(0.5, 0.62, 0.5); pose.scale(0.65F, 0.65F, 0.65F);
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(item), ItemDisplayContext.FIXED, light, overlay, pose, buffers, nursery.getLevel(), 0);
            pose.popPose();
        }
    }
}
