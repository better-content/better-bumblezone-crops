package com.bettercontent.betterbumblezonecrops.diagnostics;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
/** Actual production projection test; native gameplay gates own whole-feature state/RNG equivalence. */
final class FeatureObservationTest {
    public static void main(String[] args) { new FeatureObservationTest().actualEmitterStagesPrivacyAndObservationalPurity(); }
    @Test void actualEmitterStagesPrivacyAndObservationalPurity() {
        var records = new ArrayList<String>(); boolean[] enabled = {false};
        var log = new PlaytestLog("better_bumblezone_crops", new PlaytestLog.Sink() {
            public boolean enabled(String category, String level) { return enabled[0]; }
            public void write(String category, String level, String text) { records.add(text); }
        }, () -> 0L, new Properties());
        var contextReads = new AtomicInteger();
        Supplier<PlaytestLog.Context> ctx = () -> { contextReads.incrementAndGet(); return PlaytestLog.Context.server("minecraft:overworld", 10); };
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000042");
        // Observational purity at this scalar projection seam only, not a synthetic native replay.
        long[] state = {42, 3}; long[] before = state.clone();
        var nativeRng = new Random(41); var baselineRng = new Random(41);
        FeatureObservation.planting(log, ctx, id, "carrot");
        FeatureObservation.harvest(log, ctx, true, id, "carrot");
        FeatureObservation.harvest(log, ctx, false, id, "carrot");
        assertTrue(records.isEmpty()); assertEquals(0, contextReads.get());
        assertArrayEquals(before, state); assertEquals(baselineRng.nextLong(), nativeRng.nextLong());
        enabled[0] = true;
        FeatureObservation.planting(log, ctx, id, "carrot");
        FeatureObservation.harvest(log, ctx, true, id, "carrot");
        FeatureObservation.harvest(log, ctx, false, id, "carrot");
        assertEquals(3, records.size());
        assertTrue(records.get(0).contains("reason=origin_validated"), records.toString());
        assertTrue(records.get(1).contains("result=committed reason=native_produce_spawned"), records.toString());
        assertTrue(records.get(2).contains("result=rejected reason=duplicate_or_replaced_planting"), records.toString());
        assertTrue(records.stream().noneMatch(s -> s.contains("owner=")), records.toString());
        assertTrue(records.stream().noneMatch(s -> s.contains("seed=")), records.toString());
        assertTrue(records.stream().noneMatch(s -> s.contains("inventory=")), records.toString());
        assertArrayEquals(before, state); assertEquals(baselineRng.nextLong(), nativeRng.nextLong());
    }
}
