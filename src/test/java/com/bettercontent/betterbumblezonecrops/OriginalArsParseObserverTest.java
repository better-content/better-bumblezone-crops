package com.bettercontent.betterbumblezonecrops;

import com.bettercontent.betterbumblezonecrops.nativefixture.ArsDispatchLedger;
import com.bettercontent.betterbumblezonecrops.nativefixture.OriginalParseObserver;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class OriginalArsParseObserverTest {
    private static Codec<Integer> codec(DataResult<Integer> result, RuntimeException failure, AtomicInteger calls) {
        return new Codec<>() {
            @Override public <T> DataResult<Integer> parse(DynamicOps<T> ops, T input) {
                calls.incrementAndGet();
                if (failure != null) throw failure;
                return result;
            }
            @Override public <T> DataResult<Pair<Integer, T>> decode(DynamicOps<T> ops, T input) {
                throw new AssertionError("Observer must call the original public parse, not decode separately");
            }
            @Override public <T> DataResult<T> encode(Integer value, DynamicOps<T> ops, T prefix) {
                throw new AssertionError("No encoding is permitted");
            }
        };
    }
    @Test void forwardsOriginalSuccessfulOrFailedResultExactlyOnce() {
        for (DataResult<Integer> result : java.util.List.of(DataResult.success(7), DataResult.<Integer>error(() -> "negative"))) {
            var calls = new AtomicInteger();
            var observations = new AtomicInteger();
            var actual = OriginalParseObserver.parse(codec(result, null, calls), JsonOps.INSTANCE,
                new com.google.gson.JsonPrimitive(7), observed -> {
                    assertSame(result, observed);
                    observations.incrementAndGet();
                });
            assertSame(result, actual);
            assertEquals(1, calls.get());
            assertEquals(1, observations.get());
        }
    }
    @Test void preservesOriginalExceptionAndDoesNotInventAReturnObservation() {
        var original = new IllegalStateException("original native exception");
        var calls = new AtomicInteger();
        var observations = new AtomicInteger();
        var thrown = assertThrows(IllegalStateException.class, () -> OriginalParseObserver.parse(
            codec(null, original, calls), JsonOps.INSTANCE, com.google.gson.JsonNull.INSTANCE,
            result -> observations.incrementAndGet()));
        assertSame(original, thrown);
        assertEquals(1, calls.get());
        assertEquals(0, observations.get());
    }
    private static ArsDispatchLedger ledger(boolean registryReturn, boolean bodyReturn) {
        var ledger = new ArsDispatchLedger();
        var ids = new ArrayList<String>();
        ids.add(ArsDispatchLedger.ID);
        for (int i = 1; i < 72; i++) ids.add("fixture:row_" + i);
        ledger.begin(ids);
        for (String id : ids) {
            ledger.before(id, id.equals(ArsDispatchLedger.ID) ? "a".repeat(64) : null);
            if (id.equals(ArsDispatchLedger.ID)) {
                if (registryReturn) ledger.registry(true, true, true, true, id);
                if (bodyReturn) ledger.body(true, true, true, "a".repeat(64), ArsDispatchLedger.BODY);
            }
            ledger.after(id);
        }
        return ledger;
    }
    @Test void completeOriginalRegistryAndBodyReturnsAreBothRequired() {
        assertDoesNotThrow(() -> ledger(true, true).requireComplete(0, ArsDispatchLedger.BODY));
        assertThrows(IllegalStateException.class, () -> ledger(true, false).requireComplete(0, ArsDispatchLedger.BODY));
        assertThrows(IllegalStateException.class, () -> ledger(false, true).requireComplete(0, ArsDispatchLedger.BODY));
        assertThrows(IllegalStateException.class, () -> ledger(true, true).requireComplete(0, "fixture:wrong_body"));
    }
}
