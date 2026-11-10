#!/usr/bin/env python3
"""Create local configuration from examples, without replacing existing files."""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parent.parent


def main():
    folder = ROOT / "data/config"
    for name in ("alyra.properties",):
        destination = folder / name
        if not destination.exists():
            shutil.copy2(folder / (name + ".example"), destination)
            destination.chmod(0o600)
            print("Created", destination.relative_to(ROOT), "- review before running.")


if __name__ == "__main__":
    main()
