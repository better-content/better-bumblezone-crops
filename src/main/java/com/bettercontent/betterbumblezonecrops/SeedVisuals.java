package com.bettercontent.betterbumblezonecrops;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class SeedVisuals {
    private static final String MANIFEST = "/assets/better_bumblezone_crops/seed_tints.json";
    private static final Map<String, String> TINTS = load();

    private SeedVisuals() {}

    public static int color(String seedId) {
        String value = TINTS.get(seedId);
        if (value == null || !value.matches("#[0-9A-Fa-f]{6}")) return 0xFFFFFF;
        return Integer.parseInt(value.substring(1), 16);
    }

    private static Map<String, String> load() {
        var stream = SeedVisuals.class.getResourceAsStream(MANIFEST);
        if (stream == null) throw new IllegalStateException("Missing seed tint manifest " + MANIFEST);
        return Map.copyOf(new Gson().fromJson(
            new InputStreamReader(stream, StandardCharsets.UTF_8),
            new TypeToken<Map<String, String>>() {}.getType()));
    }
}
