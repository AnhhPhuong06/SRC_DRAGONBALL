#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
for interpreter in python3 python; do
    if command -v "$interpreter" >/dev/null 2>&1; then
        exec "$interpreter" "$ROOT/tools/server_cli.py" "$@"
    fi
done
printf '%s\n' 'Python 3.10+ is required. Ubuntu: sudo apt install python3; Termux: pkg install python' >&2
exit 1
