package com.bettercontent.bumblezonecultivars;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RootminEcologySourceContractTest {
    @Test
    void handlerIsRegisteredAndTargetsOnlyNewBumblezoneRootmins() throws Exception {
        String registration = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bumblezonecultivars/BumblezoneCultivars.java"));
        String handler = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bumblezonecultivars/RootminEcologyHandler.java"));
        String access = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bumblezonecultivars/internal/RootminEcologyAccess.java"));
        String mixins = Files.readString(Path.of("src/main/resources/bumblezone_cultivars.mixins.json"));

        assertTrue(registration.contains("MinecraftForge.EVENT_BUS.register(RootminEcologyHandler.class)"));
        assertTrue(handler.contains("event.loadedFromDisk()"));
        assertTrue(handler.contains("the_bumblezone"));
        assertTrue(handler.contains("rootmin"));
        assertTrue(handler.contains("persistent.putInt(PROFILE_KEY, variant)"));
        assertTrue(handler.contains("internal.RootminEcologyAccess"));
        assertTrue(access.contains("package com.bettercontent.bumblezonecultivars.internal"));
        assertTrue(!handler.contains("mixin.RootminEcologyAccess"));
        assertTrue(mixins.contains("RootminEcologyMixin"));
    }

    @Test
    void visualCueAccessMatchesThePinnedRootminSetter() throws Exception {
        String mixin = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bumblezonecultivars/mixin/RootminEcologyMixin.java"));
        assertTrue(mixin.contains("com.telepathicgrunt.the_bumblezone.entities.mobs.RootminEntity"));
        assertTrue(mixin.contains("setFlowerBlock(BlockState state)"));
        assertTrue(mixin.contains("implements RootminEcologyAccess"));
    }
}
