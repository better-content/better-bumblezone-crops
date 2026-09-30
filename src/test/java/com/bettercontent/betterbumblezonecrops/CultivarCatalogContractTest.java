package com.bettercontent.betterbumblezonecrops;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CultivarCatalogContractTest {
    @Test void indexedLookupsResolveCatalogEntries() {
        assertEquals("wheat", CultivarCatalog.bySeed("minecraft:wheat_seeds").id());
        assertEquals("wheat", CultivarCatalog.byPlant("minecraft:wheat").id());
        assertNull(CultivarCatalog.byPlant("minecraft:stone"));
    }

    @Test void everyCultivarHasThePublicContractAndDistinctPropagule() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("defaults/cultivars.json")) {
            assertNotNull(stream);
            JsonArray entries = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            assertTrue(entries.size() >= 40, "catalog must cover the live pack's principal food and magical flora");
            var ids = new HashSet<String>();
            for (var element : entries) {
                var cultivar = element.getAsJsonObject();
                String id = cultivar.get("id").getAsString();
                assertTrue(ids.add(id), "duplicate cultivar " + id);
                for (String field : new String[]{"seedItem", "plantBlocks", "produce", "growthForm", "maturityRule", "placementAdapter", "propagationAnchor", "originDimensions"}) {
                    assertTrue(cultivar.has(field), id + " lacks " + field);
                }
                String seed = cultivar.get("seedItem").getAsString();
                for (var produce : cultivar.getAsJsonArray("produce")) {
                    assertNotEquals(produce.getAsString(), seed, id + " plants edible produce directly");
                }
                assertFalse(cultivar.getAsJsonArray("plantBlocks").isEmpty());
                assertFalse(cultivar.getAsJsonArray("originDimensions").isEmpty());
            }
        }
    }

    @Test void publishedTagsCoverTheCompleteCatalog() throws Exception {
        JsonArray entries;
        try (var stream = getClass().getClassLoader().getResourceAsStream("defaults/cultivars.json")) {
            assertNotNull(stream);
            entries = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
        }
        Set<String> expectedSeeds = new HashSet<>();
        Set<String> expectedPlants = new HashSet<>();
        for (var element : entries) {
            var cultivar = element.getAsJsonObject();
            expectedSeeds.add(cultivar.get("seedItem").getAsString());
            cultivar.getAsJsonArray("plantBlocks").forEach(plant -> expectedPlants.add(plant.getAsString()));
        }
        assertEquals(expectedSeeds, tagValues("data/better_bumblezone_crops/tags/items/seeds.json"));
        assertEquals(expectedPlants, tagValues("data/better_bumblezone_crops/tags/blocks/origin/bumblezone.json"));
    }

    private Set<String> tagValues(String path) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            var values = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonArray("values");
            Set<String> result = new HashSet<>();
            values.forEach(value -> result.add(value.getAsString()));
            return result;
        }
    }
}
