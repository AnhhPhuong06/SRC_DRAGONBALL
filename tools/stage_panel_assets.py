#!/usr/bin/env python3
"""Stage only public panel assets; never package configuration or backups."""
import argparse
from pathlib import Path
import shutil

EXTENSIONS = {".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".woff", ".woff2", ".ttf"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("project", type=Path)
    parser.add_argument("classes", type=Path)
    args = parser.parse_args()
    project = args.project.resolve()
    classes = args.classes.resolve()
    build = (project / "build").resolve()
    if not classes.is_relative_to(build) or classes == build:
        parser.error("The generated classes directory must be inside this project's build directory")
    source = project / "web"
    target = classes / "web"
    if target.is_symlink():
        parser.error("Refusing a symlink resource destination")
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True)
    for name in ("index.html", "favicon.ico"):
        path = source / name
        if path.is_file() and not path.is_symlink():
            shutil.copy2(path, target / name)
    if not (target / "index.html").is_file():
        parser.error("Missing web/index.html")
    count = 1
    for folder in ("css", "js", "assets"):
        root = source / folder
        if not root.is_dir() or root.is_symlink():
            continue
        for path in root.rglob("*"):
            if not path.is_file() or path.suffix.lower() not in EXTENSIONS:
                continue
            if path.is_symlink() or not path.resolve().is_relative_to(root.resolve()):
                parser.error(f"Refusing symlink asset: {path}")
            relative = path.relative_to(source)
            if any(part.startswith(".") for part in relative.parts):
                continue
            destination = target / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, destination)
            count += 1
    print(f"Staged {count} public panel files; private data and backups excluded.")


if __name__ == "__main__":
    main()
