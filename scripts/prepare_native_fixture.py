"""Nonshipping, path-guarded first-launch setup; never filter native diagnostics."""
from pathlib import Path
import argparse
import hashlib
import json
import tomllib
import uuid

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "run-gametest/attempts"
TEMPLATE = ROOT / "gametest/config/the_bumblezone/general.toml"
TEMPLATE_SHA = "cfa31ee7d8122ffe4873ac64b358dafbee6b1b6c361a79ed0fb349e4d86a0757"
FLAT = {
    "layers": [{"block": "minecraft:bedrock", "height": 1},
               {"block": "minecraft:dirt", "height": 2},
               {"block": "minecraft:grass_block", "height": 1}],
    "biome": "minecraft:plains", "lakes": False, "features": False, "structure_overrides": [],
}
PROPERTIES = ("# Nonshipping authored-crop fixture; remaining settings retain Minecraft defaults.\n"
              "level-type=minecraft:flat\nlevel-seed=0\ngenerate-structures=false\n"
              "generator-settings=" + json.dumps(FLAT, separators=(",", ":")) + "\n").encode()


def sha(data):
    return hashlib.sha256(data).hexdigest()


def ordinary(path, *, directory=False):
    path = Path(path).absolute()
    if ".." in path.parts:
        raise ValueError("Unsafe native fixture path")
    for part in (path, *path.parents):
        if part.is_symlink():
            raise ValueError("Symlink native fixture path")
    if path.exists() and not (path.is_dir() if directory else path.is_file()):
        raise ValueError("Unexpected native fixture path type")
    return path


def paths(run, base):
    base = ordinary(base, directory=True)
    run = ordinary(run, directory=True)
    if run.parent != base or str(uuid.UUID(run.name)) != run.name:
        raise ValueError("Native fixture requires an immediate UUID attempt directory")
    return run, ordinary(run / "config/the_bumblezone/general.toml"), ordinary(run / "native-fixture-setup.json")


def seed(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    try:
        with path.open("xb") as output:
            output.write(content)
        return True
    except FileExistsError:
        ordinary(path)
        return False


def prepare(run, base=BASE, template=TEMPLATE):
    run, config, receipt = paths(run, base)
    if ordinary(run / "world", directory=True).exists():
        raise ValueError("Refusing an existing native world")
    content = ordinary(template).read_bytes()
    parsed = tomllib.loads(content.decode())
    if sha(content) != TEMPLATE_SHA or len(parsed) != 9 or sum(map(len, parsed.values())) != 63:
        raise ValueError("Reviewed complete Bumblezone defaults changed")
    if receipt.exists():
        verify(run, base)
        return json.loads(receipt.read_text())
    properties = ordinary(run / "server.properties")
    seeded_properties = seed(properties, PROPERTIES)
    seeded_config = seed(config, content)
    record = {"schema": "bc.crops.native_fixture_setup.v1", "run": str(run),
              "template_sha256": TEMPLATE_SHA, "general_sha256": sha(config.read_bytes()),
              "seeded_properties": seeded_properties, "seeded_general": seeded_config}
    seed(receipt, (json.dumps(record, sort_keys=True, indent=2) + "\n").encode())
    return record


def verify(run, base=BASE):
    run, config, receipt = paths(run, base)
    record = json.loads(receipt.read_text())
    if (record.get("schema") != "bc.crops.native_fixture_setup.v1" or record.get("run") != str(run)
            or record.get("template_sha256") != TEMPLATE_SHA
            or sha(config.read_bytes()) != record.get("general_sha256")):
        raise ValueError("Native Bumblezone config changed after first-launch preparation")
    tomllib.loads(config.read_text())


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("action", choices=("prepare", "verify"))
    parser.add_argument("--run", required=True)
    args = parser.parse_args()
    (prepare if args.action == "prepare" else verify)(Path(args.run))
