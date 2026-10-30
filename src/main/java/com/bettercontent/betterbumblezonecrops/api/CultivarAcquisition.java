package com.bettercontent.betterbumblezonecrops.api;

import com.bettercontent.betterbumblezonecrops.CultivarCatalog;
import com.bettercontent.betterbumblezonecrops.CultivarDefinition;
import com.bettercontent.betterbumblezonecrops.CultivarLootModifier;
import com.bettercontent.betterbumblezonecrops.CultivarPlantings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Provider-owned aliases into EXISTING cultivar authority; never a new crop/dimension permission rule. */
public final class CultivarAcquisition {
    public static final int API_VERSION = 1;
    /** Callable runtime version: unlike a static final literal, this cannot be javac-inlined by consumers. */
    public static int apiVersion() { return API_VERSION; }
    public record Binding(String canonicalCultivarId, Set<ResourceLocation> seedAliases,
                          Set<ResourceLocation> plantAliases, Set<ResourceLocation> produceAliases) {
        public Binding {
            Objects.requireNonNull(canonicalCultivarId);
            seedAliases = Set.copyOf(seedAliases); plantAliases = Set.copyOf(plantAliases); produceAliases = Set.copyOf(produceAliases);
        }
    }
    public record HarvestPosition(BlockPos anchor, boolean primary) {
        public HarvestPosition { anchor = Objects.requireNonNull(anchor).immutable(); }
    }
    @FunctionalInterface public interface PlantAnchor { HarvestPosition resolve(BlockState state, BlockPos position); }
    private static Map<String, PlantAnchor> anchors = Map.of();
    private static final Map<String, CultivarDefinition> CANONICAL = canonical();
    private static final CultivarAliasRegistry REGISTRY = registry();
    private static volatile CultivarAliasRegistry.Snapshot frozen;
    private CultivarAcquisition() {}
    private static Map<String, CultivarDefinition> canonical() {
        Map<String, CultivarDefinition> result = new LinkedHashMap<>();
        for (var row : CultivarCatalog.ALL) if (result.putIfAbsent(row.id(), row) != null) throw new IllegalStateException("Duplicate canonical cultivar " + row.id());
        return Map.copyOf(result);
    }
    private static CultivarAliasRegistry registry() {
        Map<String, String> seeds = new HashMap<>(), plants = new HashMap<>(), produce = new HashMap<>();
        CANONICAL.forEach((id, row) -> { seeds.putIfAbsent(row.seedItem(), id); row.plantBlocks().forEach(p -> plants.putIfAbsent(p, id)); row.produce().forEach(p -> produce.putIfAbsent(p, id)); });
        return new CultivarAliasRegistry(CANONICAL.keySet(), seeds, plants, produce);
    }
    /** Register in setup before the first catalogue query. Targets are canonical catalogue IDs only.
     * One alias may be both seed and produce ONLY for the same cultivar (legacy dual-purpose stock).
     * Registration is atomic; duplicate owners/aliases, role conflicts and noncanonical targets fail.
     */
    public static synchronized void register(ResourceLocation owner, List<Binding> bindings) {
        register(owner, bindings, Map.of());
    }
    /** Typed source adapters handle multipart layouts not represented by vanilla HALF.
     * An adapter can belong only to a plant alias in this same atomic registration.
     */
    public static synchronized void register(ResourceLocation owner, List<Binding> bindings, Map<ResourceLocation, PlantAnchor> plantAnchors) {
        Objects.requireNonNull(owner); bindings = List.copyOf(bindings); plantAnchors = Map.copyOf(plantAnchors);
        Set<ResourceLocation> registeredPlants = new HashSet<>(); bindings.forEach(row -> registeredPlants.addAll(row.plantAliases()));
        Map<String, PlantAnchor> nextAnchors = new HashMap<>(anchors);
        plantAnchors.forEach((plant, adapter) -> {
            if (!registeredPlants.contains(plant)) throw new IllegalArgumentException("Anchor must belong to an alias in this registration: " + plant);
            if (nextAnchors.putIfAbsent(plant.toString(), adapter) != null) throw new IllegalStateException("Duplicate plant anchor: " + plant);
        });
        REGISTRY.register(owner.toString(), bindings.stream().map(row -> new CultivarAliasRegistry.Binding(row.canonicalCultivarId(),
            strings(row.seedAliases()), strings(row.plantAliases()), strings(row.produceAliases()))).toList());
        anchors = Map.copyOf(nextAnchors);
    }
    private static Set<String> strings(Set<ResourceLocation> ids) {
        Set<String> result = new HashSet<>(); for (var id : ids) result.add(id.toString()); return Set.copyOf(result);
    }
    /** Call after native item/block registration. First catalogue query also freezes.
     * No aliases means no new registry requirements and unchanged native catalogue behavior.
     */
    public static synchronized void freeze() {
        if (frozen != null) return;
        var snapshot = REGISTRY.freeze();
        for (String id : snapshot.seeds().keySet()) requireItem(id);
        for (String id : snapshot.produce().keySet()) requireItem(id);
        for (String id : snapshot.plants().keySet()) requireBlock(id);
        Set<String> targets = new HashSet<>(); targets.addAll(snapshot.seeds().values()); targets.addAll(snapshot.plants().values()); targets.addAll(snapshot.produce().values());
        for (String id : targets) {
            var row = CANONICAL.get(id); requireItem(row.seedItem()); row.plantBlocks().forEach(CultivarAcquisition::requireBlock); row.produce().forEach(CultivarAcquisition::requireItem);
        }
        frozen = snapshot;
    }
    private static void requireItem(String id) { if (!ForgeRegistries.ITEMS.containsKey(new ResourceLocation(id))) throw new IllegalStateException("Missing registered cultivar alias/canonical item " + id); }
    private static void requireBlock(String id) { if (!ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(id))) throw new IllegalStateException("Missing registered cultivar alias/canonical block " + id); }
    private static CultivarAliasRegistry.Snapshot snapshot() { var value = frozen; if (value == null) { freeze(); value = frozen; } return value; }
    public static CultivarDefinition seedAlias(String id) { String target = snapshot().seeds().get(id); return target == null ? null : CANONICAL.get(target); }
    public static CultivarDefinition plantAlias(String id) { String target = snapshot().plants().get(id); return target == null ? null : CANONICAL.get(target); }
    public static boolean produces(CultivarDefinition cultivar, String itemId) { return cultivar.id().equals(snapshot().produce().get(itemId)); }
    public static Map<String, CultivarDefinition> plantAliases() {
        Map<String, CultivarDefinition> result = new HashMap<>(); snapshot().plants().forEach((id, cultivar) -> result.put(id, CANONICAL.get(cultivar))); return Map.copyOf(result);
    }
    public static Optional<HarvestPosition> aliasHarvestPosition(BlockState state, BlockPos position) {
        Objects.requireNonNull(state); Objects.requireNonNull(position); snapshot();
        if (anchors.isEmpty()) return Optional.empty();
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        PlantAnchor adapter = id == null ? null : anchors.get(id.toString());
        if (adapter == null) return Optional.empty();
        HarvestPosition resolved = Objects.requireNonNull(adapter.resolve(state, position), "Null source plant anchor");
        if (resolved.primary() && !resolved.anchor().equals(position)) throw new IllegalStateException("Primary plant anchor moved its harvest position: " + id);
        return Optional.of(resolved);
    }
    /** Only for a NEW native production call, never an old inventory stack or a crop relocation refund.
     * Canonical edible produce is identity, even when its legacy input item doubled as a seed.
     * Actual seed output uses the provider's existing origin/propagation decision; no cooking rule is copied.
     */
    public static ItemStack restrictNewOutput(ResourceLocation dimension, ItemStack canonicalCandidate) {
        Objects.requireNonNull(dimension); Objects.requireNonNull(canonicalCandidate);
        if (canonicalCandidate.isEmpty()) return canonicalCandidate;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(canonicalCandidate.getItem());
        if (id == null) throw new IllegalArgumentException("Unregistered new native crop output");
        CultivarDefinition cultivar = CultivarCatalog.bySeed(id.toString());
        if (cultivar == null) return canonicalCandidate;
        if (!CultivarLootModifier.allowsNewSeedProduction(cultivar, dimension.toString())) return ItemStack.EMPTY;
        ItemStack result = canonicalCandidate.copy();
        CultivarPlantings.source(result, dimension.toString());
        return result;
    }
}
