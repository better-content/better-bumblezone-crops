package com.bettercontent.betterbumblezonecrops.diagnostics;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** Supplementary wiring/privacy invariant, not the actual emitter or native behavior test. */
final class LoggingBoundaryTest {
    @Test void authoritativeFeatureBoundaryAndPrivacyRemainExplicit() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/bettercontent/betterbumblezonecrops/CultivarPlantings.java")) +
            Files.readString(Path.of("src/main/java/com/bettercontent/betterbumblezonecrops/diagnostics/FeatureObservation.java"));
        assertTrue(source.contains("FeatureObservation.harvest"), "FeatureObservation.harvest");
        assertTrue(source.contains("cultivar.plant"), "cultivar.plant");
        assertTrue(source.contains("native_produce_spawned"), "native_produce_spawned");
        assertTrue(source.contains("duplicate_or_replaced_planting"), "duplicate_or_replaced_planting");
        assertFalse(source.contains("fields(\"nbt\""));
        assertFalse(source.contains("fields(\"payload\""));
        assertFalse(source.contains("fields(\"seed\""));
        assertTrue(source.contains("enabled("));
    }
}
