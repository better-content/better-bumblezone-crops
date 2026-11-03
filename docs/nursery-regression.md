# Nursery regression

The owning repository has a focused, nonshipping `nurseryGameTest` source set. To run its native regression in a fresh retained world with Java 17:

```sh
TMPDIR="$HOME/.tmp" ./gradlew runNurseryGameTestServer --max-workers=2 --console=plain
```

The task selects the nursery namespace, checks production runtime and source archives for fixture leakage, and requires one completed GameTest without ERROR or FATAL records. Attempts and logs remain under `run-nursery-gametest/attempts/<UUID>/`; retain them as evidence. The test calls `CultivarChunkFinalizer.onChunkLoad` on unpublished chunks and checks old-chunk and other-dimension rejection, bounded planting, matching rewards, and repeat-event behavior. It uses Forge, Bumblezone, and Farmer's Delight from this repository's declared development dependencies. It does not exercise the complete canonical pack data or other native tests.

`verifyFull` and `runGameTestServer` select the separate canonical native profile. The build rejects combining those tasks with `-PnurseryRegression=true`. `verifyFast` runs deterministic checks. These are available owner commands; this note records no execution result.
