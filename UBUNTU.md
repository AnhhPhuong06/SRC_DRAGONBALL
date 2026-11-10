# SuperVIP - Ubuntu

Bo chay rieng cho Ubuntu 24.04+, Python 3.10+ va JDK 21.
Dat toan bo du an trong thu muc rieng, vi du `~/nro`.

```bash
cd ~/nro
bash ubuntu.sh install
bash ubuntu.sh
```

## Database

`bash ubuntu.sh database` bat MariaDB qua systemd. Neu may khong co systemd, dung trinh quan ly dich vu cua may.
Tao hai database trong va tai khoan DB rieng bang `sudo mariadb`.
Nhap `sql/nro_acc.sql` vao DB tai khoan va `sql/nro_data.sql` vao DB du lieu.
Ap dung cac migration can thiet theo tinh nang va phien ban DB dang dung.

Sua `data/config/alyra.properties`: host, port, name, name_data, user, pass theo DB vua tao.
Neu dung DB remote, bo qua buoc bat MariaDB cuc bo.
Bo Ubuntu khong tu ghi de database hay cau hinh DB hien tai.

## Build / chay

Dung server truoc khi build lai:

```bash
bash ubuntu.sh check
bash ubuntu.sh build
bash ubuntu.sh admin --help
bash ubuntu.sh run
```

`run` chay server va web panel khong can giao dien; `console` dung che do `-nogui`.
Host/cong panel nam trong `data/config/web_admin.properties`, thuong la `http://127.0.0.1:8888`.
Nhap `baotri` trong console de luu va dung server.

Heap mac dinh 256-2048 MB. Tuy chinh:

```bash
JAVA_XMS=512m JAVA_XMX=4096m bash ubuntu.sh run
```

Bo Ubuntu dung apt-get/systemd; khong goi pkg, wake lock hay menu Termux.
Nguon goi: https://packages.ubuntu.com/noble/openjdk-21-jdk-headless.
