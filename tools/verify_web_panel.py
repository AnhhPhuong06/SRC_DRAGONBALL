#!/usr/bin/env python3
"""Build the panel in a fresh directory; optionally run isolated tests, never the game."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile

OLD_LOGGING = {"slf4j-api-2.0.0-alpha1.jar", "slf4j-simple-2.0.0-alpha1.jar"}
TESTS = [
    "test/WebAdminPanelTest.java",
    "test/WebAdminSecurityTest.java",
    "test/server/web/admin/AdminInputValidationTest.java",
]
OPENS = ("code", "comp", "file", "main", "model", "parser", "processing", "tree", "util", "jvm")


def run(command, cwd, logfile, timeout=600):
    result = subprocess.run(command, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, encoding="utf-8", errors="replace", timeout=timeout)
    logfile.write_text(result.stdout, encoding="utf-8")
    if result.returncode:
        print("\n".join(result.stdout.splitlines()[-120:]))
        raise SystemExit(f"Command failed ({result.returncode}). Full output: {logfile}")
    return result.stdout


def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("project", type=Path)
    parser.add_argument("--jdk", type=Path, default=None)
    parser.add_argument("--tests", action="store_true", help="Run only fake/fixture panel tests, not ServerManager")
    args = parser.parse_args()
    project = args.project.resolve()
    jdk = args.jdk or Path(os.environ.get("JAVA_HOME", "C:/Program Files/Java/jdk-21.0.12"))
    suffix = ".exe" if os.name == "nt" else ""
    javac, java, jar = [jdk / "bin" / (name + suffix) for name in ("javac", "java", "jar")]
    if not all(path.is_file() for path in (javac, java, jar)):
        parser.error("Provide --jdk pointing to a complete JDK 21+ installation")
    lib = project / "lib"
    lockfile = lib / "web-dependencies.lock.json"
    if not lockfile.is_file():
        parser.error("Fetch web dependencies first: python -I tools/download_web_dependencies.py PROJECT")
    for dependency in json.loads(lockfile.read_text("utf-8")):
        path = lib / dependency["file"]
        if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != dependency["sha256"]:
            parser.error(f"Missing or changed pinned dependency: {dependency['file']}")
    jars = sorted(path for path in lib.glob("*.jar") if path.name not in OLD_LOGGING)
    classpath = os.pathsep.join(str(path) for path in jars)
    parent = project / "build"
    parent.mkdir(exist_ok=True)
    output = Path(tempfile.mkdtemp(prefix="panel-verification-", dir=parent))
    classes = output / "classes"
    classes.mkdir()
    sources = sorted((project / "src").rglob("*.java"))
    argfile = output / "sources.txt"
    argfile.write_text("\n".join('"' + source.as_posix() + '"' for source in sources) + "\n", encoding="utf-8")
    print(f"Verification directory: {output}", flush=True)
    print(f"Compile {len(sources)} Java sources with {javac}", flush=True)
    command = [str(javac)] + [f"-J--add-opens=jdk.compiler/com.sun.tools.javac.{name}=ALL-UNNAMED" for name in OPENS]
    command += ["--release", "21", "-encoding", "UTF-8", "-sourcepath", str(project / "src"),
                "-cp", classpath, "-processorpath", str(lib / "lombok.jar"), "-d", str(classes), "@" + str(argfile)]
    compilation = run(command, output, output / "compile.log")
    print("Production compilation passed.", flush=True)
    if compilation.strip():
        print("\n".join(compilation.splitlines()[-8:]), flush=True)
    # Apply the same public-asset allowlist as the shipping build. web/data may
    # contain private configuration and old credential backups.
    run([sys.executable, "-I", str(project / "tools/stage_panel_assets.py"), str(project), str(classes)],
        project, output / "stage-assets.log")
    (classes / "sql").mkdir()
    shutil.copy2(project / "sql" / "web_admin.sql", classes / "sql" / "web_admin.sql")
    (output / "lib").mkdir()
    for dependency in jars:
        shutil.copy2(dependency, output / "lib" / dependency.name)
    artifact = output / "chienbinhlucy.jar"
    run([str(jar), "cfm", str(artifact), str(project / "manifest.mf"), "-C", str(classes), "."], output, output / "package.log")
    print(f"JAR packaged: {artifact}", flush=True)
    report = {"artifact": str(artifact), "sourceCount": len(sources), "tests": [],
              "liveDatabaseUsed": False, "gameStarted": False}
    if args.tests:
        test_sources = [project / path for path in TESTS]
        missing = [str(path) for path in test_sources if not path.is_file()]
        if missing:
            raise SystemExit("Missing panel tests: " + ", ".join(missing))
        test_classes = output / "test-classes"
        test_classes.mkdir()
        test_cp = os.pathsep.join([str(artifact), classpath])
        run([str(javac), "--release", "21", "-encoding", "UTF-8", "-proc:none", "-cp", test_cp,
             "-d", str(test_classes)] + [str(path) for path in test_sources], output, output / "test-compile.log")
        fixture = output / "web-panel-fixture"
        fixture.mkdir()
        runtime_cp = os.pathsep.join([str(test_classes), str(artifact)])
        for source in test_sources:
            text = source.read_text("utf-8")
            package = re.search(r"^package\s+([\w.]+)\s*;", text, re.MULTILINE)
            main_class = (package.group(1) + "." if package else "") + source.stem
            print(f"Running {main_class} in isolated fixture...", flush=True)
            test_output = run([str(java), "-Djava.awt.headless=true", "-cp", runtime_cp, main_class], fixture,
                              output / (source.stem + ".log"), timeout=240)
            print(test_output.strip(), flush=True)
            report["tests"].append({"class": main_class, "passed": True})
    (output / "verification.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print("Verification passed. No game process, real DB migration or real grants were executed.", flush=True)


if __name__ == "__main__":
    main()
