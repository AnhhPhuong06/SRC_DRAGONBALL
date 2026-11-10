#!/usr/bin/env python3
"""Native Termux setup/menu. No root, proot or downloaded executables."""
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
import time

import server_cli

ROOT = server_cli.ROOT
STATE = ROOT / ".termux"


def tool(name):
    path = shutil.which(name)
    if not path:
        raise RuntimeError("Thieu " + name + ". Chay: bash termux.sh install")
    return path


def database():
    prefix = Path(os.environ["PREFIX"])
    datadir = prefix / "var/lib/mysql"
    STATE.mkdir(mode=0o700, exist_ok=True)
    client = [tool("mariadb"), "--protocol=socket"]
    if not (datadir / "mysql").is_dir():
        subprocess.run([tool("mariadb-install-db"), "--datadir=" + str(datadir)], check=True)
    if subprocess.run([tool("mariadb-admin"), "--protocol=socket", "ping"],
                      stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode:
        with (STATE / "mariadb.log").open("ab") as log:
            subprocess.Popen([tool("mariadbd-safe"), "--datadir=" + str(datadir),
                              "--bind-address=127.0.0.1"], stdin=subprocess.DEVNULL,
                             stdout=log, stderr=log, start_new_session=True)
        for _ in range(60):
            result = subprocess.run([tool("mariadb-admin"), "--protocol=socket", "ping"],
                                    stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            if result.returncode == 0:
                break
            time.sleep(0.5)
        else:
            raise RuntimeError("MariaDB chua san sang. Xem .termux/mariadb.log")
    # Existing Termux packages may initialize root with normal authentication;
    # manually initialized installs may instead authorize the Android app user.
    for candidate in (client, client + ["--user=root"]):
        result = subprocess.run(candidate + ["-N", "-B", "-e", "SELECT @@port"],
                                capture_output=True, text=True)
        if result.returncode == 0:
            return candidate, int(result.stdout.strip())
    raise RuntimeError("Khong dang nhap duoc MariaDB qua socket (user Termux/root). "
                       "Neu da dat mat khau DB, cau hinh data/config/alyra.properties thu cong.")


def setup():
    for name in ("java", "python", "mariadb", "mariadb-install-db", "mariadbd-safe"):
        tool(name)
    server_cli.require_java(server_cli.java_tool("java"))
    config = ROOT / "data/config/alyra.properties"
    for name in ("nro_acc.sql", "nro_data.sql"):
        if not (ROOT / "sql" / name).is_file():
            raise RuntimeError("Thieu sql/" + name)
    print("Tao 2 database MOI: supervip_termux_acc va supervip_termux_data.")
    print("Cau hinh DB hien tai se duoc sao luu; SQL mau co the chua tai khoan/du lieu cu.")
    if input("Nhap TAO MOI de tiep tuc (Enter de huy): ").strip() != "TAO MOI":
        return
    client, port = database()
    names = ("supervip_termux_acc", "supervip_termux_data")
    # Refuse both existing and partially imported databases on repeat setup.
    existing = subprocess.run(client + ["-N", "-B", "-e",
        "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME IN "
        "('supervip_termux_acc','supervip_termux_data')"], capture_output=True, text=True, check=True)
    if existing.stdout.strip():
        raise RuntimeError("Database da ton tai. Khong ghi de. Dung menu Chay server hoac cau hinh DB thu cong.")
    password = secrets.token_hex(24)
    account = "svt_" + secrets.token_hex(4)
    sql = "CREATE USER '" + account + "'@'127.0.0.1' IDENTIFIED BY '" + password + "';\n"
    # MariaDB may match the loopback connection against localhost.
    sql += "CREATE USER '" + account + "'@'localhost' IDENTIFIED BY '" + password + "';\n"
    for name in names:
        sql += "CREATE DATABASE `" + name + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n"
        for host in ("127.0.0.1", "localhost"):
            sql += "GRANT ALL PRIVILEGES ON `" + name + "`.* TO '" + account + "'@'" + host + "';\n"
    subprocess.run(client, input=sql, text=True, check=True)
    for name, dump in zip(names, ("nro_acc.sql", "nro_data.sql")):
        with (ROOT / "sql" / dump).open("rb") as source:
            subprocess.run(client + [name], stdin=source, check=True)
    if config.exists():
        shutil.copy2(config, STATE / ("alyra-before-" + str(time.time_ns()) + ".properties"))
    text = ("database.driver=com.mysql.cj.jdbc.Driver\n"
            "database.host=127.0.0.1\ndatabase.port=" + str(port) + "\n"
            "database.name=" + names[0] + "\ndatabase.name_data=" + names[1] + "\n"
            "database.user=" + account + "\ndatabase.pass=" + password + "\n"
            "database.min=1\ndatabase.max=5\ndatabase.lifetime=1800000\n")
    # Keep required game settings such as server.sv1 when changing DB settings.
    template = config if config.exists() else config.with_name(config.name + ".example")
    original = template.read_text(encoding="utf-8-sig") if template.exists() else ""
    lines = [line for line in original.splitlines() if not re.match(r"\s*database\.", line)]
    config.write_text("\n".join(lines) + "\n" + text, encoding="utf-8")
    config.chmod(0o600)
    print("Da nhap database va ghi cau hinh Termux. Mat khau DB duoc tao ngau nhien.")
    print("Chi nhap SQL goc; cac migration/tinh nang moi can duoc kiem tra rieng.")


def run():
    os.environ.setdefault("JAVA_XMS", "128m")
    os.environ.setdefault("JAVA_XMX", "768m")
    database()
    wake = shutil.which("termux-wake-lock")
    if wake:
        subprocess.run([wake], check=False)
    print("Dang chay server. Trong console, nhap baotri de dung co luu du lieu.", flush=True)
    server_cli.launch("web", [])


def check():
    import platform
    print("Android CPU:", platform.machine())
    print("Thu muc:", ROOT)
    print("PREFIX:", os.environ.get("PREFIX", ""))
    server_cli.require_java(server_cli.java_tool("java"))
    print("Java OK; JAR:", (ROOT / "dist/chienbinhlucy.jar").is_file())
    print("DB config:", (ROOT / "data/config/alyra.properties").is_file())
    print("Log MariaDB:", STATE / "mariadb.log")


def menu():
    while True:
        print("\n=== SuperVIP - Termux Android ===\n"
              "1. Cai database moi va cau hinh\n2. Bat MariaDB\n3. Chay server + web panel\n"
              "4. Build server\n5. Kiem tra moi truong\n6. Huong dan tao admin panel\n0. Thoat")
        choice = input("Chon: ").strip()
        if choice == "0":
            return
        action = {"1": setup, "2": database, "3": run, "4": server_cli.build,
                  "5": check, "6": lambda: server_cli.launch("admin", ["--help"])}.get(choice)
        if action:
            try:
                action()
            except (RuntimeError, OSError, subprocess.CalledProcessError) as error:
                print("Loi:", error, file=sys.stderr)
        else:
            print("Lua chon khong hop le.")


def main():
    if "com.termux" not in os.environ.get("PREFIX", ""):
        raise RuntimeError("Chi dung trong Termux Android goc.")
    # Keep private backups outside shared storage whenever possible.
    if str(ROOT).startswith(("/sdcard/", "/storage/")):
        raise RuntimeError("Hay chep du an vao $HOME/nro trong Termux truoc khi chay.")
    command = sys.argv[1] if len(sys.argv) > 1 else "menu"
    actions = {"menu": menu, "setup": setup, "database": database, "run": run,
               "build": server_cli.build, "check": check,
               "admin": lambda: server_cli.launch("admin", sys.argv[2:] or ["--help"])}
    if command not in actions:
        raise RuntimeError("Lenh khong hop le: " + command)
    actions[command]()


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, subprocess.CalledProcessError) as error:
        print("Loi:", error, file=sys.stderr)
        raise SystemExit(1)
    except (KeyboardInterrupt, EOFError):
        raise SystemExit(0)
