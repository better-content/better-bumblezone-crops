# Better Bumblezone Crops

See [bounded playtest observations](docs/playtest-observations.md) for planting/actual-drop diagnostics and privacy limits.

## Scope and authority

This repository owns its mod-specific behavior and authoring inputs. Read [local instructions](AGENTS.md)
and the [shared documentation/policy index](../../better-content-modpack/docs/README.md).


Better Content-owned Forge mod that makes Bumblezone the acquisition origin for cultivatable food flora while preserving portable Overworld farming.

Requires Java 17, Minecraft 1.20.1, and Forge 47.4.13. Build from this repository with its own checked-in Gradle wrapper:

```sh
./gradlew --no-daemon verifyFull stageRuntimeJar
```

Native verification requires explicit `BC_CANONICAL_PACK_ROOT`, `BC_CANONICAL_ARCHIVES`,
`BC_CANONICAL_ACTIVITY`, `BC_OWNER_SESSION` and `BC_CUSTOM_MOD_JAR_DIR` bindings.
The canonical provider DAG qualifies Compat Fixes and Gameplay Notices first, then supplies their
exact staged JARs only to `gameTestServer`. There is no sibling-output or historical-JAR fallback.
The thin Compat JAR has no transitive metadata; the native fixture explicitly supplies its required
MixinExtras0.5.4 runtime, not an assumed older copy nested in another upstream provider.
`gradle/native-sdk-roles.json` closes the entire declared SDK as 28 upstream original/API coordinates
(the existing 26 base providers plus Create and the explicitly hash-pinned MixinExtras API) and two
native-only canonical producers. Bumblezone's existing dedicated development configuration is included;
its remapped file is not a new provider. Whole-graph equality precedes original-role projection, and
extra/missing/duplicate/unknown coordinates fail with complete graph differences. The snapshot retains
configuration roles, dynamic canonical raw/source-metadata hashes, exact source commits and all peer
data-resource hashes. Compat's reviewed empty `replace:false, entries:[]` GLM index is explicitly
classified, not hidden or counted as an original declaration; new/changed peer indexes fail closed.
The original fixed53 archive model, raw77 rows, selected72 and 11 native tests remain unchanged.
The trusted Root caller owns qualification through its common build gates; source metadata alone is
not owning-gate proof. Both Source-control and canonical-release staging are supported without requiring
a Source-control-only receipt or duplicating Root session/checkpoint authority.
The native proof binds canonical artifact bytes to actual loaded source-metadata bytes and observes
the production stateless treasure-urn lookup repair in a fresh, genuine native `LevelChunk` that is
never registered in the world's cache. Setup uses only that chunk's owned native section palette/counters,
not `LevelChunk.setBlockState`, whose native implementation would enqueue actual-level lighting work
and invoke world callbacks even on an unregistered chunk. Actual production `LevelChunk.getBlockEntity`
with `CHECK` returns null and retains its minimal synthetic marker throughout the control; no cached
world cell is displaced, no pending tag is purged and no fake tile or cleanup-warning exemption is
introduced. This is an owned-state lookup control, not a production placement/world-generation proof
or a universal claim of transformed-runtime neutrality beyond the inspected native ABI. The original global block-entity predicate remains true, the native treasure factory
remains null, and genuine authored inventory paths remain native; normal urns still have tiles.
This fixes real protochunk world generation rather than suppressing its failed-promotion warnings;
Crops contains no copy of the repair. Compat owns independent native loot/storage/legacy controls. The activity must already register
this repository's output scope before Gradle starts. The owning `prepareCanonicalDataSnapshot`
task prepares a fresh immutable `.native-fixture-inputs/<native UUID>/` from approved Root and
hash-pinned raw archives, before any own native preparation; no previous conversation snapshot
is needed. Missing bindings fail clearly, never disable required data. The ten ordinary resources
stay byte-identical; the reviewed fixed72 global index is separately labelled TRANSFORMED.
Exact production Create and the data/load-proof fixture attach only to actual `gameTestServer`,
not production/client/server or runtime/source archives. `-PcropsCanonicalDataManifest` remains
an explicit immutable-input override with the same UUID/byte checks, not an ordinary prerequisite.
Source checks, cached compilation and synthetic controls do not qualify actual fixture loading.
The required Ars proof observes Forge's original root `Codec.parse` call at the native
`LootModifierManager.apply` call site: it invokes the same public receiver once and returns the
identical result, preserving original exceptions. This avoids relying on transformation of DFU
library classes already loaded before mod Mixins. The registered Ars decoder return, exact input,
body fields, row order and complete loaded-map checks remain required; forwarding/negative unit
controls do not replace the native proof.
Every one of the fixed72 rows additionally needs an original registered decoder selection, successful
root return, canonical input digest and matching loaded implementation/order. Goety `add_loot`, like
Ars, has a nonmemoized codec supplier; comparing a newly requested instance getter to its registered
codec is not valid dispatch evidence. Its original-call witness is required instead. Genuinely
memoized types retain additional direct identity checks. Missing/duplicate/partial witnesses,
changed inputs and mismatched loaded rows fail closed.
The native-only pack-selection hook admits the required fixed TOP pack last before Minecraft's
original repository rebuild. Automatic BUILT_IN selection otherwise preserves its earlier position
and bypasses default TOP insertion. Every other native pack and its relative order are retained;
there is no second resource reload, decoder invocation or status forcing. Actual selected TOP,
effective resource hashes, original dispatch and complete loaded-map observations remain required.

