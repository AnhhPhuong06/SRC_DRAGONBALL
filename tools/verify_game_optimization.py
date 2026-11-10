"""Compile game sources and run isolated optimization/gameplay regressions.

No database or game server is started. --package updates only the affected
classes after optimization checks pass and any unrelated failures reproduce
against original classes, keeping a backup first.
"""
from pathlib import Path
import argparse
import difflib
import hashlib
import json
import os
import re
import shutil
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'out/game-optimization'
CHANGED = [
    'src/utils/FileIO.java', 'src/utils/AssetCache.java',
    'src/network/Sender.java', 'src/network/QueueHandler.java',
    'src/models/mob/Mob.java', 'src/models/map/Zone.java',
]
SUITES = [
    'AssetCacheTest', 'NetworkQueueTest', 'WorldHotPathTest',
    'StabilityRegressionTest', 'XenRespawnTest', 'BossDropRulesTest',
    'MobColdDropTest', 'MobDropRulesTest', 'SieuHoaCaiTrangTest',
    'CombineTenAttemptsTest', 'CombineRetainedSelectionTest',
    'PhapSuHoaTest', 'ToriNguSacShopTest', 'ShopLegacySlotsTest',
]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    options = argparse.ArgumentParser()
    options.add_argument('--package', action='store_true')
    args = options.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    jdk = Path(os.environ.get('JAVA_HOME', 'C:/Program Files/Java/jdk-21.0.12'))
    if not (jdk / 'bin/javac.exe').is_file():
        raise SystemExit('Set JAVA_HOME to JDK 21.')
    classes = OUT / 'classes'
    tests = OUT / 'test-classes'
    classes.mkdir(exist_ok=True)
    tests.mkdir(exist_ok=True)
    libraries = sorted(p for p in (ROOT / 'lib').glob('*.jar') if '2.0.0-alpha1' not in p.name)
    cp = os.pathsep.join(map(str, libraries))
    before_configs = {str(p.relative_to(ROOT)): digest(p) for p in (ROOT / 'data/config').glob('*') if p.is_file()}

    def run(command, name, cwd=ROOT, timeout=180, check=True):
        result = subprocess.run(list(map(str, command)), cwd=cwd, capture_output=True, timeout=timeout)
        output = result.stdout + result.stderr
        (OUT / (name + '.log')).write_bytes(output)
        print(name, 'exit', result.returncode, flush=True)
        if result.returncode and check:
            print(output.decode('utf-8', errors='replace')[-8000:], flush=True)
            raise SystemExit(result.returncode)
        for line in output.decode('utf-8', errors='replace').splitlines():
            if line.startswith('PASS:'):
                print(line, flush=True)
        return result

    sources = OUT / 'sources.txt'
    sources.write_text('\n'.join('"' + p.as_posix() + '"' for p in sorted((ROOT / 'src').rglob('*.java'))), encoding='utf-8')
    opens = ['-J--add-opens=jdk.compiler/com.sun.tools.javac.' + name + '=ALL-UNNAMED'
             for name in ['code', 'comp', 'file', 'main', 'model', 'parser', 'processing', 'tree', 'util', 'jvm']]
    run([jdk / 'bin/javac.exe', *opens, '--release', '21', '-encoding', 'UTF-8',
         '-cp', cp, '-processorpath', ROOT / 'lib/lombok.jar', '-d', classes, '@' + str(sources)], 'compile')
    test_sources = sorted((ROOT / 'test').glob('*.java'))
    test_args = OUT / 'test-sources.txt'
    test_args.write_text('\n'.join('"' + p.as_posix() + '"' for p in test_sources), encoding='utf-8')
    run([jdk / 'bin/javac.exe', '-proc:none', '--release', '21', '-encoding', 'UTF-8', '-cp', str(classes) + os.pathsep + cp,
         '-d', tests, '@' + str(test_args)], 'compile-tests')
    names = {}
    for path in test_sources:
        match = re.search(r'^package\s+([\w.]+);', path.read_text(encoding='utf-8-sig'), re.M)
        names[path.stem] = (match[1] + '.' if match else '') + path.stem
    # Make legacy RNG fixtures repeatable when comparing current vs. original classes.
    seeded = OUT / 'SeededRegressionCheck.java'
    seeded.write_text('''public class SeededRegressionCheck {
        public static void main(String[] args) throws Throwable {
            java.lang.reflect.Field field = utils.Util.class.getDeclaredField("rand");
            field.setAccessible(true);
            ((java.util.Random)field.get(null)).setSeed(20261022L);
            try {
                Class.forName(args[0]).getMethod("main", String[].class).invoke(null, (Object)new String[0]);
            } catch (java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
        }
    }
    ''', encoding='utf-8')
    run([jdk / 'bin/javac.exe', '-proc:none', '-cp', str(tests) + os.pathsep + str(classes) + os.pathsep + cp,
         '-d', tests, seeded], 'compile-seeded-check')
    results = {}
    failures = {}
    known_issues = []
    for name in SUITES:
        with tempfile.TemporaryDirectory(prefix='nro-optimization-') as fixture:
            result = run([jdk / 'bin/java.exe', '-Djava.awt.headless=true', '-Dfile.encoding=UTF-8', '-cp',
                          str(tests) + os.pathsep + str(classes) + os.pathsep + cp, 'SeededRegressionCheck', names[name]], name, Path(fixture), 60,
                         check=name in SUITES[:3])
            results[name] = result.returncode
            if result.returncode:
                failures[name] = result.stderr

    # Demonstrate the eliminated scan on the same fixture against the saved original Mob.
    baseline = OUT / 'before/src/models/mob/Mob.java'
    baseline_reproduced = None
    if baseline.is_file():
        baseline_classes = OUT / 'baseline-classes'
        baseline_classes.mkdir(exist_ok=True)
        originals = [OUT / 'before' / name for name in CHANGED if (OUT / 'before' / name).is_file()]
        run([jdk / 'bin/javac.exe', *opens, '--release', '21', '-encoding', 'UTF-8', '-cp', str(classes) + os.pathsep + cp,
             '-processorpath', ROOT / 'lib/lombok.jar', '-d', baseline_classes, *originals], 'baseline-compile')
        with tempfile.TemporaryDirectory(prefix='nro-baseline-') as fixture:
            result = run([jdk / 'bin/java.exe', '-Djava.awt.headless=true', '-cp',
                          str(tests) + os.pathsep + str(baseline_classes) + os.pathsep + str(classes) + os.pathsep + cp,
                          names['WorldHotPathTest']], 'baseline-world', Path(fixture), 60, check=False)
            baseline_reproduced = result.returncode != 0 and b'skip the entire humanoid scan' in result.stderr
            if not baseline_reproduced:
                raise SystemExit('Baseline did not reproduce the expected redundant target scan.')
        for name, failure in failures.items():
            with tempfile.TemporaryDirectory(prefix='nro-baseline-regression-') as fixture:
                original = run([jdk / 'bin/java.exe', '-Djava.awt.headless=true', '-Dfile.encoding=UTF-8', '-cp',
                                str(tests) + os.pathsep + str(baseline_classes) + os.pathsep + str(classes) + os.pathsep + cp,
                                'SeededRegressionCheck', names[name]], 'baseline-' + name, Path(fixture), 60, check=False)
                signature = next((line for line in failure.splitlines() if line.startswith(b'Exception in thread "main" ')), None)
                if original.returncode == 0 or not signature or signature not in original.stderr:
                    raise SystemExit('New regression or non-repeatable baseline failure: ' + name)
                known_issues.append(name + ': ' + signature.decode('utf-8', errors='replace'))
    elif failures:
        raise SystemExit('Cannot classify old test failures without the saved baseline.')

    diff = []
    for relative in CHANGED:
        original = OUT / 'before' / relative
        before = original.read_text(encoding='utf-8').splitlines(True) if original.is_file() else []
        after = (ROOT / relative).read_text(encoding='utf-8').splitlines(True)
        diff.extend(difflib.unified_diff(before, after, fromfile='before/' + relative, tofile=relative))
    (OUT / 'changes.diff').write_text(''.join(diff), encoding='utf-8')
    for path, value in before_configs.items():
        if digest(ROOT / path) != value:
            raise SystemExit('Unexpected configuration mutation: ' + path)

    audit = dict(optimization_checks_passed=True, all_selected_suites_passed=not known_issues,
                 known_preexisting_issues=known_issues, changed_sources=CHANGED, suites=results, baseline_reproduced=baseline_reproduced,
                 database_access='none; test state is in memory or external temporary directories',
                 game_started=False, config_unchanged=True, packaged=False)
    if args.package:
        jar = ROOT / 'dist/chienbinhlucy.jar'
        backup = OUT / 'before/dist/chienbinhlucy.jar'
        backup.parent.mkdir(parents=True, exist_ok=True)
        if not backup.exists():
            shutil.copy2(jar, backup)
        replacements = {}
        for relative in CHANGED:
            base = relative.removeprefix('src/').removesuffix('.java')
            for path in (classes / base).parent.glob(Path(base).name + '*.class'):
                if path.stem == Path(base).name or path.stem.startswith(Path(base).name + '$'):
                    replacements[path.relative_to(classes).as_posix()] = path.read_bytes()
        staged = OUT / 'chienbinhlucy.optimized.jar'
        with zipfile.ZipFile(jar) as old, zipfile.ZipFile(staged, 'w') as new:
            for entry in old.infolist():
                new.writestr(entry, replacements.get(entry.filename, old.read(entry.filename)))
            for name, data in replacements.items():
                if name not in old.namelist():
                    new.writestr(name, data, compress_type=zipfile.ZIP_DEFLATED)
        with zipfile.ZipFile(jar) as old, zipfile.ZipFile(staged) as new:
            assert new.testzip() is None
            for name in old.namelist():
                if name not in replacements:
                    assert old.read(name) == new.read(name), name
        for name in ['AssetCacheTest', 'NetworkQueueTest', 'WorldHotPathTest']:
            with tempfile.TemporaryDirectory(prefix='nro-packaged-test-') as fixture:
                run([jdk / 'bin/java.exe', '-Djava.awt.headless=true', '-Dfile.encoding=UTF-8', '-cp',
                     str(tests) + os.pathsep + str(staged) + os.pathsep + cp, names[name]], 'packaged-' + name, Path(fixture), 60)
        shutil.copy2(staged, jar)
        assert digest(staged) == digest(jar)
        audit.update(packaged=True, jar_sha256=digest(jar), jar_changed_entries=sorted(replacements),
                     jar_backup=str(backup.relative_to(ROOT)), all_other_jar_entries_unchanged=True)
    (OUT / 'audit.json').write_text(json.dumps(audit, indent=2, ensure_ascii=False), encoding='utf-8')
    print('PASS: optimization checks and unaffected gameplay regressions passed; config unchanged; no live game/database access.', flush=True)
    for issue in known_issues:
        print('KNOWN PRE-EXISTING ISSUE: ' + issue, flush=True)


if __name__ == '__main__':
    main()
