# Nursery regression

The owning repository has a focused, nonshipping `nurseryGameTest` source set. To run its native regression in a fresh retained world with Java 17:

```sh
TMPDIR="$HOME/.tmp" ./gradlew runNurseryGameTestServer --max-workers=2 --console=plain
```

The task selects the nursery namespace, checks production runtime and source archives for fixture leakage, and requires one completed GameTest without ERROR or FATAL records. Attempts and logs remain under `run-nursery-gametest/attempts/<UUID>/`; retain them as evidence. The test calls `CultivarChunkFinalizer.onChunkLoad` on unpublished chunks and checks old-chunk and other-dimension rejection, bounded planting, matching rewards, and repeat-event behavior. It uses Forge, Bumblezone, and Farmer's Delight from this repository's declared development dependencies. It does not exercise the complete canonical pack data or other native tests.

`verifyFull` and `runGameTestServer` select the separate canonical native profile. The build rejects combining those tasks with `-PnurseryRegression=true`. `verifyFast` runs deterministic checks. These are available owner commands; this note records no execution result.

## Canonical CI inputs

CI requires the nonsecret repository variable `BC_CI_PACK_REF` to contain a reviewed, full 40-character pack commit. The pinned pack's `.github/scripts/prepare-native-ci-inputs.py` validates indexed source/provider identities and exact upstream archive hashes, then registers this invocation's retained input scope. CI does not use a moving branch or silently fall back to historical providers. The same full `verifyFull` gate remains mandatory.

Local callers retain the explicit `BC_CANONICAL_PACK_ROOT`, `BC_CANONICAL_ARCHIVES`, `BC_CANONICAL_ACTIVITY`, `BC_OWNER_SESSION` and `BC_CUSTOM_MOD_JAR_DIR` bindings. `BC_RETENTION_REGISTRY` may name an absolute, non-symlink registry for an isolated invocation; otherwise the existing `/home/dev/.worklane/disposable-tasks.json` is used. Active task, matching owner and admitted destination checks are unchanged.

The reviewed production-input list follows the consolidation's explicit Creating Space/day-length retirement, binds the actual bundled Valkyrien Skies interface patch, and preserves the already-selected `both` side for The One Probe (unchanged upstream bytes). The fixed 226-parent, 77-raw, 72-selected and 53-SDK-archive checks and data/codec assertions remain intact. This input reconciliation is not a native pass or full-pack qualification.
