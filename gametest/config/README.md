# Nonshipping native startup defaults

`the_bumblezone/general.toml` is the complete native-emitted default file for the
pinned Bumblezone **7.13.4**, CurseForge file **8548194**: nine sections and 63
settings. Its SHA-256 is
`cfa31ee7d8122ffe4873ac64b358dafbee6b1b6c361a79ed0fb349e4d86a0757`.
The reviewed bytes came from retained native attempt
`b041c176-2d94-4132-a78d-fcc207f45974` after generation completed.

That attempt exposed NightConfig's watcher reading the initial partial
`["Variant Bee Options"]` header. Preparing the complete file before Forge starts
prevents that first-write race; no watcher, exception, severity or failed-operation
check is disabled. The helper rejects changed seed bytes and any post-launch
rewrite, while preserving already-authored ordinary files byte for byte.

These eleven tests exercise authored crop/flower fixtures and actual loaded
registries/resources, not incidental overworld terrain generation. Fresh native
attempts therefore use complete seed-0 flat settings without structures; the
native canonical-provider observer checks the actual `FlatLevelSource`.
Remaining Minecraft settings retain their defaults. Existing worlds are never
reused, deleted or rewritten.

The flower test now gets its real glowstone neighbor from `flower_lit.snbt`
before execution rather than placing it after the conservation loop. Its runtime
deadline remains **20 ticks**, all twenty relocation checks and both hands remain,
and readiness additionally requires actual block light **14**. The positive
farmland and negative dirt cases still require identical observed native light.
No seed file or setup helper is shipped as production configuration.
