package com.bettercontent.betterbumblezonecrops;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CultivarCatalog {
    public static final List<CultivarDefinition> ALL = load();
    private static final Map<String, CultivarDefinition> BY_SEED = indexSeeds();
    private static final Map<String, CultivarDefinition> BY_PLANT = indexPlants();
    private CultivarCatalog() {}

    private static List<CultivarDefinition> load() {
        var stream = CultivarCatalog.class.getResourceAsStream("/defaults/cultivars.json");
        if (stream == null) throw new IllegalStateException("Missing fixed cultivar catalog");
        return new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), new TypeToken<List<CultivarDefinition>>() {}.getType());
    }

    private static Map<String, CultivarDefinition> indexSeeds() {
        Map<String, CultivarDefinition> result = new LinkedHashMap<>();
        ALL.forEach(cultivar -> result.putIfAbsent(cultivar.seedItem(), cultivar));
        return Map.copyOf(result);
    }

    private static Map<String, CultivarDefinition> indexPlants() {
        Map<String, CultivarDefinition> result = new LinkedHashMap<>();
        ALL.forEach(cultivar -> cultivar.plantBlocks().forEach(plant -> result.putIfAbsent(plant, cultivar)));
        return Map.copyOf(result);
    }

    public static CultivarDefinition bySeed(String id) { return BY_SEED.get(id); }
    public static CultivarDefinition byPlant(String id) { return BY_PLANT.get(id); }
    public static CultivarDefinition byPlant(Block block) { return BlockIndex.BY_PLANT.get(block); }

    private static final class BlockIndex {
        private static final Map<Block, CultivarDefinition> BY_PLANT = index();

        private static Map<Block, CultivarDefinition> index() {
            Map<Block, CultivarDefinition> result = new LinkedHashMap<>();
            CultivarCatalog.BY_PLANT.forEach((id, cultivar) -> {
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
                if (block != null) result.putIfAbsent(block, cultivar);
            });
            return Map.copyOf(result);
        }
    }

    public static boolean resolves(CultivarDefinition c) {
        return ForgeRegistries.ITEMS.containsKey(new ResourceLocation(c.seedItem())) && c.plantBlocks().stream().allMatch(id -> ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(id)));
    }
}
