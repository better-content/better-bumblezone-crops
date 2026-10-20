#!/usr/bin/env python3
"""Read-only flower resources and active, hash-pinned pack input contracts; never launches Minecraft."""
import argparse
import hashlib
import json
from pathlib import Path
import tomllib
from zipfile import ZipFile


def check(pack, cache):
    root = Path(__file__).resolve().parents[1]
    resources = root / 'src/main/resources'
    catalog = json.loads((resources / 'defaults/cultivars.json').read_text())
    flowers = [c for c in catalog if c['growthForm'] == 'flower']
    plants = {p for c in flowers for p in c['plantBlocks']}
    produce = {p for c in flowers for p in c['produce']}
    exclusions = json.loads((resources / 'defaults/flower_tag_exclusions.json').read_text())
    assert not plants.intersection(exclusions), 'flower is also excluded'
    assert all(exclusions.values()), 'exclusions need reasons'
    def tag(path):
        values = json.loads((resources / path).read_text())['values']
        return {v if isinstance(v, str) else v['id'] for v in values}
    assert plants == tag('data/better_bumblezone_crops/tags/blocks/decorative_flowers.json')
    assert {c['seedItem'] for c in catalog} == tag('data/better_bumblezone_crops/tags/items/seeds.json')
    assert {p for c in catalog for p in c['plantBlocks']} == tag('data/better_bumblezone_crops/tags/blocks/origin/bumblezone.json')
    dedicated = {c['seedItem'].split(':', 1)[1] for c in catalog if c['seedItem'].startswith('better_bumblezone_crops:')}
    assets = resources / 'assets/better_bumblezone_crops'
    tints = json.loads((assets / 'seed_tints.json').read_text())
    assert dedicated == tints.keys()
    assert len(tints.values()) == len(set(tints.values())), 'duplicate seed tint'
    for seed in dedicated:
        model = json.loads((assets / 'models/item' / (seed + '.json')).read_text())
        assert model['textures']['layer0'] == 'better_bumblezone_crops:item/cultivar_seed'
    assert all(c['originDimensions'] == ['the_bumblezone:the_bumblezone'] for c in flowers)
    assert all(c['seedItem'] not in c['produce'] for c in flowers)

    # Native block tags are authoritative inputs, not retained registry dumps. Inspect every
    # active Packwiz pin; disabled embedded datapacks (e.g. Ratlantis) are intentionally not loaded.
    tags = {}
    recipes = []
    pinned = {}
    for pinfile in sorted((pack / 'mods').glob('*.pw.toml')):
        pin = tomllib.loads(pinfile.read_text())
        jar = cache / pin['filename']
        assert jar.is_file(), 'Missing pinned native input: ' + str(jar)
        digest = hashlib.new(pin['download']['hash-format'], jar.read_bytes()).hexdigest()
        assert digest == pin['download']['hash'], 'Native input does not match active pin: ' + str(jar)
        with ZipFile(jar) as archive:
            pinned[pin['filename']] = archive.namelist()
            for name in archive.namelist():
                parts = name.split('/')
                if len(parts) < 4 or parts[0] != 'data' or parts[2] == 'datapacks' or '/datapacks/' in name:
                    continue
                if '/tags/blocks/' in name and name.endswith('.json'):
                    tag_id = parts[1] + ':' + name.split('/tags/blocks/', 1)[1][:-5]
                    data = json.loads(archive.read(name))
                    if data.get('replace'):
                        # None of the current native flower tags replace other providers. Fail
                        # rather than guessing pack ordering if a provider changes that contract.
                        assert tag_id not in {'minecraft:flowers', 'minecraft:small_flowers', 'minecraft:tall_flowers'}, name
                    tags.setdefault(tag_id, []).extend(data.get('values', []))
                if '/recipes/' in name and name.endswith('.json'):
                    try:
                        recipes.append((name, json.loads(archive.read(name))))
                    except (ValueError, UnicodeError):
                        pass  # Non-JSON custom loaders are not native JSON recipes.
    def resolve(tag_id, seen):
        assert tag_id not in seen, 'recursive native flower tag: ' + tag_id
        result = set()
        for value in tags.get(tag_id, []):
            value = value if isinstance(value, str) else value['id']
            result.update(resolve(value[1:], seen | {tag_id}) if value.startswith('#') else {value})
        return result
    native = set().union(*(resolve(t, set()) for t in ['minecraft:flowers', 'minecraft:small_flowers', 'minecraft:tall_flowers']))
    assert not native - plants - exclusions.keys(), 'Unclassified pinned flower tags: ' + str(sorted(native - plants - exclusions.keys()))

    # Explicit source-reviewed, untagged decorations. A catalogue removal cannot silently
    # shrink coverage merely because upstream did not tag the plant.
    untagged = {'minecraft:spore_blossom', 'hexerei:flowering_lily_pad', 'ars_elemental:spore_blossom_up',
                'the_deep_void:corpse_blossom', 'the_deep_void:pus_flower', 'the_deep_void:void_blossom', 'the_deep_void:void_rose',
                'goety:corpse_blossom', 'twilightforest:huge_water_lily', 'projectvibrantjourneys:pink_lotus',
                'natures_spirit:helvola', 'eanimod:growable_cornflower'}
    untagged.update('projectvibrantjourneys:' + color + '_wildflowers' for color in ['blue', 'mixed', 'orange', 'purple', 'white', 'yellow'])
    assert untagged <= plants, 'Source-reviewed decorative flower disappeared from catalogue'

    def strings(value):
        if isinstance(value, str):
            return {value}
        if isinstance(value, dict):
            return set().union(*(strings(v) for v in value.values())) if value else set()
        if isinstance(value, list):
            return set().union(*(strings(v) for v in value)) if value else set()
        return set()
    for name, recipe in recipes:
        results = set().union(*(strings(recipe.get(key)) for key in ['result', 'results', 'output', 'outputs']))
        flower_outputs = results & produce
        if not flower_outputs:
            continue
        kind = recipe.get('type')
        if kind in {'minecraft:crafting_shaped', 'minecraft:crafting_shapeless'}:
            continue  # FlowerCraftingMixin gates net creation before matching succeeds.
        if kind == 'goety:cursed_infuser_recipes' and flower_outputs == {'goety:corpse_blossom'}:
            assert recipe['ingredient'] == {'item': 'minecraft:spore_blossom'}, name
            continue  # One existing flower converted to one existing flower: no multiplication.
        raise AssertionError('Flower-producing recipe needs an explicit owning dimension policy: ' + name)

    # Bytecode symbol contracts for each optional direct-native bonemeal override. This is
    # input verification, not evidence that mixins applied or that runtime behavior passed.
    expected = {
        'natures_spirit-2.2.5-1.20.1.jar': ['net/hibiscus/naturespirit/blocks/LotusFlowerBlock.class', 'net/hibiscus/naturespirit/blocks/WaterFlowerbedBlock.class'],
        'the_deep_void-1.98.1-forge-1.20.1.jar': ['net/mcreator/thedeepvoid/block/VoidBlossomBlock.class'],
        'goety-2.5.53.1.jar': ['com/Polarice3/Goety/common/blocks/CorpseBlossomBlock.class'],
    }
    mixin = (root / 'src/main/java/com/bettercontent/betterbumblezonecrops/mixin/ModFlowerBonemealMixin.java').read_text()
    for filename, classes in expected.items():
        assert filename in pinned, 'Re-review optional flower hooks for changed pin: ' + filename
        with ZipFile(cache / filename) as archive:
            for cls in classes:
                assert b'm_214148_' in archive.read(cls), 'Native bonemeal override changed: ' + cls
                assert cls[:-6].replace('/', '.') in mixin
    configured = set(json.loads((resources / 'better_bumblezone_crops.mixins.json').read_text())['mixins'])
    hooks = {'FlowerGrassBonemealMixin', 'NativeFlowerBonemealMixin', 'ModFlowerBonemealMixin',
             'LotusFlowerGrowthMixin', 'ArsFlowerPlacementMixin', 'FlowerCraftingMixin'}
    assert hooks <= configured, 'Known flower route hook missing from mixin config'
    sources = root / 'src/main/java/com/bettercontent/betterbumblezonecrops'
    for hook in hooks:
        assert (sources / 'mixin' / (hook + '.java')).is_file(), 'Configured hook lacks source: ' + hook
    with ZipFile(cache / 'natures_spirit-2.2.5-1.20.1.jar') as archive:
        stem = archive.read('net/hibiscus/naturespirit/blocks/LotusStemBlock.class')
        assert b'grow' in stem and b'm_213898_' in stem, 'Re-review lotus growth hooks for changed native methods'
    ars_pin = next(name for name in pinned if name.startswith('ars_nouveau-'))
    with ZipFile(cache / ars_pin) as archive:
        placement = archive.read('com/hollingsworth/arsnouveau/api/ritual/features/PlaceBlockFeature.class')
        assert b'onPlace' in placement and b'm_40576_' in placement, 'Re-review scoped Ars flower placement hook'
    assert 'instanceof net.minecraft.world.entity.animal.sniffer.Sniffer' in (sources / 'CultivarLootModifier.java').read_text()
    compat = root.parent / 'better-compat-fixes'
    classification = (compat / 'src/main/java/com/bettercontent/bettercompatfixes/compat/DecorativeFlowerPolicy.java').read_text()
    offhand = (compat / 'src/main/java/com/bettercontent/bettercompatfixes/compat/OffhandBlockPlacement.java').read_text()
    assert 'new ResourceLocation("better_bumblezone_crops", "decorative_flowers")' in classification
    assert 'BlockTags.FLOWERS' not in classification
    assert '!DecorativeFlowerPolicy.isDecorative(blockItem.getBlock().defaultBlockState())' in offhand
    assert 'InteractionHand.OFF_HAND' in offhand and 'Event.Result.DENY' in offhand
    assert 'Event.Result.ALLOW' not in offhand, 'Flower exception must not override other denials'
    # The standalone Compat GameTests use a declared, nonshipping representative subset.
    # Check it against the actual canonical classification, never against a cached registry dump.
    fixture = set(json.loads((compat / 'src/nativeFixture/resources/data/better_bumblezone_crops/tags/blocks/decorative_flowers.json').read_text())['values'])
    assert fixture <= plants and not fixture.intersection(exclusions)
    assert {'minecraft:dandelion', 'minecraft:sunflower', 'minecraft:pink_petals', 'minecraft:spore_blossom'} <= fixture
    assert not (compat / 'src/main/resources/data/better_bumblezone_crops/tags/blocks/decorative_flowers.json').exists(), 'Duplicate production classification in Compat'
    print(f'Flower source/resource contracts passed: {len(flowers)} definitions; {len(pinned)} hash-pinned native inputs. No build or runtime acceptance.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--pack-root', type=Path, default=Path(__file__).resolve().parents[3] / 'better-content-modpack')
    parser.add_argument('--native-cache', type=Path, default=Path.home() / '.cache/bc/packwiz-downloads/mods')
    args = parser.parse_args()
    check(args.pack_root, args.native_cache)
