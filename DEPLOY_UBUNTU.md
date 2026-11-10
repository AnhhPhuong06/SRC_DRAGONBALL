# DragonBall server - VPS Ubuntu

Java 21 server with a headless console and web admin panel. Ubuntu and native Android Termux have separate launchers.

## 1. Clone and install on Ubuntu 24.04+

```bash
sudo apt update
sudo apt install -y git
git clone https://github.com/AnhhPhuong06/SRC_DRAGONBALL.git
cd SRC_DRAGONBALL
bash ubuntu.sh install
```

The installer installs OpenJDK 21, Python and MariaDB, creates `data/config/alyra.properties` from its example and downloads the versioned game asset bundle from this repository's GitHub Release. The SHA-256 checksum is verified before extraction. Existing local config is preserved.

## 2. Create local databases

```bash
bash ubuntu.sh database
sudo mariadb
```

Run the following SQL once, replacing the sample password. Use the same password for both user entries:

```sql
CREATE DATABASE supervip_acc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE supervip_data CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'supervip'@'127.0.0.1' IDENTIFIED BY 'REPLACE_WITH_YOUR_PASSWORD';
CREATE USER 'supervip'@'localhost' IDENTIFIED BY 'REPLACE_WITH_YOUR_PASSWORD';
GRANT ALL PRIVILEGES ON supervip_acc.* TO 'supervip'@'127.0.0.1';
GRANT ALL PRIVILEGES ON supervip_data.* TO 'supervip'@'127.0.0.1';
GRANT ALL PRIVILEGES ON supervip_acc.* TO 'supervip'@'localhost';
GRANT ALL PRIVILEGES ON supervip_data.* TO 'supervip'@'localhost';
EXIT;
```

Import the schema and public game templates into these NEW databases:

```bash
mariadb -h 127.0.0.1 -u supervip -p supervip_acc < sql/nro_acc.sql
mariadb -h 127.0.0.1 -u supervip -p supervip_data < sql/nro_data.sql
nano data/config/alyra.properties
```

Set `database.pass` to your password. Set `server.ip` and `server.sv1` to the VPS address and game port. Review optional migrations in `sql/` against the features you use. The published dumps contain no existing players, accounts, gift codes, purchase records or transactions; they keep schema and selected public game templates.

## 3. Build and initialize the panel

```bash
bash ubuntu.sh check
bash ubuntu.sh build
bash ubuntu.sh admin --init --username admin --host 127.0.0.1 --port 8888
bash ubuntu.sh run
```

The admin initializer asks for a panel password (or generates one) and stores it in the private `data/config/web_admin.properties`. It refuses to overwrite an existing file. Also set a separate random `command_password` in that file before enabling game admin commands.

The panel stays on localhost by default. From your own computer, open an SSH tunnel:

```bash
ssh -L 8888:127.0.0.1:8888 YOUR_VPS_USER@YOUR_VPS_IP
```

Then open `http://127.0.0.1:8888`. Game clients use the game port configured in `alyra.properties` (default 14446). Allow that port in the VPS/provider firewall if remote clients should connect. No script opens firewall ports automatically.

## Operation

Run inside a terminal session manager such as tmux to keep the console after SSH disconnects. In the server console enter `baotri` to save and stop. Stop the server before rebuilding or pulling updates.

```bash
JAVA_XMS=512m JAVA_XMX=2048m bash ubuntu.sh run
```

Runtime files, credentials, generated JARs and logs are ignored by Git. Assets can be downloaded again with `bash ubuntu.sh assets`. Back up your live databases separately; Git does not store player data.

Ubuntu details: [UBUNTU.md](UBUNTU.md). Android phone: [TERMUX.md](TERMUX.md), `bash termux.sh install` then `bash termux.sh`.

The CI workflow builds on Ubuntu with Java 21. This does not prove that your VPS database, firewall or game client configuration is correct.