The reobfuscated runtime artifact is `build/libs/better-bumblezone-crops-0.1.0.jar`. Local verification does not authorize deployment or pack tests. Generated build, cache, and runtime data stay untracked.

GameTest configuration atomically admits a unique retained directory under `run-gametest/attempts/<UUID>`; Forge launch preparation and the structure-copy task use that same directory. Launch refuses an existing `world` or a changed working directory, rather than clearing/recycling a saved fixture. The admitted attempt and world paths appear in the verification log. All attempts, failed evidence, and the historical `run-gametest/world` remain retained outside Gradle's cleanable `build/` tree; do not delete them to obtain a green gate. Native food-policy fixture diagnostics distinguish actual planting preconditions from the cancellation assertion; they are not full-pack acceptance.

Licensed under GPL-3.0-or-later; see [LICENSE](LICENSE). Contribution and validation requirements are in [AGENTS.md](AGENTS.md).

Imported-cultivar discovery evidence records Bumblezone seed origin on the harvested seed stack, then saves the planter and planting identity with the Overworld crop position. Only successfully spawned produce from that mature crop emits `CultivarHarvestEvent`; loot previews, disabled drops, untagged seeds, and repeated delivery cannot award it. This currently covers the native block-drop harvest path, including automatic block harvesting; right-click-only harvest adapters do not fabricate an equivalent outcome.

An uprooted, mature Overworld crop with that recorded lineage returns exactly one origin-marked seed, including in later planting generations. Wild or unrecorded Overworld crops return no seed. Persistent-harvest plants do not create off-origin seed on harvest. Decorative flowers instead relocate freely and never yield seeds off-origin; their multiplication belongs only in Bumblezone.

Farmer's Delight's seven wild crops are finite, food-only forage: carrot, potato, beetroot, cabbage, onion, tomato and rice. The pack owns their broad new-chunk placement and native edible-yield loot tables; this mod filters wild loot to the corresponding edible product only, rejects transplantable wild plants and direct planting of edible carrot/potato/onion/rice at `BlockItem.place`, and blocks native bone-meal multiplication (including direct growth-machine calls). Imported cultivated crops keep their separate lineage and propagation rules. The `better_bumblezone_crops:overworld_forage` placement filter excludes dimensions that reuse Overworld biomes; biome tags alone are not a dimension check.

When a real player actually breaks one of these Overworld wild plants and its edible item spawns, `WildVegetableDropEvent` identifies that seedless drop for contextual teaching. The historical event name/signature is retained, including for wild rice. Loot previews, disabled drops and FakePlayers do not publish it. Wild rice retains its native paired-half loot conditions rather than adding a second drop decision.

Kelp makes its propagation roll only at the `minecraft:kelp` head: `kelp_plant` body segments never receive an immature fallback seed, so player and automated harvest order cannot multiply a single column's propagation. Ordinary melon and pumpkin stems use their real age; attached stems are mature because they have fruit. New Bumblezone pollen nurseries select their cultivar from the world seed, dimension, and pollen-site position with a fixed salt. Existing finalized chunks are retained unchanged.

`verifyFull` includes native wild-forage GameTests using Farmer's Delight file 8007609 (1.20.1-1.3.1, matching the pack pin): food-only loot with ordinary/shears/Fortune/Silk Touch tools, direct bone-meal multiplication denial, real-player versus FakePlayer/preview/disabled-drop discovery, item-placement rejection and paired rice consumption. Source-mod tests use native Farmer's Delight loot plus the mod's allowlist; the pack's edible shears tables and actual fresh-chunk density require separate pack acceptance. The Minecraft-free pack contract is `python3 -B scripts/test_wild_crop_forage.py` from the modpack.

