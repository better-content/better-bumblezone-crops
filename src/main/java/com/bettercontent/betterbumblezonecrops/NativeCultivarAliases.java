package com.bettercontent.betterbumblezonecrops;

import com.bettercontent.betterbumblezonecrops.api.CultivarAcquisition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Existing three native KC identities; no canonical-output conversion or new seed roll. */
public final class NativeCultivarAliases {
    private NativeCultivarAliases() {}
    private static ResourceLocation kc(String path) { return new ResourceLocation("kaleidoscope_cookery", path); }
    public static void register() {
        if (CultivarAcquisition.apiVersion() != 1) throw new IllegalStateException("Unsupported cultivar alias API");
        CultivarAcquisition.register(new ResourceLocation(BumblezoneCultivars.MOD_ID, "kaleidoscope_crops"), List.of(
            new CultivarAcquisition.Binding("farmers_delight_tomato", Set.of(kc("tomato_seed")), Set.of(kc("tomato_crop")), Set.of(kc("tomato"))),
            new CultivarAcquisition.Binding("farmers_delight_cabbage", Set.of(kc("lettuce_seed")), Set.of(kc("lettuce_crop")), Set.of(kc("lettuce"))),
            new CultivarAcquisition.Binding("farmers_delight_rice", Set.of(kc("rice"), kc("wild_rice")), Set.of(kc("rice_crop")), Set.of(kc("rice"), kc("rice_panicle")))
        ), Map.of(kc("rice_crop"), NativeCultivarAliases::riceAnchor));
    }
    private static CultivarAcquisition.HarvestPosition riceAnchor(BlockState state, BlockPos position) {
        if (!kc("rice_crop").equals(ForgeRegistries.BLOCKS.getKey(state.getBlock())))
            throw new IllegalStateException("Rice LOCATION adapter received another crop");
        IntegerProperty location = state.getProperties().stream().filter(property -> property instanceof IntegerProperty
                && property.getName().equals("location")).map(property -> (IntegerProperty) property).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing pinned KC rice LOCATION"));
        int value = state.getValue(location);
        if (value < 0 || value > 2) throw new IllegalStateException("Pinned KC rice changed its three locations");
        return new CultivarAcquisition.HarvestPosition(position.below(value), value == 0);
    }
    /** Called only for an actual native NEW output, not inventory, old-world, or refund repair. */
    public static ItemStack newOutput(Level level, ItemStack nativeOutput) {
        if (level.isClientSide || nativeOutput.isEmpty()) return nativeOutput;
        return CultivarAcquisition.restrictNewOutput(level.dimension().location(), nativeOutput);
    }
}
