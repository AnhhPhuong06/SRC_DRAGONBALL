#!/usr/bin/env bash
# Ubuntu launcher, independent from the native Android launcher.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if [[ "$(uname -s)" != Linux || "${PREFIX:-}" == *com.termux* ]]; then
    echo 'Ubuntu Linux only. Android: bash termux.sh' >&2
    exit 1
fi
export JAVA_XMS="${JAVA_XMS:-256m}"
export JAVA_XMX="${JAVA_XMX:-2048m}"

as_root() {
    if [[ "$EUID" -eq 0 ]]; then "$@"; else sudo "$@"; fi
}

cli() {
    if ! command -v python3 >/dev/null 2>&1; then
        echo 'Run first: bash ubuntu.sh install' >&2
        return 1
    fi
    python3 "$ROOT/tools/server_cli.py" "$@"
}

action() {
    local command="${1:-menu}"
    shift || true
    case "$command" in
        install)
            as_root apt-get update
            as_root apt-get install -y openjdk-21-jdk-headless python3 mariadb-server
            python3 "$ROOT/tools/init_configs.py"
            python3 "$ROOT/tools/fetch_assets.py"
            echo 'Packages installed. Configure your database using UBUNTU.md.'
            ;;
        database) as_root systemctl start mariadb ;;
        assets) python3 "$ROOT/tools/fetch_assets.py" ;;
        run|web) cli web "$@" ;;
        console) cli run "$@" ;;
        build|check) cli "$command" "$@" ;;
        admin) if [[ "$#" -eq 0 ]]; then cli admin --help; else cli admin "$@"; fi ;;
        menu)
            while true; do
                printf '\n=== SuperVIP - Ubuntu ===\n1. Install packages\n2. Start MariaDB\n3. Run server + panel\n4. Build\n5. Check environment\n6. Admin help\n0. Exit\n'
                read -r -p 'Choose: ' choice || return 0
                case "$choice" in
                    0) return 0 ;;
                    1) action install || echo 'Install failed.' >&2 ;;
                    2) action database || echo 'MariaDB failed. Check systemd.' >&2 ;;
                    3) cli web || echo 'Server exited with an error.' >&2 ;;
                    4) cli build || echo 'Build failed.' >&2 ;;
                    5) cli check || echo 'Environment check failed.' >&2 ;;
                    6) cli admin --help || echo 'Admin help failed.' >&2 ;;
                    *) echo 'Invalid choice.' ;;
                esac
            done
            ;;
        *) echo 'Usage: bash ubuntu.sh [menu|install|assets|database|build|check|run|console|admin]' >&2; return 1 ;;
    esac
}
action "$@"
