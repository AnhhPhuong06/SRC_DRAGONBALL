#!/usr/bin/env python3
"""Portable Java 21 server build and headless launcher (Ubuntu / Termux)."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parent.parent
EXCLUDED = {"slf4j-api-2.0.0-alpha1.jar", "slf4j-simple-2.0.0-alpha1.jar"}


def java_tool(name):
    suffix = ".exe" if os.name == "nt" else ""
    home = os.environ.get("JAVA_HOME")
    candidate = Path(home) / "bin" / (name + suffix) if home else None
    if candidate and candidate.is_file():
        return str(candidate)
    found = shutil.which(name)
    if not found:
        raise RuntimeError("Missing " + name + ". Install OpenJDK 21 and check JAVA_HOME/PATH.")
    return found


def require_java(tool, build=False):
    result = subprocess.run([tool, "-version"], capture_output=True, text=True, check=True)
    match = re.search(r'(?:version\s+"|javac\s+)(\d+)', result.stdout + result.stderr)
    if not match or (int(match[1]) != 21 if build else int(match[1]) < 21):
        raise RuntimeError("Build requires JDK 21; running requires Java 21 or newer.")


def libraries():
    jars = sorted(p for p in (ROOT / "lib").glob("*.jar") if p.name not in EXCLUDED)
    if not jars:
        raise RuntimeError("Missing lib/*.jar dependencies.")
    return jars


def build():
    javac, jar = java_tool("javac"), java_tool("jar")
    require_java(javac, build=True)
    jars = libraries()
    build_dir = ROOT / "build"
    build_dir.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="portable-", dir=build_dir) as temp:
        work = Path(temp)
        classes = work / "classes"
        classes.mkdir()
        sources = sorted((ROOT / "src").rglob("*.java"))
        if not sources:
            raise RuntimeError("Missing src/*.java sources.")
        arguments = work / "sources.txt"
        arguments.write_text("\n".join('"' + p.as_posix() + '"' for p in sources), encoding="utf-8")
        opens = ["-J--add-opens=jdk.compiler/com.sun.tools.javac." + part + "=ALL-UNNAMED"
                 for part in "code comp file main model parser processing tree util jvm".split()]
        subprocess.run([javac, *opens, "--release", "21", "-encoding", "UTF-8",
                        "-cp", os.pathsep.join(map(str, jars)), "-processorpath", str(ROOT / "lib/lombok.jar"),
                        "-d", str(classes), "@" + str(arguments)], cwd=ROOT, check=True)
        subprocess.run([sys.executable, "-I", str(ROOT / "tools/stage_panel_assets.py"),
                        str(ROOT), str(classes)], cwd=ROOT, check=True)
        artifact = work / "chienbinhlucy.jar"
        subprocess.run([jar, "cfm", str(artifact), str(ROOT / "manifest.mf"),
                        "-C", str(classes), "."], cwd=ROOT, check=True)
        destination = ROOT / "dist"
        runtime_libs = destination / "lib"
        runtime_libs.mkdir(parents=True, exist_ok=True)
        for dependency in jars:
            shutil.copy2(dependency, runtime_libs / dependency.name)
        # Publish only after compilation, asset staging and packaging succeed.
        os.replace(artifact, destination / artifact.name)
    print("Built dist/chienbinhlucy.jar. No database or server was started.")


def launch(command, arguments):
    java = java_tool("java")
    require_java(java)
    artifact = ROOT / "dist/chienbinhlucy.jar"
    if not artifact.is_file():
        raise RuntimeError("Missing dist/chienbinhlucy.jar. Run bash build_server.sh first.")
    if command != "admin":
        if not (ROOT / "data/config/alyra.properties").is_file():
            raise RuntimeError("Missing database config. Run tools/init_configs.py and edit data/config/alyra.properties.")
        manifest = ROOT / "runtime-assets.json"
        if manifest.is_file():
            required = json.loads(manifest.read_text(encoding="utf-8"))["required_files"]
            if any(not (ROOT / name).is_file() for name in required):
                raise RuntimeError("Missing game assets. Ubuntu: bash ubuntu.sh assets; Termux: python tools/fetch_assets.py")
    if command == "admin":
        argv = [java, "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-cp", str(artifact),
                "server.web.tools.WebAdminInit", *arguments]
    else:
        if "-oldpanel" in arguments:
            raise RuntimeError("Ubuntu/Termux launcher uses headless mode; -oldpanel requires a desktop.")
        minimum = os.environ.get("JAVA_XMS", "256m")
        maximum = os.environ.get("JAVA_XMX", "2048m")
        mode = "-web" if command == "web" or "-web" in arguments else "-nogui"
        arguments = [arg for arg in arguments if arg not in ("-web", "-nogui")]
        argv = [java, "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-Xms" + minimum,
                "-Xmx" + maximum, "-jar", str(artifact), mode, *arguments]
    os.chdir(ROOT)
    os.execv(java, argv)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("build", "run", "web", "admin", "check"))
    parser.add_argument("arguments", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    if args.command in ("build", "check") and args.arguments:
        parser.error("This command takes no extra arguments.")
    try:
        if args.command == "build":
            build()
        elif args.command == "check":
            require_java(java_tool("java"))
            require_java(java_tool("javac"), build=True)
            java_tool("jar")
            print("Java 21 build tools available;", len(libraries()), "dependencies found.")
        else:
            launch(args.command, args.arguments)
    except (RuntimeError, OSError, subprocess.CalledProcessError) as error:
        print("Error:", error, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
