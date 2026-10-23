"""Pure synthetic/source controls; no Native launch, Root edit or acceptance substitution."""
import importlib.util
import copy
import json
from pathlib import Path
import unittest
import uuid
import zipfile

ROOT = Path(__file__).resolve().parents[1]
def load(name):
    spec = importlib.util.spec_from_file_location(name, ROOT/'scripts'/(name+'.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

prepare = load('prepare-native-canonical-data')
archive = load('check-native-canonical-archive')


class CanonicalControls(unittest.TestCase):
    def fixture(self):
        directory = ROOT/'build/native-canonical-controls'/str(uuid.uuid4())
        directory.mkdir(parents=True)
        scope = json.loads(prepare.SCOPE.read_text())
        root = directory/'synthetic-root'
        pack = directory/'pack'
        rows = []
        for name in scope['resources']:
            raw = b'{"synthetic_control_only": true}\n'
            source = root/'kubejs'/name
            copied = pack/name
            for p in (source, copied):
                p.parent.mkdir(parents=True, exist_ok=True)
                p.write_bytes(raw)
            rows.append(dict(resource=name, source='kubejs/'+name, blob='synthetic_blob', sha256=prepare.sha(raw)))
        context = []
        for name in scope['context_inputs']:
            raw = (b'side="both"\n[update.curseforge]\nfile-id=7178761\nproject-id=328085\n' if name.endswith('create.pw.toml') else b'synthetic context\n')
            p = root/name
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_bytes(raw)
            context.append(dict(source=name, blob='synthetic_blob', sha256=prepare.sha(raw)))
        (pack/'pack.mcmeta').write_text(json.dumps(dict(pack=dict(pack_format=15, description=scope['pack_id']))))
        synthetic_projection = {'synthetic_control_only': True}
        index = pack/prepare.INDEX
        index.parent.mkdir(parents=True, exist_ok=True)
        index.write_text('{"replace":true,"entries":[]}')
        original = directory/'canonical-parent/global_loot_modifiers.json'
        original.parent.mkdir()
        original.write_bytes((root/'kubejs'/prepare.INDEX).read_bytes())
        (directory/'frozen-scope.json').write_bytes(prepare.fixed_glm.POLICY.read_bytes())
        data = dict(schema='bc.crops_canonical_snapshot.v2', pack_id=scope['pack_id'], pack_format=15,
                    root=str(root), archives='SYNTHETIC_NOT_NATIVE', attempt=directory.name, scope_sha256=prepare.sha(prepare.SCOPE.read_bytes()),
                    resources=rows, context_inputs=context, projection=synthetic_projection, global_index_projected=True)
        manifest = directory/'manifest.json'
        manifest.write_text(json.dumps(data))
        return directory, manifest, data

    def verify_with_synthetic_git(self, manifest, dirty=False):
        original, original_projection, original_roles = prepare.git, prepare.fixed_glm.verify, prepare.verify_sdk_roles
        # Role admission is separately exercised by test_canonical_sdk_roles, never by this IO fixture.
        prepare.verify_sdk_roles = lambda data: None
        prepare.git = lambda root, *args: ('dirty' if dirty else '') if args[0] == 'status' else 'synthetic_blob'
        # This fixture tests snapshot IO/oracles only, NOT real Root/archive/Native closure.
        prepare.fixed_glm.verify = lambda root, archives: ({'synthetic_control_only': True}, b'{"replace":true,"entries":[]}', (Path(root)/'kubejs'/prepare.INDEX).read_bytes())
        try:
            return prepare.verify(manifest)
        finally:
            prepare.git, prepare.fixed_glm.verify, prepare.verify_sdk_roles = original, original_projection, original_roles

    def test_exact_scope_and_context_not_projected(self):
        directory, manifest, data = self.fixture()
        self.assertEqual(len(self.verify_with_synthetic_git(manifest)['resources']), 10)
        self.assertNotIn(prepare.INDEX, [r['resource'] for r in data['resources']])
        self.assertEqual(len(data['context_inputs']), 3)
        print('Retained SYNTHETIC canonical controls:', directory)

    def test_missing_extra_reordered_duplicate_or_changed_inputs_fail(self):
        for kind in ('missing', 'extra', 'order', 'duplicate', 'bytes', 'scope', 'context', 'dirty'):
            directory, manifest, data = self.fixture()
            if kind == 'missing': (directory/'pack'/data['resources'][0]['resource']).rename(directory/'retained-missing-control.json')
            if kind == 'extra': (directory/'pack/unexpected.json').write_text('{}')
            if kind == 'order': data['resources'].reverse()
            if kind == 'duplicate': data['resources'][1] = data['resources'][0]
            if kind == 'bytes': (directory/'pack'/data['resources'][0]['resource']).write_text('{}')
            if kind == 'scope': data['scope_sha256'] = 'wrong'
            if kind == 'context': data['context_inputs'][0]['sha256'] = 'wrong'
            manifest.write_text(json.dumps(data))
            with self.assertRaises((AssertionError, ValueError)):
                self.verify_with_synthetic_git(manifest, dirty=kind == 'dirty')

    def test_actual_archive_exclusion_oracle_synthetic_negative_controls(self):
        directory, _, data = self.fixture()
        for i, name in enumerate(['META-INF/mods.toml', 'com/bettercontent/betterbumblezonecrops/nativefixture/CanonicalDataFixture.class',
                                  'com/bettercontent/betterbumblezonecrops/nativefixture/CanonicalDataFixture.java',
                                  'bc_crops_canonical_source_scope.json', data['resources'][0]['resource']]):
            jar = directory/(str(i)+'.jar')
            with zipfile.ZipFile(jar, 'w') as z:
                z.writestr(name, 'synthetic control only')
            if i == 0: archive.check(jar)
            else:
                with self.assertRaises(ValueError): archive.check(jar)

    def test_fixed_226_72_77_53_policy_mutation_controls(self):
        model = json.loads(prepare.fixed_glm.POLICY.read_bytes())
        self.assertEqual(len(prepare.fixed_glm.validate_model(model)), 72)
        for kind in ('drop', 'reorder', 'addition', 'raw5', 'sdk', 'definition', 'codec', 'omission'):
            mutant = copy.deepcopy(model)
            if kind == 'drop': mutant['proposed_ordered_in_domain_ids'].pop()
            if kind == 'reorder': mutant['proposed_ordered_in_domain_ids'].reverse()
            if kind == 'addition': next(r for r in mutant['root_rows'] if r['decision'] == prepare.fixed_glm.OUT)['production_resource_owners'][0]['selected_sdk_candidate'] = True
            if kind == 'raw5': mutant['every_raw_sdk_id_excluded_by_root'].pop()
            if kind == 'sdk': mutant['selected_sdk_parent_hashes'].pop()
            if kind == 'definition': mutant['root_rows'][0]['effective_selected_definition'] = None
            if kind == 'codec': mutant['root_rows'][0]['primary_codec'] = None
            if kind == 'omission': mutant['root_rows'].pop()
            with self.assertRaises(ValueError, msg=kind):
                prepare.fixed_glm.validate_model(mutant)

    def test_source_api_native_always_required_production_off_and_unmodified_count(self):
        script = (ROOT/'gradle/native-canonical-data.gradle').read_text()
        self.assertNotIn('def enabled', script)
        self.assertIn("tasks.register('prepareCanonicalDataSnapshot')", script)
        self.assertIn("dependsOn('prepareCanonicalDataSnapshot')", script)
        self.assertIn("file('.native-fixture-inputs/' + token + '/manifest.json')", script)
        for binding in ('BC_CANONICAL_PACK_ROOT', 'BC_CANONICAL_ARCHIVES', 'BC_CANONICAL_ACTIVITY', 'BC_OWNER_SESSION'):
            self.assertIn(binding, script)
        self.assertIn('Required canonical native inputs missing', script)
        self.assertIn("'--attempt', token, '--task', bindings.BC_CANONICAL_ACTIVITY", script)
        self.assertIn("minecraft.runs.getByName('gameTestServer')", script)
        self.assertIn("expected = run.name == 'gameTestServer'", script)
        self.assertIn("create-328085:7178761", script)
        self.assertNotIn('miners-delight', script)
        self.assertNotIn('create-328085:7178761', (ROOT/'gradle/native-provider-closure.gradle').read_text())
        source = (ROOT/'src/nativeFixture/java/com/bettercontent/betterbumblezonecrops/nativefixture/CanonicalDataFixture.java').read_text()
        for token in ('Pack.Position.TOP, true', 'resource.sourcePackId()', 'server.getResourceManager().getResource(id)',
                      'StandardOpenOption.CREATE_NEW', 'getSelectedIds()', 'globalIndexProjected', 'contextInputs'):
            self.assertIn(token, source)
        self.assertNotIn('getBlockState(', source)
        self.assertNotIn('getChunk(', source)
        self.assertNotIn('kubejs', script.lower())
        existing = (ROOT/'gradle/native-gametest-fixture.gradle').read_text()
        self.assertIn('All 11 required tests passed', existing)
        self.assertIn('11 GAME TESTS COMPLETE', existing)


if __name__ == '__main__':
    unittest.main()
