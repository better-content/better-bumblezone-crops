package com.bettercontent.bumblezonecultivars;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FirethornCueContractTest {
    @Test void onlyFirethornSeedGetsTheHazardCue() {
        assertEquals("item.bumblezone_cultivars.goety_firethorn_seeds.tooltip",
            FirethornCuePolicy.tooltipKey("goety:firethorn"));
        assertNull(FirethornCuePolicy.tooltipKey("minecraft:sweet_berry_bush"));
        assertNull(FirethornCuePolicy.tooltipKey("goety:other_plant"));
    }

    @Test void hazardCueIsLocalizedAndDescribesFastMatureContact() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(
            "assets/bumblezone_cultivars/lang/en_us.json")) {
            assertNotNull(stream);
            JsonObject language = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            String cue = language.get("item.bumblezone_cultivars.goety_firethorn_seeds.tooltip").getAsString();
            assertTrue(cue.contains("mature Firethorn"));
            assertTrue(cue.contains("Fast contact"));
        }
    }
}
