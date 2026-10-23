#!/usr/bin/env python3
"""Ten byte-identical ordinary resources + explicitly TRANSFORMED reviewed fixed72 fixture index. No gameplay repair."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tomllib
import uuid
import fixed_native_glm_policy as fixed_glm

REPO = Path(__file__).resolve().parents[1]
SCOPE = REPO/'gradle/native-canonical-data-files.json'
sha = lambda b: hashlib.sha256(b).hexdigest()


def git(root, *args):
    return subprocess.check_output(['git', '-C', str(root), *args], text=True).strip()


def checked_file(path):
    for parent in [path, *path.parents]:
        if parent.is_symlink():
            raise ValueError('Symlink input ancestry')
    if not path.is_file():
        raise ValueError('Missing canonical input: ' + str(path))
    return path.read_bytes()


def verify(manifest):
    data = json.loads(checked_file(manifest))
    scope = json.loads(checked_file(SCOPE))
    assert data['schema'] == 'bc.crops_canonical_snapshot.v2' and data['global_index_projected'] is True
    assert data['scope_sha256'] == sha(SCOPE.read_bytes())
    assert data['pack_id'] == scope['pack_id'] and data['pack_format'] == scope['pack_format']
    assert [r['resource'] for r in data['resources']] == scope['resources']
    assert len(data['resources']) == len(set(scope['resources'])) == 10
    assert INDEX not in scope['resources']
    root = Path(data['root'])
    create = tomllib.loads(checked_file(root/'mods/create.pw.toml').decode())
    assert create['side'] == 'both' and create['update']['curseforge'] == {'file-id': 7178761, 'project-id': 328085}
    pack = manifest.parent/'pack'
    projection, transformed, parent = fixed_glm.verify(root, Path(data['archives']))
    assert projection == data['projection'], 'Recomputed full closure/recipe differs'
    assert checked_file(manifest.parent/'canonical-parent/global_loot_modifiers.json') == parent
    assert checked_file(manifest.parent/'frozen-scope.json') == fixed_glm.POLICY.read_bytes()
    assert checked_file(pack/INDEX) == transformed, 'TRANSFORMED fixed72 index changed'
    files = sorted(p.relative_to(pack).as_posix() for p in pack.rglob('*') if p.is_file())
    assert files == sorted([*scope['resources'], INDEX, 'pack.mcmeta']), 'Extra/missing snapshot resource'
    assert [r['source'] for r in data['context_inputs']] == scope['context_inputs']
    for row in data['context_inputs']:
        assert sha(checked_file(root/row['source'])) == row['sha256']
        assert git(root, 'rev-parse', 'HEAD:'+row['source']) == row['blob']
        assert not git(root, 'status', '--porcelain', '--', row['source'])
    for row in data['resources']:
        relative = 'kubejs/' + row['resource']
        source = root/relative
        assert sha(checked_file(source)) == row['sha256'], 'Root source changed'
        assert git(root, 'rev-parse', 'HEAD:'+relative) == row['blob'], 'Root scoped blob changed'
        assert not git(root, 'status', '--porcelain', '--', relative), 'Root input is dirty/untracked'
        assert sha(checked_file(pack/row['resource'])) == row['sha256'], 'Snapshot changed'
    meta = json.loads(checked_file(pack/'pack.mcmeta'))
    assert meta['pack']['pack_format'] == 15 and meta['pack']['description'] == scope['pack_id']
    return data


INDEX = 'data/forge/loot_modifiers/global_loot_modifiers.json'
if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--verify', type=Path)
    parser.add_argument('--root', type=Path)
    parser.add_argument('--archives', type=Path)
    parser.add_argument('--attempt')
    parser.add_argument('--task')
    parser.add_argument('--owner')
    args = parser.parse_args()
    if args.verify:
        data = verify(args.verify.absolute())
        print('Canonical inputs/snapshot still byte-identical: ' + data['attempt'])
    else:
        if not all((args.root, args.archives, args.attempt, args.task, args.owner)):
            parser.error('Snapshot needs explicit author Root, actual raw SDK archives directory, canonical attempt UUID, active retention task and owner')
        assert str(uuid.UUID(args.attempt)) == args.attempt
        root = args.root.absolute()
        assert root.resolve() == root and git(root, 'rev-parse', '--show-toplevel') == str(root)
        scope_bytes = checked_file(SCOPE)
        scope = json.loads(scope_bytes)
        assert len(scope['resources']) == len(set(scope['resources'])) == 10
        assert INDEX not in scope['resources'], 'Transformed policy is separate from byte-identical ordinary inputs'
        destination = REPO/'.native-fixture-inputs'/args.attempt
        for p in [destination, *destination.parents]:
            assert not p.is_symlink(), 'Snapshot ancestry is a symlink'
        assert not destination.exists(), 'Never adopt an existing snapshot'
        tasks = json.loads(Path('/home/dev/.worklane/disposable-tasks.json').read_text())['tasks']
        task = tasks[args.task]
        assert task['state'] == 'active' and task['owner'] == args.owner
        assert any(destination.is_relative_to(Path(p)) for p in task['paths']), 'Register inputs before creating them'
        head = git(root, 'rev-parse', 'HEAD')
        rows = []
        payloads = []
        for resource in scope['resources']:
            assert resource.startswith('data/') and '..' not in resource.split('/') and resource.endswith('.json')
            relative = 'kubejs/'+resource
            raw = checked_file(root/relative)
            assert len(raw) <= 2 * 1024 * 1024
            assert not git(root, 'status', '--porcelain', '--', relative), 'Dirty/untracked canonical Root resource'
            blob = git(root, 'rev-parse', 'HEAD:'+relative)
            assert subprocess.check_output(['git','-C',str(root),'show','HEAD:'+relative]) == raw
            rows.append(dict(resource=resource, source=relative, blob=blob, sha256=sha(raw)))
            payloads.append(raw)
        context = []
        for relative in scope['context_inputs']:
            raw = checked_file(root/relative)
            assert not git(root, 'status', '--porcelain', '--', relative)
            assert subprocess.check_output(['git','-C',str(root),'show','HEAD:'+relative]) == raw
            context.append(dict(source=relative, blob=git(root, 'rev-parse', 'HEAD:'+relative), sha256=sha(raw)))
        assert git(root, 'rev-parse', 'HEAD') == head, 'Root HEAD changed during snapshot'
        projection, transformed, parent_bytes = fixed_glm.verify(root, args.archives.absolute())
        destination.mkdir(parents=True, exist_ok=False)
        pack = destination/'pack'
        original = destination/'canonical-parent/global_loot_modifiers.json'
        original.parent.mkdir()
        original.write_bytes(parent_bytes)
        (destination/'frozen-scope.json').write_bytes(fixed_glm.POLICY.read_bytes())
        index = pack/INDEX
        index.parent.mkdir(parents=True)
        index.write_bytes(transformed)
        for row, raw in zip(rows, payloads):
            p = pack/row['resource']
            p.parent.mkdir(parents=True, exist_ok=True)
            with p.open('xb') as f:
                f.write(raw)
            p.chmod(0o444)
        (pack/'pack.mcmeta').write_text(json.dumps(dict(pack=dict(pack_format=15, description=scope['pack_id'])))+'\n')
        manifest = destination/'manifest.json'
        manifest.write_text(json.dumps(dict(schema='bc.crops_canonical_snapshot.v2', attempt=args.attempt,
            root=str(root), archives=str(args.archives.absolute()), root_head=head, scope_sha256=sha(scope_bytes), pack_id=scope['pack_id'],
            pack_format=15, resources=rows, context_inputs=context, projection=projection, global_index_projected=True), indent=2)+'\n')
        verify(manifest)
        for p in destination.rglob('*'):
            p.chmod(0o555 if p.is_dir() else 0o444)
        destination.chmod(0o555)
        print(manifest)