## Decorative flowers

`defaults/cultivars.json` is the explicit decorative-flower inventory, including reviewed untagged decorations. Its `decorative_flowers` block tag is shared with Better Compat Fixes; the native `minecraft:flowers` tag alone is unsuitable because it also contains wild food crops and tree foliage. `defaults/flower_tag_exclusions.json` records those nonflower exceptions, including fallen tree litter.

Ordinary flower items retain native placement and harvest rules. Flower seeds consume one seed to place one native flower unit in any dimension; legacy seeds remain usable. Water-surface seeds also support fluid raycasting. Quark's itemless water petals use recovered pink petals as their native placement unit; Helvola seeds start one bloom, not a bare pad or a full cluster. Native placement supplies support, orientation, water, multiblock and petal-quantity checks rather than installing a fully populated cluster from a seed. The pack's separate general offhand BlockItem restriction still applies to ordinary flower BlockItems; the cultivar crop blocker itself exempts flowers in either hand.

Only `the_bumblezone:the_bumblezone` grants flower-harvest propagation: one seed with a 50% chance of a second **per recovered native flower unit**. Upper tall halves do not get another roll, and no recovered flower means no seed bonus. Provenance is not required: transplanted wild flowers and rapid place/break farming qualify there. Outside Bumblezone no flower seed bonus is possible. Native flower-item loot remains unchanged.

Dimension-aware hooks cover native tall flowers/petals (including inherited overrides), grass bonemeal's flower branch, reviewed Nature's Spirit/Goety/Deep Void direct bonemeal overrides, lotus-stem flower regrowth (random and fertilized), Sniffer-created flower planting resources, Ars ritual-generated flower placements, and net flower creation by native shaped/shapeless recipes. The grass branch keeps unrelated grass growth; Ars keeps nonflower placements; one-for-one flower conversions remain possible. Forge bonemeal admission is owned by Better Compat Fixes. None of these hooks intercepts arbitrary world block placement or natural generation. No worlds, flowers, seed stacks or lineage records are migrated or removed.

The SOURCE SDK declares the exact production BOTH pins for all18 namespaces required by the
unconditional origin/seeds tags, plus existing Bumblezone and seven mandatory transitives
(Blueprint, Citadel, Curios, GeckoLib, Moonlight, TerraBlender, Zeta):26 base third-party providers.
Production Create with its own JarJar graph and the two canonical custom providers above attach
additionally only to the native fixture; they add no shipped Crops dependency or optional acceptance lane.
`gradle/native-provider-closure.gradle` adds only the missing24; Farmer's Delight and the existing
Bumblezone remap stay unchanged. No shipped mod dependency, tag ID, required flag or11-test coverage
is reduced. Both latest/DEBUG logs reject ERROR/FATAL and actual loot-modifier failed WARNs.
Pinned metadata/range inspection is not Gradle/native acceptance; Goety's raw unexpanded version
placeholders are not patched or treated as meaningful upstream compatibility bounds.

The read-only `python3 -B scripts/check-flower-contract.py` checks source resources and **every active hash-pinned Packwiz mod archive** against the sibling pack (override `--pack-root` and `--native-cache` explicitly if needed). Missing or mismatched inputs fail; it neither fetches nor uses historical registry dumps. It checks catalogue/tag/model/tint agreement, upstream flower-tag coverage with reasoned exclusions, explicit untagged flowers, flower-producing recipe coverage, and pinned optional native bonemeal symbols. Prepare pinned inputs separately when absent. `FlowerPropagationPolicyTest`, `FlowerGameTests`, and the existing tall-flower GameTest cover probability boundaries, actual off-origin flower loot, relocation, seed quantity, direct native bonemeal denial and installed tag coverage when their documented gate is authorized. These checks are not evidence that an unbuilt candidate works in the pack. Full modded aquatic/multiblock interactions, other installed generator adapters and same-artifact pack acceptance must still be exercised through an explicitly authorized runtime run.

`verifyFull` also includes a Forge GameTest using the pinned Bumblezone 7.13.4 dependency and vanilla wheat. It checks actual produce spawning, loot-preview rejection, disabled drops, author persistence, and duplicate rejection. Bumblezone's inline SRG mixin targets need an official-name development adaptation: `scripts/remap-test-mixin-strings.py` writes only `build/development-dependencies/bumblezone-mapped.jar`. The upstream dependency and deployable Cultivars JAR remain separate and unchanged by that development remap.
