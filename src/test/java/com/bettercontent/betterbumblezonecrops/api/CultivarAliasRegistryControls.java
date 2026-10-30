package com.bettercontent.betterbumblezonecrops.api;
import java.util.*;

/** Executable without Minecraft bootstrap, Forge, JUnit or reflective access. */
public final class CultivarAliasRegistryControls {
    private static int assertions;
    private static CultivarAliasRegistry registry() {
        return new CultivarAliasRegistry(Set.of("rice", "tomato"), Map.of("native:rice_seed", "rice"),
            Map.of("native:rice_crop", "rice"), Map.of("native:rice", "rice"));
    }
    private static CultivarAliasRegistry.Binding row(String cultivar, String seed, String plant, String produce) {
        return new CultivarAliasRegistry.Binding(cultivar, seed == null ? Set.of() : Set.of(seed), plant == null ? Set.of() : Set.of(plant), produce == null ? Set.of() : Set.of(produce));
    }
    private static void check(boolean value) { assertions++; if (!value) throw new AssertionError("control " + assertions); }
    private static void rejects(Class<? extends Throwable> type, Runnable action) {
        assertions++; try { action.run(); } catch (Throwable failure) { if (type.isInstance(failure)) return; throw new AssertionError("Wrong failure", failure); } throw new AssertionError("Missing " + type.getSimpleName());
    }
    public static void run() {
        assertions = 0;
        var empty = registry().freeze(); check(empty.seeds().isEmpty() && empty.plants().isEmpty() && empty.produce().isEmpty());
        var r = registry(); r.register("owner:crop", List.of(row("rice", "legacy:rice", "legacy:crop", "legacy:rice")));
        var s = r.freeze(); check(s.seeds().get("legacy:rice").equals("rice")); check(s.plants().get("legacy:crop").equals("rice")); check(s.produce().get("legacy:rice").equals("rice")); check(!s.seeds().containsKey("native:rice_seed"));
        rejects(UnsupportedOperationException.class, () -> s.seeds().put("bad:mutation", "rice"));
        rejects(IllegalStateException.class, () -> r.register("owner:late", List.of(row("rice", "legacy:new", null, null))));
        var duplicate = registry(); duplicate.register("owner:one", List.of(row("rice", "legacy:rice", null, null)));
        rejects(IllegalStateException.class, () -> duplicate.register("owner:one", List.of(row("tomato", "legacy:tomato", null, null))));
        rejects(IllegalStateException.class, () -> duplicate.register("owner:two", List.of(row("rice", "legacy:rice", null, null))));
        var atomic = registry();
        rejects(IllegalArgumentException.class, () -> atomic.register("owner:atomic", List.of(row("rice", "legacy:rice", null, null), row("missing_alias_target", "legacy:second", null, null))));
        atomic.register("owner:atomic", List.of(row("rice", "legacy:rice", null, null))); check(atomic.freeze().seeds().size() == 1);
        var conflict = registry();
        rejects(IllegalArgumentException.class, () -> conflict.register("owner:conflict", List.of(row("rice", "legacy:dual", null, null), row("tomato", null, null, "legacy:dual"))));
        rejects(IllegalArgumentException.class, () -> registry().register("owner:native", List.of(row("rice", "native:rice_seed", null, null))));
        rejects(IllegalArgumentException.class, () -> registry().register("owner:nativecrossrole", List.of(row("tomato", "native:rice", null, null))));
        rejects(IllegalArgumentException.class, () -> registry().register("OWNER_BAD", List.of(row("rice", "legacy:rice", null, null))));
        rejects(IllegalArgumentException.class, () -> registry().register("owner:empty", List.of()));
        rejects(IllegalArgumentException.class, () -> registry().register("owner:emptyrow", List.of(row("rice", null, null, null))));
        rejects(IllegalArgumentException.class, () -> registry().register("owner:cycle", List.of(row("legacy:rice", "legacy:rice", null, null))));
        System.out.println("Cultivar alias controls: " + assertions + " assertions passed");
    }
    public static void main(String[] args) { run(); }
}
