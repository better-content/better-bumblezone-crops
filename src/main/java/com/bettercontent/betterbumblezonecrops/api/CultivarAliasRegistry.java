package com.bettercontent.betterbumblezonecrops.api;

import java.util.*;

/** Pure setup ownership/indexing; alias targets are canonical cultivar IDs, never other aliases. */
final class CultivarAliasRegistry {
    record Binding(String cultivar, Set<String> seeds, Set<String> plants, Set<String> produce) {
        Binding { Objects.requireNonNull(cultivar); seeds = Set.copyOf(seeds); plants = Set.copyOf(plants); produce = Set.copyOf(produce); }
    }
    record Snapshot(Map<String, String> seeds, Map<String, String> plants, Map<String, String> produce) {
        Snapshot { seeds = Map.copyOf(seeds); plants = Map.copyOf(plants); produce = Map.copyOf(produce); }
    }
    private final Set<String> cultivars;
    private final Map<String, String> nativeSeeds, nativePlants, nativeProduce;
    private final Set<String> owners = new HashSet<>();
    private Map<String, String> seeds = Map.of(), plants = Map.of(), produce = Map.of();
    private boolean frozen;
    CultivarAliasRegistry(Set<String> cultivars, Map<String, String> nativeSeeds, Map<String, String> nativePlants, Map<String, String> nativeProduce) {
        this.cultivars = Set.copyOf(cultivars); this.nativeSeeds = Map.copyOf(nativeSeeds); this.nativePlants = Map.copyOf(nativePlants); this.nativeProduce = Map.copyOf(nativeProduce);
    }
    synchronized void register(String owner, List<Binding> bindings) {
        id(owner); Objects.requireNonNull(bindings);
        if (frozen) throw new IllegalStateException("Cultivar acquisition aliases are frozen");
        if (owners.contains(owner)) throw new IllegalStateException("Duplicate cultivar alias owner " + owner);
        if (bindings.isEmpty()) throw new IllegalArgumentException("Empty cultivar alias registration");
        Map<String, String> nextSeeds = new LinkedHashMap<>(seeds), nextPlants = new LinkedHashMap<>(plants), nextProduce = new LinkedHashMap<>(produce);
        for (Binding row : bindings) {
            Objects.requireNonNull(row);
            if (!cultivars.contains(row.cultivar())) throw new IllegalArgumentException("Alias target must be an existing canonical cultivar, not an alias: " + row.cultivar());
            if (row.seeds().isEmpty() && row.plants().isEmpty() && row.produce().isEmpty()) throw new IllegalArgumentException("Empty cultivar binding");
            add(nextSeeds, nativeSeeds, row.seeds(), row.cultivar()); add(nextPlants, nativePlants, row.plants(), row.cultivar()); add(nextProduce, nativeProduce, row.produce(), row.cultivar());
        }
        Map<String, String> all = new HashMap<>();
        for (Map<String, String> role : List.of(nextSeeds, nextPlants, nextProduce)) role.forEach((alias, cultivar) -> {
            if (nativeSeeds.containsKey(alias) || nativePlants.containsKey(alias) || nativeProduce.containsKey(alias))
                throw new IllegalArgumentException("Cannot rebind any canonical native identity: " + alias);
            String previous = all.putIfAbsent(alias, cultivar);
            if (previous != null && !previous.equals(cultivar)) throw new IllegalArgumentException("Cross-role cultivar alias conflict: " + alias);
        });
        // Commit atomically only after every row/role validates.
        seeds = Map.copyOf(nextSeeds); plants = Map.copyOf(nextPlants); produce = Map.copyOf(nextProduce); owners.add(owner);
    }
    private static void add(Map<String, String> aliases, Map<String, String> nativeIds, Set<String> ids, String cultivar) {
        for (String alias : ids) {
            id(alias);
            if (nativeIds.containsKey(alias)) throw new IllegalArgumentException("Cannot rebind a canonical native identity: " + alias);
            if (aliases.putIfAbsent(alias, cultivar) != null) throw new IllegalStateException("Duplicate cultivar alias: " + alias);
        }
    }
    private static void id(String id) {
        Objects.requireNonNull(id);
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) throw new IllegalArgumentException("Invalid cultivar alias/provider ID: " + id);
    }
    synchronized Snapshot freeze() { frozen = true; return new Snapshot(seeds, plants, produce); }
}
