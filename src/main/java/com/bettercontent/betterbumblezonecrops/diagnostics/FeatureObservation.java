package com.bettercontent.betterbumblezonecrops.diagnostics;
import java.util.function.Supplier;
/** Pure owner-specific projection of already computed native outcomes; no gameplay writes or RNG. */
public final class FeatureObservation {
    private FeatureObservation() {}
    public static void planting(PlaytestLog log, Supplier<PlaytestLog.Context> context, java.util.UUID operation, String cultivar) {
        if (!log.enabled("planting")) return;
        log.debug("planting", "cultivar.plant", context.get(), "committed", "origin_validated", () -> PlaytestLog.fields("operation", operation, "cultivar", cultivar));
    }
    public static void harvest(PlaytestLog log, Supplier<PlaytestLog.Context> context, boolean removed, java.util.UUID operation, String cultivar) {
        if (!log.enabled("harvest")) return;
        log.debug("harvest", "cultivar.harvest", context.get(), removed ? "committed" : "rejected", removed ? "native_produce_spawned" : "duplicate_or_replaced_planting", () -> PlaytestLog.fields("operation", operation, "cultivar", cultivar));
    }
    public static void decision(PlaytestLog log, String category, String event, String result, String reason) {
        if (!log.enabled(category)) return;
        log.event(category, event, "server", result, reason, () -> "");
    }
}
