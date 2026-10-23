"""Owning fixture exclusion only, not a package/content/native acceptance verdict."""
import json
import hashlib
from pathlib import Path
import sys
import zipfile

SCOPE = Path(__file__).resolve().parents[1]/'gradle/native-canonical-data-files.json'


def check(path):
    scoped = set(json.loads(SCOPE.read_text())['resources'])
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        forbidden = [n for n in names if '/nativefixture/' in n or '.native-fixture-inputs/' in n
                     or n in ('bc_crops_canonical_source_scope.json', 'bc_crops_native_glm_policy.json', 'bc_crops_native_canonical.mixins.json') or n in scoped]
        index = 'data/forge/loot_modifiers/global_loot_modifiers.json'
        if index in names:
            original = SCOPE.parents[1]/'src/main/resources'/index
            if hashlib.sha256(z.read(index)).digest() != hashlib.sha256(original.read_bytes()).digest():
                raise ValueError('Shipping transformed Native policy instead of unchanged own production index')
        if forbidden:
            raise ValueError('Shipping canonical fixture inputs/classes/resources: '+repr(forbidden))


if __name__ == '__main__':
    for p in sys.argv[1:]:
        check(p)
    if len(sys.argv) != 3:
        raise SystemExit('Require actual staged runtime AND sources archives')
    print('Actual runtime AND sources archive fixture exclusions passed')
