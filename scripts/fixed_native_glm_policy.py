"""Reviewed FIXED projection, not a generic scope/filtering engine. No task-artifact dependency."""
from contextlib import ExitStack
import hashlib
import io
import json
from pathlib import Path
import re
import zipfile

REPO = Path(__file__).resolve().parents[1]
POLICY = REPO/'gradle/native-canonical-glm-policy.json'
INDEX = 'data/forge/loot_modifiers/global_loot_modifiers.json'
IN = 'IN_DOMAIN_DEFINITION_PRESENT'
OUT = 'OUTSIDE_DOMAIN_RESOURCE_EXISTS_ONLY_IN_UNSELECTED_PRODUCTION_PROVIDER'
sha = lambda b: hashlib.sha256(b).hexdigest()
ROOT_DATA_OWNER_VERSION = 'bc.crops.root_data_owner.v1'
ROOT_DATA_OWNERS = frozenset({
    'root://mods/better-chemlib-hazards-0.2.0.jar',
    'root://mods/better-wild-fruit-trees-0.1.1.jar',
    'root://mods/better-bumblezone-crops-0.1.0.jar',
})
OWNER_METADATA = ('META-INF/mods.toml', 'pack.mcmeta')


def require(value, message):
    if not value:
        raise ValueError(message)


class Inputs:
    def __init__(self, root, archives):
        self.root, self.archives = Path(root), Path(archives)
        self.stack = ExitStack()
        self.zips, self.hashes, self.data_owners = {}, {}, {}

    def close(self):
        self.stack.close()

    def path(self, locator):
        for prefix, base in [('root://', self.root), ('source://', REPO), ('archive://', self.archives)]:
            if locator.startswith(prefix):
                relative = locator[len(prefix):]
                require(not Path(relative).is_absolute() and '..' not in Path(relative).parts, 'Invalid owned input locator')
                p = base/relative
                for parent in [p, *p.parents]:
                    require(not parent.is_symlink(), 'Symlink policy input ancestry')
                return p
        raise ValueError('Unknown explicit input locator: '+locator)

    def archive(self, owner):
        if owner not in self.zips:
            parts = owner.split('::')
            if len(parts) == 1:
                z = zipfile.ZipFile(self.path(owner))
            else:
                parent, entry = owner.rsplit('::', 1)
                z = zipfile.ZipFile(io.BytesIO(self.archive(parent).read(entry)))
            self.zips[owner] = self.stack.enter_context(z)
        return self.zips[owner]

    def owner_sha(self, owner):
        if owner not in self.hashes:
            if '::' in owner:
                parent, entry = owner.rsplit('::', 1)
                value = sha(self.archive(parent).read(entry))
            else:
                h = hashlib.sha256()
                with self.path(owner).open('rb') as f:
                    for chunk in iter(lambda: f.read(1024*1024), b''):
                        h.update(chunk)
                value = h.hexdigest()
            self.hashes[owner] = value
        return self.hashes[owner]

    def resource(self, owner, resource):
        z = self.archive(owner)
        require(z.namelist().count(resource) <= 1, 'Duplicate archive resource')
        if resource not in z.namelist():
            return None
        return z.read(resource)

    def definition(self, owner, resource):
        return self.resource(owner, resource) if owner.split('::')[-1].endswith('.jar') else self.path(owner).read_bytes()

    def data_owner(self, owner):
        require(owner in ROOT_DATA_OWNERS, 'Data-only identity is restricted to the exact three Root custom owners')
        if owner not in self.data_owners:
            self.data_owners[owner] = root_data_owner_identity(self.archive(owner))
        return self.data_owners[owner]


def root_data_owner_identity(archive):
    """ALL data payloads + exact owner metadata, including explicit metadata absence.

    Directory records, classes, ZIP bookkeeping and source provenance are not data.
    No resource-existence/codec/row filter is used to construct this identity.
    """
    searched = [i for i in archive.infolist()
                if i.filename.startswith('data/') or i.filename in OWNER_METADATA]
    names = [i.filename for i in searched]
    require(len(names) == len(set(names)), 'Duplicate searched data/owner metadata name')
    require(all(i.orig_filename == i.filename for i in searched), 'Truncated searched archive name')
    data = {i.filename: sha(archive.read(i)) for i in sorted(searched, key=lambda i: i.filename)
            if i.filename.startswith('data/') and not i.is_dir()}
    metadata = {name: sha(archive.read(name)) if name in names else None for name in OWNER_METADATA}
    require(not any(i.is_dir() for i in searched if i.filename in OWNER_METADATA), 'Owner metadata must be a file')
    return dict(version=ROOT_DATA_OWNER_VERSION, data=data, metadata=metadata)


