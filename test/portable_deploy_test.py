"""Local deployment checks; no external network, database or package manager."""
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
import fetch_assets
import init_configs


class PortableDeployTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def bundle(self, name="data/map/tile_set_info", valid=True):
        archive = self.root / "sample.zip"
        with zipfile.ZipFile(archive, "w") as output:
            output.writestr(name, b"test-data")
        digest = hashlib.sha256(archive.read_bytes()).hexdigest()
        manifest = {"url": archive.as_uri(), "sha256": digest if valid else "0" * 64,
                    "required_files": ["data/map/tile_set_info"]}
        (self.root / "runtime-assets.json").write_text(json.dumps(manifest))
        return manifest

    def test_download_integrity_and_idempotency(self):
        manifest = self.bundle()
        folder = self.root / "data/config"
        folder.mkdir(parents=True)
        config = folder / "alyra.properties"
        config.write_text("private-local-settings")
        with patch.object(fetch_assets, "ROOT", self.root):
            fetch_assets.main()
            with patch.object(fetch_assets.urllib.request, "urlopen", side_effect=AssertionError("Unexpected download")):
                fetch_assets.main()
        self.assertEqual(config.read_text(), "private-local-settings")
        self.assertEqual((self.root / "data/.assets-version").read_text().strip(), manifest["sha256"])

    def test_bad_checksum_extracts_nothing(self):
        self.bundle(valid=False)
        with patch.object(fetch_assets, "ROOT", self.root):
            with self.assertRaisesRegex(ValueError, "checksum"):
                fetch_assets.main()
        self.assertFalse((self.root / "data").exists())

    def test_path_traversal_and_config_replacement_rejected(self):
        for name in ("../outside", "data/../outside", "/data/map/file", "data/config/alyra.properties"):
            with self.subTest(name=name):
                with self.assertRaises(ValueError):
                    fetch_assets.checked_name(zipfile.ZipInfo(name))

    def test_config_initialization_preserves_local_and_leaves_admin_uninitialized(self):
        folder = self.root / "data/config"
        folder.mkdir(parents=True)
        (folder / "alyra.properties.example").write_text("example")
        with patch.object(init_configs, "ROOT", self.root):
            init_configs.main()
            self.assertEqual((folder / "alyra.properties").read_text(), "example")
            (folder / "alyra.properties").write_text("private")
            init_configs.main()
        self.assertEqual((folder / "alyra.properties").read_text(), "private")
        self.assertFalse((folder / "web_admin.properties").exists())


if __name__ == "__main__":
    unittest.main()
