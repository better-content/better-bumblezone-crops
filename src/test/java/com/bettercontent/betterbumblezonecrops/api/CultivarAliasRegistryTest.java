package com.bettercontent.betterbumblezonecrops.api;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CultivarAliasRegistryTest {
    @Test void setupOwnershipControls() { CultivarAliasRegistryControls.run(); }
    @Test void callableRuntimeApiVersion() { assertEquals(1, CultivarAcquisition.apiVersion()); }

    @Test void threeExistingNativeAliasesKeepRiceSeedAndProduceRolesDistinct() {
        // Pure registry contract for the three production bindings; registration with
        // real KC blocks, rice LOCATION and native drop counts require a native check.
        var registry = new CultivarAliasRegistry(Set.of("farmers_delight_tomato", "farmers_delight_cabbage", "farmers_delight_rice"),
                Map.of(), Map.of(), Map.of());
        registry.register("better_bumblezone_crops:kaleidoscope_crops", List.of(
                new CultivarAliasRegistry.Binding("farmers_delight_tomato",
                        Set.of("kaleidoscope_cookery:tomato_seed"), Set.of("kaleidoscope_cookery:tomato_crop"),
                        Set.of("kaleidoscope_cookery:tomato")),
                new CultivarAliasRegistry.Binding("farmers_delight_cabbage",
                        Set.of("kaleidoscope_cookery:lettuce_seed"), Set.of("kaleidoscope_cookery:lettuce_crop"),
                        Set.of("kaleidoscope_cookery:lettuce")),
                new CultivarAliasRegistry.Binding("farmers_delight_rice",
                        Set.of("kaleidoscope_cookery:rice", "kaleidoscope_cookery:wild_rice"),
                        Set.of("kaleidoscope_cookery:rice_crop"),
                        Set.of("kaleidoscope_cookery:rice", "kaleidoscope_cookery:rice_panicle"))));
        var aliases = registry.freeze();
        assertEquals(4, aliases.seeds().size());
        assertEquals(3, aliases.plants().size());
        assertEquals(4, aliases.produce().size());
        assertEquals("farmers_delight_rice", aliases.seeds().get("kaleidoscope_cookery:rice"));
        assertEquals("farmers_delight_rice", aliases.produce().get("kaleidoscope_cookery:rice"));
        assertEquals("farmers_delight_rice", aliases.produce().get("kaleidoscope_cookery:rice_panicle"));
        assertFalse(aliases.seeds().containsKey("fruitsdelight:rice"));
    }
}