def validate_root_data_owners(policy):
    require(policy.get('schema') == 'bc.crops_fixed_glm_projection.source.v2', 'Fixed data policy version changed')
    owners = policy.get('root_data_owner_inputs')
    require(isinstance(owners, dict) and set(owners) == ROOT_DATA_OWNERS,
            'Exactly three frozen Root custom data-owner identities are mandatory')
    referenced = {o['origin'] for row in policy['root_rows'] for o in row['production_resource_owners']}
    require(ROOT_DATA_OWNERS <= referenced, 'Frozen Root data owner is not referenced by the reviewed rows')
    for identity in owners.values():
        require(isinstance(identity, dict) and set(identity) == {'version', 'data', 'metadata'} and
                identity['version'] == ROOT_DATA_OWNER_VERSION, 'Invalid Root data-owner identity version/shape')
        data, metadata = identity['data'], identity['metadata']
        require(isinstance(data, dict) and data and list(data) == sorted(data), 'Complete sorted data identity required')
        require(isinstance(metadata, dict) and set(metadata) == set(OWNER_METADATA), 'Explicit owner metadata presence/absence required')
        require(all(isinstance(name, str) and name.startswith('data/') and not name.endswith('/') and
                    '\\' not in name and all(p not in ('', '.', '..') for p in name.split('/'))
                    for name in data), 'Unsafe/non-data identity name')
        require(all(isinstance(value, str) and re.fullmatch('[0-9a-f]{64}', value) for value in data.values()),
                'Invalid data identity hash')
        require(all(value is None or isinstance(value, str) and re.fullmatch('[0-9a-f]{64}', value)
                    for value in metadata.values()), 'Invalid owner metadata hash/absence')


def verify_production_owner(inputs, owner, policy):
    origin = owner['origin']
    if origin in ROOT_DATA_OWNERS:
        require(inputs.data_owner(origin) == policy['root_data_owner_inputs'][origin],
                'Actual complete Root data-owner resources/metadata changed; explicit review required')
    else:
        # Every third-party/SDK/unknown owner retains the original whole-archive gate.
        require(inputs.owner_sha(origin) == owner['archive_sha256'], 'Actual data owner archive changed')


def validate_model(policy):
    validate_root_data_owners(policy)
    rows = policy['root_rows']
    ids = [r['id'] for r in rows]
    require(len(rows) == len(set(ids)) == 226 and [r['root_order'] for r in rows] == list(range(226)), 'All226 ordered unique row decisions are mandatory')
    require(all(r['decision'] in (IN, OUT) for r in rows), 'Unresolved fixed row decision')
    selected = [r['id'] for r in rows if r['decision'] == IN]
    require(len(selected) == 72 and selected == policy['proposed_ordered_in_domain_ids'], 'Mandatory72 drop/extra/reorder')
    raw = policy['raw_sdk_ids']
    require(len(raw) == len(set(raw)) == 77 and set(raw) == {r['id'] for r in policy['raw_sdk_rows']}, 'Raw77 closure drift')
    exclusions = [r['id'] for r in policy['every_raw_sdk_id_excluded_by_root']]
    require(len(exclusions) == 5 and set(exclusions) == set(raw)-set(ids), 'Raw five Root exclusions changed/reintroduced')
    require(len(policy['selected_sdk_parent_hashes']) == 53, 'Fixed53 selected archive graph drift')
    for r in rows:
        if r['decision'] == IN:
            require(r['effective_selected_definition'] is not None and r['primary_codec'], 'In-scope missing definition/codec cannot be dropped')
        else:
            require(r['production_resource_owners'] and not r['selected_source_definition'], 'Unproved outside source ownership')
            require(not any(o['selected_sdk_candidate'] for o in r['production_resource_owners']), 'Outside row owned by selected SDK')
    return selected


