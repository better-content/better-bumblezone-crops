# Source-native nursery regression

Run this focused owning-repository regression without a pack checkout, canonical
snapshot, custom-mod provider directory, or retired Wild Fruit dependency:

```sh
JAVA_HOME=$HOME/.sdkman/candidates/java/17.0.17-tem TMPDIR=$HOME/.tmp \
  ./gradlew runNurseryGameTestServer --max-workers=2 --console=plain
```

This explicitly selects the nonshipping `nurseryGameTest` source set and namespace.
The declared base SDK is Forge + Bumblezone (the existing development remap) and
Farmer's Delight. No custom-mod provider or full canonical data fixture is loaded.
A fresh UUID directory under `run-nursery-gametest/attempts/` retains each actual
world and native logs; existing worlds/prepared attempts are never adopted or
removed. Success requires exactly one required GameTest, no native ERROR/FATAL
records, and runtime/source archive inspection proving fixture code/resources do
not ship. Do not delete those retained attempts with normal generated build files.

The test invokes the production `CultivarChunkFinalizer.onChunkLoad` on unpublished
native chunks in the real Bumblezone server dimension. It checks old-chunk no-op,
24-placement bound with abundant pollen sites, initialized cultivar-matching four
planting-item rewards with origin, repeated-new-event no-op, and other-dimension
rejection. Detached chunks model the unpublished load-event boundary without
scanning/backfilling real terrain or requesting the loading chunk recursively.

The fixture replaces only this mod's four unrelated optional catalogue tags with
empty owned tag data. Nursery selection uses the production resolved cultivar
registry/catalogue directly, not those tags. This is **not** propagation-tag,
full catalogue, pack worldgen-density, or pack loot-policy coverage.

## Default full canonical profile is unchanged

`verifyFull` / `runGameTestServer` still select the existing default canonical
fixture with its original eleven native tests and all original provider/data/log
guards. Mixing that profile with the nursery profile is rejected. The full fixture
currently rejects the independently changed canonical pack GLM parent against its
frozen projection; this focused regression neither requalifies nor bypasses that
full-profile check, and passing it is not equivalent to passing the full suite.

`-PnurseryRegression=true` explicitly selects this source profile for supporting
compilation/archive tasks. It must not be combined with default full native tasks.
