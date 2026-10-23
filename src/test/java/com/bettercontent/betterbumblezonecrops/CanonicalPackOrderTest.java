package com.bettercontent.betterbumblezonecrops;

import com.bettercontent.betterbumblezonecrops.nativefixture.CanonicalPackOrder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CanonicalPackOrderTest {
    private static final String ID = CanonicalPackOrder.PACK_ID;

    @Test void canonicalFirstOrMiddleBecomesLastWithoutChangingOtherPacks() {
        var input = new ArrayList<>(List.of("vanilla", ID, "builtin/aether_accessories", "create:dynamic_data"));
        var before = List.copyOf(input);
        assertEquals(List.of("vanilla", "builtin/aether_accessories", "create:dynamic_data", ID),
            CanonicalPackOrder.highestPriority(input, true));
        assertEquals(before, input);
        assertEquals(List.of("vanilla", ID), CanonicalPackOrder.highestPriority(List.of(ID, "vanilla"), true));
    }
    @Test void requiredMissingCanonicalIsAdmittedAndAlreadyHighestPreservesIdentity() {
        assertEquals(List.of("vanilla", ID), CanonicalPackOrder.highestPriority(List.of("vanilla"), true));
        var ordered = List.of("vanilla", "create:dynamic_data", ID);
        assertSame(ordered, CanonicalPackOrder.highestPriority(ordered, true));
    }
    @Test void unrelatedRepositoriesAndDuplicateCanonicalFailClosed() {
        var input = List.of("vanilla", "unrelated");
        assertSame(input, CanonicalPackOrder.highestPriority(input, false));
        assertThrows(IllegalStateException.class,
            () -> CanonicalPackOrder.highestPriority(List.of(ID, "vanilla", ID), true));
    }
}
