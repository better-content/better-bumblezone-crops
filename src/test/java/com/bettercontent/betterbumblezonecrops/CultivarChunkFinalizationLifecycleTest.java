package com.bettercontent.betterbumblezonecrops;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class CultivarChunkFinalizationLifecycleTest {
    @Test
    void chunkLoadInitializationDoesNotRequestTheLoadingChunkAgain() throws IOException {
        final String blockEntity = Files.readString(Path.of(
                "src/main/java/com/bettercontent/betterbumblezonecrops/LivingPollenNurseryBlockEntity.java"));
        final String finalizer = Files.readString(Path.of(
                "src/main/java/com/bettercontent/betterbumblezonecrops/CultivarChunkFinalizer.java"));

        final int initializerStart = blockEntity.indexOf("void initializeSeedId");
        final int initializerEnd = blockEntity.indexOf("@Override", initializerStart);
        assertTrue(initializerStart >= 0 && initializerEnd > initializerStart);
        assertFalse(blockEntity.substring(initializerStart, initializerEnd).contains("setChanged("));

        assertTrue(finalizer.contains("nursery.initializeSeedId(chosen.seedItem());"));
        assertTrue(finalizer.contains("chunk.setUnsaved(true);"));
        assertFalse(finalizer.contains("nursery.setSeedId("));
    }
}
