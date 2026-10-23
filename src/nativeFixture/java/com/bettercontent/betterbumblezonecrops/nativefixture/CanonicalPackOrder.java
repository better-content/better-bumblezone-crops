package com.bettercontent.betterbumblezonecrops.nativefixture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Native-only admission order; never loads resources, decodes data or changes any other pack's order. */
public final class CanonicalPackOrder {
    public static final String PACK_ID = "bc_crops_canonical_source";
    private CanonicalPackOrder() {}

    public static Collection<String> highestPriority(Collection<String> requested, boolean available) {
        if (!available) return requested;
        var ordered = new ArrayList<String>();
        int occurrences = 0;
        for (String id : requested) {
            if (PACK_ID.equals(id)) occurrences++;
            else ordered.add(id);
        }
        if (occurrences > 1) throw new IllegalStateException("Duplicate canonical selection");
        // Minecraft auto-selects BUILT_IN packs before rebuilding, so default TOP insertion
        // is otherwise bypassed. Keep all native packs, but admit this required fixed TOP last.
        ordered.add(PACK_ID);
        if (List.copyOf(requested).equals(ordered)) return requested;
        return List.copyOf(ordered);
    }
}
