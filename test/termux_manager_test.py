"""Isolated Termux setup tests: no packages, MariaDB or game are started."""
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
import termux_manager as manager


class TermuxSetupTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "data/config").mkdir(parents=True)
        (self.root / "sql").mkdir()
        for name in ("nro_acc.sql", "nro_data.sql"):
            (self.root / "sql" / name).write_text("SELECT 1;")
        self.config = self.root / "data/config/alyra.properties"
        self.config.write_text("original-config")
        for target, value in (("ROOT", self.root), ("STATE", self.root / ".termux")):
            p = patch.object(manager, target, value)
            p.start()
            self.addCleanup(p.stop)
        self.root.joinpath(".termux").mkdir()

    def test_cancel_does_not_start_database(self):
        with patch.object(manager, "tool"), patch.object(manager.server_cli, "java_tool"), \
             patch.object(manager.server_cli, "require_java"), patch("builtins.input", return_value=""), \
             patch.object(manager, "database") as database:
            manager.setup()
            database.assert_not_called()
        self.assertEqual(self.config.read_text(), "original-config")

    def test_existing_database_refused(self):
        with patch.object(manager, "tool"), patch.object(manager.server_cli, "java_tool"), \
             patch.object(manager.server_cli, "require_java"), patch("builtins.input", return_value="TAO MOI"), \
             patch.object(manager, "database", return_value=(["mariadb"], 3306)), \
             patch.object(manager.subprocess, "run", return_value=subprocess.CompletedProcess([], 0, "supervip_termux_acc")) as run:
            with self.assertRaisesRegex(RuntimeError, "da ton tai"):
                manager.setup()
            self.assertEqual(run.call_count, 1)
        self.assertEqual(self.config.read_text(), "original-config")

    def test_success_imports_both_and_backs_up_configuration(self):
        with patch.object(manager, "tool"), patch.object(manager.server_cli, "java_tool"), \
             patch.object(manager.server_cli, "require_java"), patch("builtins.input", return_value="TAO MOI"), \
             patch.object(manager, "database", return_value=(["mariadb"], 3307)), \
             patch.object(manager.subprocess, "run", return_value=subprocess.CompletedProcess([], 0, "")) as run:
            manager.setup()
            self.assertEqual(run.call_count, 4)
            self.assertEqual(run.call_args_list[-2].args[0][-1], "supervip_termux_acc")
            self.assertEqual(run.call_args_list[-1].args[0][-1], "supervip_termux_data")
        text = self.config.read_text()
        self.assertIn("original-config", text)
        self.assertIn("database.port=3307", text)
        self.assertIn("database.user=svt_", text)
        backups = list((self.root / ".termux").glob("alyra-before-*.properties"))
        self.assertEqual(len(backups), 1)
        self.assertEqual(backups[0].read_text(), "original-config")

    def test_failed_import_preserves_configuration(self):
        results = [subprocess.CompletedProcess([], 0, ""), subprocess.CompletedProcess([], 0),
                   subprocess.CalledProcessError(1, ["mariadb"])]
        with patch.object(manager, "tool"), patch.object(manager.server_cli, "java_tool"), \
             patch.object(manager.server_cli, "require_java"), patch("builtins.input", return_value="TAO MOI"), \
             patch.object(manager, "database", return_value=(["mariadb"], 3306)), \
             patch.object(manager.subprocess, "run", side_effect=results):
            with self.assertRaises(subprocess.CalledProcessError):
                manager.setup()
        self.assertEqual(self.config.read_text(), "original-config")

    def test_termux_package_root_fallback(self):
        (self.root / "var/lib/mysql/mysql").mkdir(parents=True)
        results = [subprocess.CompletedProcess([], 0), subprocess.CompletedProcess([], 1, ""),
                   subprocess.CompletedProcess([], 0, "3306\n")]
        with patch.dict(manager.os.environ, {"PREFIX": str(self.root)}), \
             patch.object(manager, "tool", side_effect=lambda name: name), \
             patch.object(manager.subprocess, "run", side_effect=results):
            client, port = manager.database()
        self.assertIn("--user=root", client)
        self.assertEqual(port, 3306)


if __name__ == "__main__":
    unittest.main()
