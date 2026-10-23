package com.bettercontent.betterbumblezonecrops.nativefixture;

import java.util.List;

/** Bounded scalar-only original-apply/row/parse state. No native/codec/JSON objects. */
public final class ArsDispatchLedger {
    public static final String ID = "ars_nouveau:dungeon_loot";
    public static final String BODY = "com.hollingsworth.arsnouveau.api.loot.DungeonLootEnhancerModifier";
    private final ThreadLocal<Frame> current = new ThreadLocal<>();
    private int starts;
    private String fault;
    private static final class Frame {
        List<String> ids;
        int before, after, selected, bodies, arsBefore, arsAfter, ordinal = -1;
        boolean parse, ars;
        String digest, body;
    }
    public synchronized void fail(String code) { if (fault == null) fault = code; }
    public synchronized void begin(List<String> ids) {
        if (current.get() != null) { fail("STALE_APPLY"); current.remove(); }
        if (++starts > 4 || ids.size() != 72 || ids.stream().distinct().count() != 72
                || ids.stream().filter(ID::equals).count() != 1 || ids.stream().anyMatch(s -> s.length() > 160)) {
            fail("APPLY_BOUNDS"); return;
        }
        Frame f = new Frame(); f.ids = List.copyOf(ids); current.set(f);
    }
    public boolean activeArsParse() { Frame f = current.get(); return f != null && f.parse && f.ars; }
    public void before(String row, String digest) {
        Frame f = current.get();
        if (f == null) { fail("NO_APPLY"); return; }
        if (f.parse) { fail("UNMATCHED_BEFORE"); f.parse = false; }
        int i = f.before++;
        if (i >= 72 || !f.ids.get(i).equals(row)) fail("ROW_ORDER");
        f.ordinal = i; f.parse = true; f.ars = ID.equals(row); f.digest = digest;
        if (f.ars && (++f.arsBefore != 1 || digest == null || digest.length() != 64)) fail("ARS_INPUT");
    }
    public void registry(boolean exactReceiver, boolean exactOps, boolean successWithoutError,
            boolean exactSelectedRegisteredCodec, String key) {
        Frame f = current.get();
        if (!activeArsParse() || !exactReceiver) return; // unrelated/nested registry is not evidence
        if (!exactOps || !successWithoutError || !exactSelectedRegisteredCodec || !ID.equals(key)
                || ++f.selected != 1) fail("REGISTERED_DISPATCH");
    }
    public void body(boolean exactRootReceiver, boolean exactOps, boolean successWithoutError,
            String inputDigest, String implementation) {
        Frame f = current.get();
        if (!activeArsParse() || !exactRootReceiver) return; // includes lazy KEY codec and nested wrappers
        if (!exactOps || !successWithoutError || !java.util.Objects.equals(f.digest, inputDigest) || !BODY.equals(implementation)
                || f.selected != 1 || ++f.bodies != 1) fail("ROOT_BODY");
        f.body = implementation;
    }
    public void after(String row) {
        Frame f = current.get();
        if (f == null || !f.parse) { fail("UNMATCHED_AFTER"); return; }
        if (f.ordinal >= 72 || !f.ids.get(f.ordinal).equals(row)) fail("AFTER_ROW");
        if (f.ars && (++f.arsAfter != 1 || f.selected != 1 || f.bodies != 1)) fail("INCOMPLETE_ARS_PARSE");
        f.parse = false; f.ars = false; f.after++;
    }
    public synchronized void requireComplete(int arsOrdinal, String loadedImplementation) {
        Frame f = current.get();
        if (fault != null || f == null || f.parse || f.before != 72 || f.after != 72
                || f.arsBefore != 1 || f.arsAfter != 1 || f.selected != 1 || f.bodies != 1
                || arsOrdinal < 0 || arsOrdinal >= 72 || !ID.equals(f.ids.get(arsOrdinal))
                || !BODY.equals(loadedImplementation) || !BODY.equals(f.body))
            throw new IllegalStateException("Incomplete original Ars codec dispatch/body proof: " + fault);
    }
    public void end() { current.remove(); }
}
