# Source-only cultivar acquisition API v1

`com.bettercontent.betterbumblezonecrops.api.CultivarAcquisition.apiVersion() == 1`.
Consumers must call this getter for a runtime version guard; the convenience `API_VERSION` literal alone is javac-inlined and cannot detect a different runtime provider.
This is an opt-in setup integration, not a new dimension, planting, growth, probability or quota policy. Default registration is empty. Native catalogue definitions, maturity rules, origin dimensions, propagation quotas/rolls, saved-data format and existing stock remain unchanged.

```java
record Binding(String canonicalCultivarId,
               Set<ResourceLocation> seedAliases,
               Set<ResourceLocation> plantAliases,
               Set<ResourceLocation> produceAliases) {}
register(ResourceLocation owner, List<Binding> bindings);
register(ResourceLocation owner, List<Binding> bindings,
         Map<ResourceLocation, PlantAnchor> sourceAnchors);
record HarvestPosition(BlockPos anchor, boolean primary) {}
interface PlantAnchor {
    HarvestPosition resolve(BlockState state, BlockPos position);
}
freeze();
ItemStack restrictNewOutput(ResourceLocation actualDimension, ItemStack canonicalCandidate);
```

Register before the first catalogue query. Explicit `freeze()` and the first catalogue query freeze registration; freeze must occur **after native item/block registration** because it validates real source and canonical item/block IDs. Duplicate owners/role aliases, unknown or alias cultivar targets, attempts to rebind native canonical identities, mismatched cross-role ownership, missing registry IDs and late registrations fail visibly. Targets must be existing canonical catalogue IDs: aliases cannot target aliases, so cycles cannot be authored. Registration, including optional source anchors, is atomic. Adapters belong only to plant aliases in that same registration. Use typed native properties, never reflection or guessed property names.

A source item may retain both seed and produce roles only for the same cultivar. This is needed for finite OLD KC rice stock: it was an actual native seed. This registration does **not** replace old item stacks. NEW canonical FD rice is edible produce, not a dedicated planting seed; existing `WildCropForage.blocksPlanting(String itemId, String blockId)` remains the planting authority. No crop receives a new blanket dimension growth/planting ban.

`CultivarCatalog.bySeed/byPlant` return the **same canonical definition** for an alias. `isSeed/produces` add alias-role recognition without modifying the definition. Planting provenance still requires the physical seed's existing `BumblezoneCultivarOrigin` tag; old unmarked stock is not given a fabricated origin. Canonical and aliased seed outputs are removed together before the existing single `seedCount`/RNG branch. Canonical produce is not treated as a seed. CHEF purchasing produce for emeralds is not a seed-sale offer.

Multipart sources need a typed adapter. KC rice uses `RiceCropBlock.LOCATION` 0/1/2, not `DoublePlantBlock.HALF`: source owner resolves `position.below(location)`, primary only at 0. The provider suppresses all middle/top propagules and returns **before any roll/refund**, and records planting only for the primary segment. Actual harvest provenance resolves the same root. Canonical/no-alias plants retain their previous HALF handling.

`restrictNewOutput` is only for a newly generated output at an audited native callsite, after canonicalization. Non-seed produce is identity, preserving the native five panicles/four edible rice. Actual seed output outside its existing origin dimensions is suppressed through the provider's existing seed-production decision; an allowed NEW seed receives its real current source marker and a detached copy, preserving count and meaningful tags. This API does not issue sourced-planting relocation refunds, roll additional randomness or change existing crop-loot quotas. Never route inventories, old found stock, arbitrary ItemEntities or existing crop-refund stacks through it.

## Cooking's exact source bindings

- `farmers_delight_tomato`: KC `tomato_seed`, `tomato_crop`, `tomato`.
- `farmers_delight_cabbage`: KC `lettuce_seed`, `lettuce_crop`, `lettuce`.
- `farmers_delight_rice`: seed aliases KC `rice` and `wild_rice`; plant `rice_crop`; produce aliases KC `rice` and `rice_panicle`; typed three-part source anchor is mandatory for this integration.

All KC IDs use `kaleidoscope_cookery:`. Registration is owner-controlled and must remain absent for inert Cooking DiscoveryOnly operation. This provider never reads or approves Cooking's food/ingredient aliases.

## Evidence boundary

The pure registry controls and mapped compilation are not transformed/native acceptance. Native acceptance still needs: actual alias definitions and registry IDs, canonical/legacy duplicate suppression, all three rice segments and unchanged origin quota/RNG, actual sourced/unsourced planting and exactly-once harvest, old-stock identity, direct-pop new seed denial versus ordinary produce identity, and default-no-alias controls. No native pass, pack activation or deployment is claimed by source/API compilation.
