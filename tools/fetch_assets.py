#!/usr/bin/env python3
"""Download the versioned public game assets, checking SHA-256 before extraction."""
import hashlib
import json
from pathlib import Path, PurePosixPath
import shutil
import stat
import tempfile
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parent.parent


def checked_name(entry):
    path = PurePosixPath(entry.filename)
    if (path.is_absolute() or ".." in path.parts or "\\" in entry.filename
            or not path.parts or path.parts[0] != "data"
            or len(path.parts) < 3 or path.parts[1] == "config"
            or stat.S_ISLNK(entry.external_attr >> 16)):
        raise ValueError("Unsafe asset path: " + entry.filename)
    return path


def main():
    manifest = json.loads((ROOT / "runtime-assets.json").read_text(encoding="utf-8"))
    marker = ROOT / "data/.assets-version"
    if marker.is_file() and marker.read_text().strip() == manifest["sha256"]:
        if all((ROOT / name).is_file() for name in manifest["required_files"]):
            print("Game assets already installed.")
            return
    with tempfile.TemporaryDirectory(prefix="nro-assets-") as temp:
        archive = Path(temp) / "runtime-assets.zip"
        print("Downloading game assets:", manifest["url"], flush=True)
        with urllib.request.urlopen(manifest["url"], timeout=60) as response, archive.open("wb") as output:
            shutil.copyfileobj(response, output)
        with archive.open("rb") as source:
            digest = hashlib.file_digest(source, "sha256").hexdigest() if hasattr(hashlib, "file_digest") else None
        if digest is None:
            checksum = hashlib.sha256()
            with archive.open("rb") as source:
                for block in iter(lambda: source.read(1024 * 1024), b""):
                    checksum.update(block)
            digest = checksum.hexdigest()
        if digest != manifest["sha256"]:
            raise ValueError("Asset checksum mismatch; nothing extracted.")
        with zipfile.ZipFile(archive) as bundle:
            for entry in bundle.infolist():
                path = checked_name(entry)
                destination = ROOT.joinpath(*path.parts)
                resolved = destination.resolve()
                if not resolved.is_relative_to((ROOT / "data").resolve()):
                    raise ValueError("Asset path leaves data directory")
                if entry.is_dir():
                    destination.mkdir(parents=True, exist_ok=True)
                    continue
                destination.parent.mkdir(parents=True, exist_ok=True)
                with bundle.open(entry) as source, destination.open("wb") as output:
                    shutil.copyfileobj(source, output)
    if not all((ROOT / name).is_file() for name in manifest["required_files"]):
        raise ValueError("Asset bundle is missing required files")
    marker.write_text(manifest["sha256"] + "\n", encoding="ascii")
    print("Game assets installed; local configuration preserved.")


if __name__ == "__main__":
    main()
