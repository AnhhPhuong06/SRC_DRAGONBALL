# SuperVIP - Termux Android

Chay truc tiep tren dien thoai, khong can root/proot/Ubuntu.
Chep toan bo du an vao `$HOME/nro` cua Termux, khong chay tren `/sdcard` hay `/storage`.
Can goi OpenJDK 21 phu hop kien truc dien thoai.

```bash
cd ~/nro
bash termux.sh install
bash termux.sh
```

`install` dung pkg de cai Java 21, Python, MariaDB va mo buoc tao database.
Nhap `TAO MOI` de tao `supervip_termux_acc` va `supervip_termux_data`.
Database da ton tai se bi tu choi ghi de. Tai khoan DB rieng dung mat khau ngau nhien, pool 1-5 ket noi.
Cau hinh cu duoc sao luu trong `.termux/alyra-before-*.properties`.
Chi nhap hai SQL goc; kiem tra/ap dung cac migration can cho tinh nang moi rieng.
Neu da co database day du, huy khoi tao va sua `data/config/alyra.properties` theo DB do.

## Menu

1. Tao database va cau hinh.
2. Bat MariaDB cuc bo.
3. Bat MariaDB va chay server + web panel.
4. Build server.
5. Kiem tra moi truong.
6. Huong dan tao admin panel.
0. Thoat.

Chay truc tiep tung chuc nang:

```bash
bash termux.sh check
bash termux.sh database
bash termux.sh build
bash termux.sh admin --help
bash termux.sh run
```

Dung server truoc khi build lai. Panel dung host/cong trong `data/config/web_admin.properties`, thuong la `http://127.0.0.1:8888` tren trinh duyet dien thoai.
Heap mac dinh 128-768 MB. Doi RAM:

```bash
JAVA_XMS=256m JAVA_XMX=1024m bash termux.sh run
```

Neu dung DB remote, sua cau hinh va chay `JAVA_XMS=128m JAVA_XMX=768m bash run_web_panel.sh` de bo qua MariaDB cuc bo.
Nhap `baotri` trong console de luu va dung; sau do dung `termux-wake-unlock` de tha wake lock.
Bo han che pin cho Termux khi chay lau. Log MariaDB: `.termux/mariadb.log`.
Neu MariaDB da co mat khau admin, tao/import DB thu cong bang tai khoan admin hien tai.

Bo Termux dung pkg, MariaDB truc tiep va wake lock; khong goi apt-get, sudo hay systemctl.
Tham khao menu: https://github.com/KhanhNguyen9872/DragonBoy_Termux.
Nguon goi: https://github.com/termux/termux-packages/tree/master/packages/openjdk-21.
