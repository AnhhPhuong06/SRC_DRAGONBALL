#!/usr/bin/env bash
# Native Termux menu; run with: bash termux.sh
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if [[ -z "${PREFIX:-}" || "$PREFIX" != *com.termux* ]]; then
    echo 'File nay danh cho Termux Android. Ubuntu: bash ubuntu.sh' >&2
    exit 1
fi
export JAVA_XMS="${JAVA_XMS:-128m}"
export JAVA_XMX="${JAVA_XMX:-768m}"
case "${1:-menu}" in
    install)
        pkg update
        pkg install -y openjdk-21 python mariadb
        python "$ROOT/tools/init_configs.py"
        python "$ROOT/tools/fetch_assets.py"
        exec python "$ROOT/tools/termux_manager.py" setup
        ;;
    menu|setup|database|run|build|check|admin)
        if ! command -v python >/dev/null 2>&1; then
            echo 'Chay truoc: bash termux.sh install' >&2
            exit 1
        fi
        exec python "$ROOT/tools/termux_manager.py" "${1:-menu}" "${@:2}"
        ;;
    *) echo 'Dung: bash termux.sh [install|menu|setup|database|run|build|check|admin]' >&2; exit 1 ;;
esac
