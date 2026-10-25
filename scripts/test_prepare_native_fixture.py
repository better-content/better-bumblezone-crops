import json
from pathlib import Path
import tempfile
import unittest
import uuid
import prepare_native_fixture as fixture


class NativeFixtureTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.base = Path(self.tmp.name) / 'attempts'
        self.run = self.base / str(uuid.uuid4())

    def prepare(self):
        return fixture.prepare(self.run, self.base)

    def test_complete_flat_setup_and_native_default_seed_are_idempotent(self):
        record = self.prepare()
        self.assertTrue(record['seeded_properties'])
        self.assertTrue(record['seeded_general'])
        properties = (self.run / 'server.properties').read_text()
        self.assertIn('level-type=minecraft:flat\n', properties)
        self.assertIn('level-seed=0\n', properties)
        self.assertIn('generate-structures=false\n', properties)
        settings = json.loads(properties.split('generator-settings=', 1)[1])
        self.assertEqual(settings, fixture.FLAT)
        self.assertEqual(settings['structure_overrides'], [])
        self.assertEqual(sum(row['height'] for row in settings['layers']), 4)
        self.assertEqual(record, self.prepare())
        fixture.verify(self.run, self.base)

    def test_authored_properties_and_config_are_preserved_byte_for_byte(self):
        config = self.run / 'config/the_bumblezone/general.toml'
        config.parent.mkdir(parents=True)
        config.write_bytes(b'# authored\nvalue = 42\n')
        properties = self.run / 'server.properties'
        properties.write_bytes(b'max-tick-time=123456\nlevel-name=retained-config\n')
        record = self.prepare()
        self.assertFalse(record['seeded_properties'])
        self.assertFalse(record['seeded_general'])
        self.assertEqual(config.read_bytes(), b'# authored\nvalue = 42\n')
        self.assertEqual(properties.read_bytes(), b'max-tick-time=123456\nlevel-name=retained-config\n')
        fixture.verify(self.run, self.base)

    def test_unsafe_non_uuid_and_outside_paths_are_rejected(self):
        for path in [self.base, self.base / 'plain', Path(self.tmp.name) / str(uuid.uuid4()),
                     self.base / '..' / str(uuid.uuid4())]:
            with self.subTest(path=path), self.assertRaises(ValueError):
                fixture.prepare(path, self.base)

    def test_symlink_paths_and_nonregular_targets_are_rejected(self):
        self.run.mkdir(parents=True)
        elsewhere = Path(self.tmp.name) / 'elsewhere'
        elsewhere.mkdir()
        (self.run / 'config').symlink_to(elsewhere, target_is_directory=True)
        with self.assertRaises(ValueError):
            self.prepare()
        (self.run / 'config').unlink()
        (self.run / 'server.properties').mkdir()
        with self.assertRaises(ValueError):
            self.prepare()
        self.assertEqual(list(elsewhere.iterdir()), [])

    def test_existing_world_is_never_reused(self):
        (self.run / 'world').mkdir(parents=True)
        with self.assertRaises(ValueError):
            self.prepare()

    def test_default_schema_drift_and_postlaunch_rewrites_are_rejected(self):
        altered = Path(self.tmp.name) / 'altered.toml'
        altered.write_bytes(fixture.TEMPLATE.read_bytes() + b'\n# altered\n')
        with self.assertRaises(ValueError):
            fixture.prepare(self.run, self.base, altered)
        self.prepare()
        config = self.run / 'config/the_bumblezone/general.toml'
        config.write_bytes(config.read_bytes() + b'\n# rewritten\n')
        with self.assertRaises(ValueError):
            fixture.verify(self.run, self.base)

    def test_flower_lamp_is_authored_before_execution_and_deadline_remains_twenty(self):
        root = fixture.ROOT
        source = (root / 'src/main/java/com/bettercontent/betterbumblezonecrops/FlowerGameTests.java').read_text()
        self.assertRegex(source, r'@GameTest\(template = "flower_lit", timeoutTicks = 20\)\s+public static void flowerRelocationConservesQuantity')
        template = (root / 'src/main/resources/gameteststructures/flower_lit.snbt').read_text()
        self.assertIn('pos:[3,1,2]', template)
        self.assertIn('Name:"minecraft:glowstone"', template)
        body = source.split('void flowerRelocationConservesQuantity', 1)[1].split('@GameTest', 1)[0]
        self.assertIn('cycle < 20', body)
        self.assertIn('LightLayer.BLOCK, pos) == 14', body)
        self.assertIn('!dirtSurvival', body)
        self.assertIn('!dirtEvent.isCanceled()', body)
        self.assertIn('dirtBrightness == diagnostic.get("raw_brightness").getAsInt()', body)


if __name__ == '__main__':
    unittest.main()