def verify(root, archives, policy=None):
    """Re-open actual parents/archives/definitions, never accept an output digest as closure proof."""
    policy = policy if policy is not None else json.loads(POLICY.read_bytes())
    selected_ids = validate_model(policy)
    inputs = Inputs(root, archives)
    try:
        parent = inputs.path(policy['canonical_parent']['path']).read_bytes()
        require(sha(parent) == policy['canonical_parent']['sha256'], 'Canonical Root parent changed; review required')
        actual_definitions = {p.relative_to(inputs.root).as_posix(): sha(p.read_bytes())
            for p in sorted((inputs.root/'kubejs/data').glob('*/loot_modifiers/**/*.json'))
            if p.relative_to(inputs.root).as_posix() != 'kubejs/'+INDEX}
        require(actual_definitions == policy['canonical_definition_inputs'],
            'New/missing/changed canonical definition requires reviewed ownership, even when not raw-indexed')
        canonical = json.loads(parent)
        require(canonical['replace'] is True and canonical['entries'] == [r['id'] for r in policy['root_rows']], 'Full226 canonical order/decisions changed')
        for p in [*policy['production_pin_inputs'], *policy['sdk_declaration_inputs']]:
            require(sha(inputs.path(p['path']).read_bytes()) == p['sha256'], 'Actual pin/SDK declaration changed')
        sdk = {s['origin'] for s in policy['selected_sdk_parent_hashes']}
        actual_raw = []
        for s in policy['selected_sdk_parent_hashes']:
            require(inputs.owner_sha(s['origin']) == s['sha256'], 'Different/missing actual SDK archive')
            raw = inputs.resource(s['origin'], INDEX)
            require((sha(raw) if raw is not None else None) == s['index_sha256'], 'Actual raw index missing/stale/extra')
            if raw is not None:
                actual_raw += [(s['origin'], i, mid) for i, mid in enumerate(json.loads(raw)['entries'])]
        own = policy['own_source_index']
        raw = inputs.path(own['path']).read_bytes()
        require(sha(raw) == own['sha256'], 'Current owning source index changed')
        actual_raw += [(own['path'], i, mid) for i, mid in enumerate(json.loads(raw)['entries'])]
        expected_raw = [(r['owner'], r['order_in_owner_index'], r['id']) for r in policy['raw_sdk_rows']]
        require(sorted(actual_raw) == sorted(expected_raw), 'Raw77 exact owner/order/ID rows changed')
        resolved = []
        for row in policy['root_rows']:
            resource = row['resource']
            for owner in row['production_resource_owners']:
                verify_production_owner(inputs, owner, policy)
                require((owner['origin'] in sdk) == owner['selected_sdk_candidate'], 'Fixed provider/SDK membership mismatch')
                definition = inputs.resource(owner['origin'], resource)
                require(definition is not None and sha(definition) == owner['sha256'], 'Actual resource-owner definition absent/stale')
                require(json.loads(definition) == owner['json'], 'Resource-owner JSON model differs')
            source = row['selected_source_definition']
            if source:
                require(sha(inputs.path(source['path']).read_bytes()) == source['sha256'], 'Current source definition missing/stale')
            override = row['root_override']
            if override:
                require(sha(inputs.path(override['path']).read_bytes()) == override['sha256'], 'Actual Root override changed')
            if row['decision'] == IN:
                value = row['effective_selected_definition']
                definition = inputs.definition(value['origin'], resource)
                require(definition is not None and sha(definition) == value['sha256'], 'MANDATORY in-scope definition missing/stale')
                decoded = json.loads(definition)
                require(decoded == value['json'] and decoded['type'] == row['primary_codec'], 'MANDATORY codec declaration changed/absent')
                if value['origin'].startswith('root://'):
                    owner = dict(kind='canonical_factory', group='bc_crops_canonical_source', pack_id='bc_crops_canonical_source')
                elif value['origin'].startswith('source://'):
                    owner = dict(kind='source_mod', group='source://better_bumblezone_crops', owner_mod_ids=['better_bumblezone_crops'])
                else:
                    import tomllib
                    metadata = inputs.resource(value['origin'], 'META-INF/mods.toml')
                    require(metadata is not None, 'Selected data owner lacks actual mod pack metadata')
                    ids = [m['modId'] for m in tomllib.loads(metadata.decode())['mods']]
                    require(0 < len(ids) <= 72 and all(isinstance(mid, str) and 0 < len(mid) <= 64 for mid in ids)
                            and len(set(ids)) == len(ids), 'Missing/ambiguous selected logical owner keys')
                    owner = dict(kind='archive_mod', group=value['origin'], owner_mod_ids=ids)
                resolved.append(dict(id=row['id'], resource=resource, sha256=value['sha256'], codec=row['primary_codec'],
                                     origin=value['origin'], expected_owner=owner))
        require([r['id'] for r in resolved] == selected_ids, 'MANDATORY row lost during verification')
        index = (json.dumps(dict(replace=True, entries=selected_ids), indent=2)+'\n').encode()
        return dict(recipe=policy['recipe'], owner_projection_version='bc.crops.logical_data_owner.v1',
                    root_data_owner_identity_version=ROOT_DATA_OWNER_VERSION,
                    frozen_manifest_sha256=sha(POLICY.read_bytes()), canonical_parent_sha256=sha(parent),
                    transformed=True, ordered_ids=selected_ids, index_sha256=sha(index), resources=resolved), index, parent
    finally:
        inputs.close()
