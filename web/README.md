# Panel Quản Trị Web

Vỏ giao diện quản trị dạng web cho server, phục vụ bởi
[`WebPanelServer`](../src/nro/server/WebPanelServer.java).
HTML/CSS/JS thuần — **không build, không npm**.

## Chạy panel

Kèm server game (đã nối trong `ServerManager.main`):

```
http://localhost:8888
```

Chạy độc lập:

```bash
java -cp "build/classes;lib/*" nro.server.WebPanelServer 8888
```

> Cổng bận: `WebPanelServer.start(9999)`. Thư mục giao diện: `-Dwebpanel.dir=...`.

Mở thẳng `web/index.html` cũng xem được giao diện — khi đó `PanelAPI` tự fallback
sang `localStorage` và topbar báo "Offline". Chế độ này **không cần đăng nhập** vì
không có API nào để bảo vệ.

## Đăng nhập

Panel bắt đăng nhập trước khi dùng. Chỉ tài khoản có **`isFounder = 1`**
(ADMINISTRATOR) mới vào được — tài khoản VIP, MOD hay thành viên thường đều bị từ chối.

- Dùng đúng tài khoản / mật khẩu đăng nhập game. `GodGK` đăng nhập bằng
  `where username = ? and password = ?` nên mật khẩu trong DB là **chuỗi thô**;
  panel so sánh y như vậy.
- Phiên giữ bằng cookie **`nro_panel_sid`** (`HttpOnly`, `SameSite=Lax`), lưu trong bộ nhớ
  của `WebPanelServer` và sống **12 giờ**, mỗi lần dùng hợp lệ tự gia hạn thêm 12 giờ.
  Tick **Ghi nhớ đăng nhập** thì phiên sống **30 ngày**. Khởi động lại server là mọi phiên
  cũ hết hiệu lực.
- Đăng nhập xong panel mở thẳng trang **Doanh Thu & CSDL**.
- Panel chặn `F12`, `Ctrl+Shift+I/J/C`, `Ctrl+U`, `Ctrl+S` và menu chuột phải. Đây chỉ là
  lớp cản trở cho người dùng phổ thông — người biết kỹ thuật vẫn mở được DevTools bằng
  menu trình duyệt, và dữ liệu thật đã bị chặn ở API bằng `401`. `Ctrl+C` / `Ctrl+V`
  **vẫn dùng được** để bạn dán API Token vào panel.
- Sai quá **5 lần trong 5 phút** thì tạm khoá đăng nhập **60 giây**. Bộ đếm này cũng
  nằm trong bộ nhớ nên khởi động lại server là xoá hết.
- Nút **Đăng Xuất** ở chân sidebar gọi `POST /api/panel/auth?action=logout` rồi quay về
  màn hình đăng nhập. Tên tài khoản đang dùng hiện ở topbar.

Mọi endpoint dưới `/api/panel/` **trừ `auth`** đều trả `401` khi chưa có phiên hợp lệ,
nên không thể gọi API trực tiếp mà bỏ qua đăng nhập. Phía giao diện, `PanelAPI` gặp
`401` sẽ tự đưa người dùng về màn hình đăng nhập (xem `onUnauthorized` trong `js/api.js`)
— nhờ vậy phiên hết hạn giữa chừng cũng không làm panel hiện lỗi lung tung.

### API đăng nhập

| Method | Path | Mô tả |
|--------|------|-------|
| `GET` | `/api/panel/auth` | phiên hiện tại — `{ authenticated, accountId?, username? }`, luôn trả 200 nên không cần quyền |
| `POST` | `/api/panel/auth?action=login` | `{ username, password }` → đặt cookie phiên |
| `POST` | `/api/panel/auth?action=logout` | xoá phiên hiện tại |

Mã lỗi của `login`: `400` thiếu dữ liệu · `401` sai tài khoản/mật khẩu ·
`403` đúng mật khẩu nhưng không phải `isFounder` · `429` đang bị khoá vì sai quá nhiều.

Giới hạn đã biết: `ban` **không** được kiểm tra khi đăng nhập (panel chỉ yêu cầu
`isFounder`), vì `AccountApi` đã chặn khoá một tài khoản quản trị nên trạng thái đó
không tạo ra được từ panel.

## Cấu trúc

```
web/
├── index.html         # shell (sidebar, topbar) + các view
├── css/panel.css      # design token, layout, bảng, modal (sáng/tối)
├── js/api.js          # PanelAPI — điểm vào duy nhất để gọi server
├── js/panel.js        # NAV, router hash, form cấu hình, module giftcode + máy chủ (6 tab)
│                      #   + doanh thu + cửa hàng shop NPC + quản lý nhân vật
│                      #   + quản lý map + quản lý mob + quản lý nội tại + quản lý tài khoản
│                      #   + cấu hình nạp tiền + cài đặt website + DNS Cloudflare
│                      #   + đăng nhập / đăng xuất (chỉ tài khoản isFounder)
├── data/settings.json # cấu hình website
└── uploads/           # logo upload lên
```

## API

| Method | Path                              | Mô tả                                     |
|--------|-----------------------------------|-------------------------------------------|
| GET    | `/api/panel/auth`                 | phiên đăng nhập hiện tại                  |
| POST   | `/api/panel/auth?action=...`      | `login` / `logout`                        |
| GET    | `/api/panel/website`              | cấu hình website (bảng `web_config`) + sao lưu |
| POST   | `/api/panel/website?action=...`   | `save` / `restore` bảng `web_config`           |
| GET    | `/api/panel/cloudflare`           | DNS Cloudflare: zone + bản ghi            |
| POST   | `/api/panel/cloudflare?action=...`| token / zone / bản ghi / purge cache      |
| GET    | `/api/panel/status`               | port, playersOnline, uptime...            |
| GET    | `/api/panel/settings`             | cấu hình website                          |
| POST   | `/api/panel/settings`             | ghi cấu hình website                      |
| POST   | `/api/panel/logo`                 | upload logo (`multipart`, field `file`)   |
| GET    | `/api/panel/item?id=457`          | tra item template → `name`, `icon`        |
| GET    | `/api/panel/item?q=thỏi`          | cả bảng item (`q` rỗng) hoặc lọc theo tên |
| GET    | `/api/panel/option?id=30`         | tra item option template                  |
| GET    | `/api/panel/option?q=tấn`         | tìm / liệt kê option                      |
| GET    | `/api/panel/giftcode`             | danh sách giftcode                        |
| POST   | `/api/panel/giftcode`             | tạo giftcode                              |
| PUT    | `/api/panel/giftcode?id=28`       | cập nhật giftcode                         |
| DELETE | `/api/panel/giftcode?id=28`       | xoá giftcode                              |
| POST   | `/api/panel/giftcode/sync`        | nạp lại giftcode vào server game          |
| GET    | `/api/panel/server`               | thông số máy chủ + người chơi/boss/bot    |
| GET    | `/api/panel/server?action=bots`   | danh sách bot (`group`, `type`, `q`)      |
| POST   | `/api/panel/server?action=...`    | chạy tác vụ thật của server (xem dưới)     |
| GET    | `/api/panel/revenue`              | doanh thu + tài khoản + biểu đồ 7 ngày    |
| POST   | `/api/panel/revenue?action=...`   | cộng/trừ VND, khoá tài khoản, chặn IP clone |
| GET    | `/api/panel/shop`                 | danh sách cửa hàng NPC (+ NPC, loại shop, loại tiền) |
| GET    | `/api/panel/shop?shop=14`         | chi tiết 1 cửa hàng: tab → vật phẩm → option |
| POST   | `/api/panel/shop?action=...`      | thêm/sửa/xoá shop, tab, vật phẩm + đồng bộ server |
| GET    | `/api/panel/player`               | danh sách nhân vật (`q`, `filter`)        |
| GET    | `/api/panel/player?id=2`          | chi tiết 1 nhân vật + bảng tra để dựng select |
| POST   | `/api/panel/player?action=...`    | sửa chỉ số / tiền / đệ tử / nhiệm vụ / JSON thô |
| GET    | `/api/panel/map`                  | danh sách bản đồ (`q`, `type`, `planet`)  |
| GET    | `/api/panel/map?id=5`             | chi tiết 1 bản đồ: cổng dịch chuyển, quái, NPC |
| POST   | `/api/panel/map?action=...`       | sửa bản đồ / cổng dịch chuyển / quái / NPC |
| GET    | `/api/panel/mob`                  | danh sách mẫu quái (`q`, `type`)          |
| GET    | `/api/panel/mob?id=1`             | chi tiết 1 mẫu quái + bản đồ đang dùng    |
| POST   | `/api/panel/mob?action=...`       | thêm / sửa mẫu quái (không có xoá)        |
| GET    | `/api/panel/intrinsic`            | danh sách nội tại (`q`, `gender`)         |
| GET    | `/api/panel/intrinsic?id=5`       | chi tiết 1 nội tại + ai đang mang         |
| POST   | `/api/panel/intrinsic?action=...` | thêm / sửa / xoá / gán nội tại            |
| GET    | `/api/panel/account`              | danh sách tài khoản (`q`, `filter`)       |
| GET    | `/api/panel/account?id=112415`    | chi tiết 1 tài khoản + nhân vật của nó    |
| POST   | `/api/panel/account?action=...`   | tạo / sửa / xoá tài khoản, mật khẩu, ví, kick |

### Trang "Máy Chủ Trực Tuyến"

`GET /api/panel/server` trả về: `players`, `ramUsedMb`/`ramTotalMb`, `cpuPercent`,
`threads`, `sessions`, `uptimeText`, `expRate`, `bosses{alive,dead,resting}`,
`giftcodes`, `consignItems`, `autoMaintenance` + `autoMaintenanceTime`,
`autoSave`, `currencyInt`, `event{id,name}` + `events[]`, `botStats`, và 2 danh sách
`playerList` / `bossList` (mỗi boss có `status` ACTIVE/AFK/DEAD, `mapName`, `zoneId`).
Panel gọi lại mỗi 3 giây khi đang mở trang này.

Mỗi dòng người chơi có `mapName`, `zoneId`, `task` (nhiệm vụ chính đang làm, đúng nguồn
MenuUI dùng) và `power`; panel lọc theo tên ở client và cho kick từng người.

Mỗi dòng boss còn có `head`/`body`/`leg` (đúng `BossData.outfit`), `hpMax`, `dame` và
`hasAvatar`. Panel lấy `head` làm avatar: nếu có file `web/assets/boss/<head>.png`
thì hiện ảnh, chưa có thì hiện vòng tròn chữ cái đầu — xem `web/assets/boss/README.md`.
Server không có sẵn ảnh head (ảnh nhân vật nằm trong client), nên đây là chỗ để thả ảnh vào.

Danh sách bot **không** nằm trong gói trên vì server có thể có hàng nghìn bot;
tab Quản Lý Bot gọi riêng `GET /api/panel/server?action=bots&group=&type=&q=`,
trả về `{ total, shown, limit, rows[] }` (tối đa 300 dòng mỗi lần, dòng có cả
HP/MP, bản đồ, khu, tọa độ, trạng thái).

`POST /api/panel/server?action=<tên>` — tất cả đều gọi đúng hàm có sẵn của server:

| action | Việc thực hiện |
|--------|----------------|
| `reload-giftcodes` | `GiftCodeManager.gI().reload()` |
| `reload-shops` | `Manager.SHOPS = ShopDAO.getShops(con)` |
| `reload-npcs` | nạp lại `npc_template` vào `Manager.NPC_TEMPLATES` |
| `reload-top` | `TopServer.LoadingTop()` |
| `clean-trash` | đóng session không còn người chơi (nút *Dọn Dẹp Phần Rác*) |
| `reward-top` | trao hồng ngọc/ngày cho cao thủ Siêu Hạng đang online |
| `respawn-boss` | `BossManager.gI().respawnAllRestingBosses()` |
| `reset-boss` | `BossManager.gI().resetAllBosses()` |
| `boss-summon` | body `{"id": -100001}` → hồi sinh boss đang nghỉ/chết, chưa có thì `BossManager.createBoss` |
| `kick-all` | kick toàn bộ người chơi đang online (`Client.kickSession`) |
| `kick` | body `{"id": 12}` → kick 1 người chơi đang online |
| `bot-create` | body `{"type": 0, "quantity": 10}` → `NewBot.runBot(type, null, null, quantity)` (type 0 cổ điển, 2 đánh người) |
| `bot-delete` | body `{"group":"classic","type":0,"keyword":"harry"}` hoặc `{"group":"classic","id":1900000001}` |
| `exp-rate` | body `{"rate": 7}` → đổi `Manager.RATE_EXP_SERVER` + thông báo toàn server |
| `set-event` | body `{"id": 3}` → `EventManager.setCurrentEvent` (0-10, tự nạp lại dữ liệu sự kiện) |
| `autosave` | body `{"on": true}` → `HoannganhManager.startAutoSave/stopAutoSave` |
| `maintenance-now` | body `{"seconds": 60}` → `Maintenance.gI().start(seconds)` |
| `auto-maintenance` | body `{"on": true}` → bật/tắt `AutoMaintenance.AutoMaintenance` |
| `auto-maintenance-time` | body `{"hour": 23, "minute": 59, "on": true}` → `AutoMaintenance.setTime` |
| `currency` | body `{"int": true}` → `Manager.readInt` (int tối đa 2 tỷ / long) |

Các action có body JSON: `boss-summon`, `kick`, `bot-create`, `bot-delete`, `exp-rate`,
`set-event`, `autosave`, `maintenance-now`, `auto-maintenance`, `auto-maintenance-time`,
`currency`. Còn lại gửi body rỗng.

### Trang "Doanh Thu & CSDL"

`GET /api/panel/revenue` trả về:

- `accounts`: `total`, `today`, `week` (số tài khoản tạo mới), `mtv`, `locked`, `jailed`,
  `founders`, `admins`, `totalTopup`, `topupAccounts` (số tài khoản đã nạp) — đều đếm trên bảng `account`.
  Nếu đọc bảng `account` lỗi thì có thêm `accounts.error` và panel hiện cảnh báo dưới biểu đồ.
- `revenue`: `total`, `today`, `week`, `month`, `weekLabel`, `fromRecharge`.
- `chart`: 7 phần tử `{ day, date, revenue, accounts }` (cũ → mới) để vẽ biểu đồ.
- `topupToday`: `{ count, rows[] }` — `rows` có `username`, `source`, `amount`, `status`, `time`.
- `recharge`: `{ ready, rows, successRows, successStatus, statuses[] }` — tình trạng bảng nạp.
- `clones`: `[{ ip, accounts }]` — IP có hơn `cloneLimit` tài khoản.

Nguồn số liệu (không bịa bảng mới):

| Chỉ số | Nguồn |
|--------|-------|
| Tổng tài khoản / đăng ký mới | `COUNT(*)` và `accountCreatedTimes` của `account` |
| Thành viên (MTV) | `SUM(active = 1)` — đúng cột `active` mà `PlayerService.MoThanhVienPlayer` ghi |
| Tài khoản bị khoá | `SUM(ban <> 0)` — đúng cột `ban` mà `PlayerService.KhoaTaiKhoan` ghi |
| Tổng doanh thu | `SUM(account.tongnap)` — số nạp tích luỹ từng tài khoản, **không giới hạn** |

Về giới hạn của tổng doanh thu:

- `SUM()` của MySQL trả về kiểu số thập phân nên **tổng không bị chặn**; panel đọc bằng `long`.
- Trần duy nhất từng có là cột `account.tongnap` để `int(11)` (tối đa 2.147.483.647) nên mỗi
  tài khoản chỉ nạp tới đó. Panel tự nâng cột này lên `bigint(20)` một lần khi chạy
  (`RevenueApi.ensureRevenueSchema`, in log khi có thay đổi) — không cần sửa SQL bằng tay.
  `sql/nro.sql` cũng đã để `tongnap bigint(20)` cho cài đặt mới.
- Phía game, `MySession.tongnap` là `long` và `GodGK` đọc cột bằng kiểu số tổng quát,
  nên đọc được cả khi cột còn `int` lẫn khi đã là `bigint`.
- Cột `account.vnd` (ví VND dùng cho nút Cộng/Trừ VND) vẫn là `int` và panel giữ kẹp trần
  2.000.000.000 đúng như game (`MySession.vnd` là `int`) — muốn bỏ luôn thì phải đổi cả
  `MySession.vnd` sang `long` trong toàn bộ source.
| Doanh thu theo ngày | bảng `cvh_recharge` (website ghi: `amount`, `amount_real`, `type`, `status`, `time`) |

Lưu ý về `cvh_recharge`:

- `time` là cột `varchar` nên `RevenueApi` đọc thô rồi tự phân tích (hỗ trợ epoch giây/mili
  giây và các dạng `yyyy-MM-dd HH:mm[:ss]`, `dd/MM/yyyy HH:mm[:ss]`).
- Chỉ dòng có `status = RevenueApi.RECHARGE_SUCCESS` (mặc định **1**) được tính vào doanh thu.
  Nếu website của bạn dùng mã trạng thái khác thì sửa đúng hằng số này trong
  `src/nro/server/webpanel/RevenueApi.java`.
- Số tiền lấy `amount_real` nếu > 0, ngược lại lấy `amount`.
- Chưa có bảng/không đọc được thì `recharge.ready = false` và panel ghi rõ
  "chưa đọc được bảng cvh_recharge" thay vì hiện số sai.

`POST /api/panel/revenue?action=<tên>` — các nút trên trang gọi đúng nghiệp vụ đang có trong source:

| action | body | Việc thực hiện |
|--------|------|----------------|
| `money-add` | `{"username":"abc", "amount":50000}` | `account.vnd = LEAST(vnd + ?, 2 tỷ)` + cập nhật ví phiên đang chơi |
| `money-sub` | `{"username":"abc", "amount":50000}` | `account.vnd = GREATEST(vnd - ?, 0)` + cập nhật ví phiên đang chơi |
| `account-mtv-on` | `{"username":"abc"}` | `active = 1` (như `MoThanhVienPlayer`) |
| `account-mtv-off` | `{"username":"abc"}` | `active = 0` (như `XoaThanhVienPlayer`) |
| `account-lock` | `{"username":"abc"}` | `ban = 1` (như `KhoaTaiKhoan`) |
| `account-unlock` | `{"username":"abc"}` | `ban = 0` |
| `account-admin-on` | `{"username":"abc"}` | `isFounder = 1` — quyền ADMINISTRATOR |
| `account-admin-off` | `{"username":"abc"}` | `isFounder = 0` |
| `account-quantri-on` | `{"username":"abc"}` | `isQuanTriVien = 1` — quyền Quản Trị Viên |
| `account-quantri-off` | `{"username":"abc"}` | `isQuanTriVien = 0` |
| `ip-ban` | `{"ip":"192.168.1.1"}` | `ban = 1` cho mọi tài khoản cùng IP |
| `ip-unban` | `{"ip":"192.168.1.1"}` | `ban = 0` cho mọi tài khoản cùng IP |
| `clone-ban` | `{"limit":5}` | khoá tài khoản thuộc mọi IP có hơn `limit` tài khoản |

Quy tắc an toàn:

- `ip-ban` và `clone-ban` **bỏ qua** tài khoản `isFounder = 1` và `isQuanTriVien = 1`;
  `account-lock` từ chối khoá tài khoản quản trị.
- Khoá/mở thành viên, cấp/thu quyền quản trị đều tự ngắt kết nối nhân vật đang online
  (`Client.kickSession`) để lần đăng nhập sau đọc lại quyền mới — giống hành vi của `PlayerService`.
- Cấp quyền admin dùng đúng cột game đọc khi đăng nhập: `isFounder` (ADMINISTRATOR) và
  `isQuanTriVien` (Quản Trị Viên). Lưu ý bản source cũ ghi vào 2 cột **không tồn tại**
  (`account.admin`, `account.isQUANTRI`) ở `PlayerService.CapQuyenKeyController` /
  `CapQuyenAdmin` / `HuyQuyen...` nên chức năng cấp quyền trong game không chạy — đã sửa lại
  thành `isQuanTriVien` và `isFounder`.
- Panel gọi lại API mỗi 15 giây khi đang mở trang; biểu đồ chỉ vẽ lại khi số liệu đổi.

### Trang "Cửa Hàng Shop NPC"

Dữ liệu nằm ở 4 bảng, đúng thứ tự mà `ShopDAO.getShops` đọc:

```
shop ─┬─ tab_shop ─── item_shop ─── item_shop_option
      └─ npc_id (FK) → npc_template.id
```

`GET /api/panel/shop` (không tham số) trả về:

- `shops[]`: `{ id, npcId, npcName, tagName, typeShop, typeShopName, tabCount, itemCount }`
  — sắp theo `npc_id` rồi `id`, đúng thứ tự server nạp.
- `npcs[]`: toàn bộ `npc_template` (`id`, `name`) để chọn NPC khi tạo shop.
- `typeShops[]` / `typeSells[]`: bảng tên cho 2 cột mã, xem bên dưới.

`GET /api/panel/shop?shop=<id>` trả về `{ shop: { id, npcId, npcName, tagName, typeShop,
typeShopName, tabs[] } }`, mỗi tab là `{ id, shopId, name, itemCount, items[] }`, mỗi vật phẩm là:

| Trường | Nguồn |
|--------|-------|
| `id` / `tabId` / `tempId` | `item_shop.id` / `tab_id` / `temp_id` |
| `name`, `icon` | `item_template` theo `temp_id` (`icon` = `iconID`, không phải `Item.id`) |
| `isNew`, `isSell` | `is_new`, `is_sell` — panel hiện **cả** dòng `is_sell = 0` để bật/tắt lại |
| `typeSell`, `typeSellName` | `type_sell` + tên loại tiền |
| `cost`, `costGold` | `cost`, `costgold` |
| `iconSpec`, `specName` | `icon_spec` (là **iconID**) + tên vật phẩm có icon đó (`ItemService.getItemIdByIcon`) |
| `options[]` | `item_shop_option` → `{ id, param, name, text }`, `text` = tên option thay `#` bằng `param` (giống `ItemOption.getOptionString`) |
| `unitName` | đơn vị giá hiển thị, xem bảng loại shop |

Tab có tên chứa `<>` là **xuống dòng** (`ShopDAO.loadShopTab` đổi `<>` → `\n`);
API trả về tên đã xuống dòng và tự đổi ngược lại khi ghi, nên trong modal chỉ cần gõ nhiều dòng.

Hai cột mã được dịch theo đúng hằng số trong `nro.shop.ShopService`:

| `shop.type_shop` | Tên | Cách tính giá khi mua |
|------------------|-----|------------------------|
| 0 | Shop thường | `type_sell` quyết định trừ vàng / ngọc / hồng ngọc |
| 1 | Shop học kỹ năng | server tự tính theo tiềm năng kỹ năng, **không** dùng `cost` |
| 2 | Shop điểm | trừ `cost` điểm tích luỹ |
| 3 | Shop đặc biệt | trừ `cost` lần vật phẩm có `iconID = icon_spec` |

| `item_shop.type_sell` | Tên |
|-----------------------|-----|
| 0 | Vàng |
| 1 | Ngọc Xanh |
| 2 | Ngọc Đỏ |
| 3 | Hồng Ngọc |
| 4 | Sự kiện |

`POST /api/panel/shop?action=<tên>`:

| action | body | Việc thực hiện |
|--------|------|----------------|
| `shop-create` | `{"npcId":21,"tagName":"BUA_1H","typeShop":3}` | thêm shop, trả `id` vừa tạo |
| `shop-update` | `{"id":6,"npcId":21,"tagName":"BUA_1H","typeShop":3}` (trường nào bỏ qua thì giữ nguyên) | sửa shop |
| `shop-delete` | `{"id":6}` | xoá shop + toàn bộ tab, vật phẩm, option (1 transaction) |
| `tab-create` | `{"shopId":6,"name":"Sách\nVỏ"}` | thêm tab |
| `tab-update` | `{"id":14,"name":"Bùa\n1 giờ"}` | đổi tên tab |
| `tab-delete` | `{"id":14}` | xoá tab + vật phẩm bên trong |
| `item-create` | `{"tabId":14,"tempId":93,"cost":1,"costGold":0,"typeSell":1,"iconSpec":4028,"isNew":true,"isSell":true,"options":[{"id":50,"param":160}]}` | thêm vật phẩm |
| `item-update` | như trên + `id` (gửi `options` là **thay toàn bộ** option của vật phẩm) | sửa vật phẩm |
| `item-delete` | `{"id":700}` | xoá vật phẩm + option |
| `sync` | (không cần body) | `Manager.SHOPS = ShopDAO.getShops(con)` |

Quy tắc:

- Mọi action ghi đều tự gọi lại `Manager.SHOPS` sau khi commit nên game thấy thay đổi
  ngay; nút **Đồng Bộ Server** chỉ là cách chạy lại thủ công (`sync`).
- Tạo/sửa/xoá nhiều bảng đều nằm trong 1 transaction (`ShopApi.inTransaction`), lỗi thì rollback.
- Validate trước khi ghi: NPC phải có trong `npc_template` (bảng `shop` có FK tới đó),
  vật phẩm phải có trong `item_template`, tên tab/tag không được rỗng, `typeShop` kẹp 0-3,
  `typeSell` kẹp 0-4.
- `ConnectResultSet` ép kiểu thô nên `ShopApi` đọc số qua `getObject` + `number(...)`
  (giống `RevenueApi`): `COUNT(*)` trả `Long` còn `tinyint(1)` trả `Boolean`. Vì
  `ResultSetMetaData.getColumnName()` trả **tên cột gốc**, các câu `COUNT(*)` đặt alias
  phải đọc **theo vị trí** — đặt alias rồi đọc theo tên sẽ ra 0.
- Trên trang, bấm đúp vào tên tab để đổi tên; nút **Xóa Tab** xoá tab đang mở.

### Trang "Quản Lý Nhân Vật"

Bảng `player` lưu gần như toàn bộ dữ liệu dưới dạng **JSON trong cột TEXT**, nên panel
không bao giờ dựng lại cả dòng: mỗi lần sửa chỉ đọc chuỗi JSON ra, thay đúng ô cần đổi
rồi ghi lại — nhờ vậy các ô còn lại không bị mất (xem `PlayerApi`).

**Thứ tự các ô** (theo `PlayerDAO.updatePlayer` và `GodGK`):

| Cột | Ý nghĩa |
|-----|---------|
| `data_inventory` | `[vàng, ngọc xanh, hồng ngọc]` |
| `data_location` | `[mapId, x, y]` |
| `data_point` | `[limitPower, sức mạnh, tiềm năng, thể lực, thể lực tối đa, HP gốc +%, KI gốc +%, sát thương +%, giáp +%, chí mạng +%, (ô trống), HP, KI, sát thương, HP tối đa, KI tối đa]` |
| `data_magic_tree` | `[cấp, số đậu, đang nâng cấp 0/1, lần thu hoạch, lần nâng cấp]` |
| `data_intrinsic` | `[id, tham số 1, tham số 2, số lần mở, đang bật, skillID, cooldown, lần dùng cuối (ms)]` |
| `data_charm` | 21 mốc hết hạn (ms), thứ tự theo `nro.player.Charms.addTimeCharms` |
| `skills` | mảng `"[templateId, điểm, lần dùng cuối, cấp]"` |
| `skills_shortcut` | 10 id kỹ năng của ô dùng nhanh (`-1` = không gán) |
| `pet` | `[]` = không có đệ tử, ngược lại là `[petInfo, petPoint, petBody, petSkill]` |
| `data_task` | `[taskMainId, index, số lượng, lần cập nhật cuối]` |
| `data_side_task`, `data_clan_task` | `[templateId, nhận lúc, đã làm, tối đa, còn lại, cấp]` |

`GET /api/panel/player?q=&filter=online|offline` → danh sách nhân vật (`JOIN account` để
lấy `username` / `ban`): `{ players[], shown, total, limit, online, readInt }`. Mỗi dòng:
`id, name, accountId, username, gender, genderName, head, power, rank, pointPvp, gold, gem,
ruby, online, banned, createTime`. Bảng chỉ trả tối đa `LIST_LIMIT = 200` dòng/lần nên
khi `total > shown` panel nhắc dùng ô tìm kiếm để lọc bớt.

`GET /api/panel/player?id=<id>` → `{ player, catalog }`.

- `player`: `info` (thông tin cơ bản + `createTime`, `banned`), `online`, `point`,
  `inventory`, `location`, `magicTree`, `intrinsic`, `skills[]`, `shortcut[10]`, `pet`,
  `task`, `sideTask`, `clanTask`, `raw` (12 cột JSON dạng chuỗi).
- `catalog`: dữ liệu để dựng select — `skills` (chỉ kỹ năng **của đúng hệ** nhân vật, vì
  `skill_template` dùng khoá `(nclass_id, id)`), `shortcutSlots`, `intrinsics`,
  `taskMains`, `sideTasks`, `charms[{pos, field, itemId, itemName, time}]`,
  `pointFields`, `rawColumns`, `readInt`, `maxStat`, `genders`.

`POST /api/panel/player?action=<tên>`, body luôn có `id`:

| action | body | Việc thực hiện |
|--------|------|----------------|
| `basic` | `{name, gender, head, rank, pointPvp}` | sửa thông tin cơ bản (chặn tên trùng) |
| `point` | `{power, tiemNang, hp, mp, dame, ...}` | sửa các ô của `data_point` gửi lên |
| `max-stats` | — | đặt HP / KI / sát thương và mức tối đa = `Integer.MAX_VALUE` |
| `inventory` | `{gold, gem, ruby}` | sửa tiền tệ |
| `location` | `{mapId, x, y}` | sửa vị trí (`x`, `y` kẹp 0–3000) |
| `magic-tree` | `{level, peas, upgrading}` | sửa đậu thần |
| `intrinsic` | `{intrinsicId, param1, param2, countOpen, lastTimeUseSkill}` | sửa nội tại; id phải có trong bảng `intrinsic` |
| `intrinsic-clear` | — | xoá nội tại (4 ô đầu = 0) |
| `cooldown-clear` | — | xoá hiệu ứng chờ của kỹ năng nội tại (4 ô sau) |
| `charms` | `{charms:[{pos, time}]}` | sửa các ô bùa được gửi lên; không gửi gì thì báo lỗi |
| `shortcut` | `{slots:[...]}` | ghi lại cả 10 ô dùng nhanh (thiếu thì điền `-1`) |
| `skills` | `{skills:[{point, currLevel}]}` | chỉ sửa điểm / cấp của kỹ năng đã có, giữ nguyên độ dài mảng |
| `pet` | `{name, typeDeTu, status, thuctinh, limitPower, power, hp, mp, dame, ...}` | sửa đệ tử |
| `pet-delete` | — | xoá đệ tử (ghi `pet = "[]"`) |
| `task` | `{taskMainId, index, count, lastTime}` | sửa nhiệm vụ chính |
| `side-task`, `clan-task` | `{templateId, receivedTime, count, maxCount, leftTask, level}` | sửa nhiệm vụ hàng ngày / bang |
| `raw` | `{column, value}` | ghi lại nguyên cột JSON (chỉ nhận 12 cột trong `RAW_COLUMNS`, phải là JSON hợp lệ) |
| `kick` | — | ngắt kết nối nhân vật đang online (không ghi gì) |

Quy tắc:

- **Nhân vật đang online**: panel **kick trước rồi mới ghi DB**. Vì
  `Client.kickSession → remove(session) → dispose(player) → PlayerDAO.updatePlayer`, tức là
  dữ liệu trong bộ nhớ được ghi xuống trước; nếu ghi DB trước thì server sẽ ghi đè lại.
  Phản hồi khi đó có thêm `kicked: true` và câu nhắc trong `message`.
- Mọi cột JSON đều ghi qua `padded(raw, size)` (thiếu ô thì thêm 0, thừa thì cắt) rồi
  `writeArray` — chỉ đụng đúng cột cần sửa, không dựng lại cả dòng.
- Modal có 4 tab: **Nhân Vật** (thông tin, chỉ số, tiền tệ, vị trí & đậu thần),
  **Đệ Tử**, **Nâng Cao** (nội tại, kỹ năng, 10 ô dùng nhanh, bùa, JSON thô) và
  **Nhiệm Vụ Mở Rộng** (nhiệm vụ chính / hàng ngày / bang).
- Tab Nâng Cao có nút **Lưu Dữ Liệu Nâng Cao** gom nội tại + ô dùng nhanh + bùa vào
  **một lần nạp lại**, vì lưu từng phần riêng sẽ vẽ lại danh sách bùa và làm mất các ô
  chưa kịp lưu.
- Ô bùa để trống (bấm **Xóa**) được ghi thành mốc `0` = đã hết hạn.
- `datetime-local` đổi qua lại với mili giây bằng `msToLocal` / `localToMs` trong
  `panel.js`; `data_task.lastTime`, `data_intrinsic[7]` và `data_charm` đều là mili giây.

### Trang "Quản Lý Map"

Bảng `map_template` có 248 dòng (`id` 0–248, **có khoảng trống**). `id` chính là chỉ số
tên map mà client dùng, nên panel **không thêm và không xoá bản đồ** — chỉ sửa thông tin,
cổng dịch chuyển, quái và NPC của bản đồ đang có (xem `MapApi`).

Ba cột danh sách lưu dạng chuỗi trong TEXT, mỗi phần tử là một mảng JSON:

| Cột | Mỗi phần tử |
|-----|-------------|
| `waypoints` | `[tên, minX, minY, maxX, maxY, vào, offline, map đến, x đến, y đến]` |
| `mobs` | `[tempId, level, hp, x, y]` |
| `npcs` | `[npcId, x, y]` |

`waypoints` trong DB tồn tại **hai dạng**: dạng bọc chuỗi
`["[\"Tên\",0,360,…]", …]` và dạng mảng thường `[["Tên",…], …]`. `Manager.loadDatabase`
chuẩn hoá bằng đúng ba lệnh `.replaceAll("[\"[", "[[")`, `.replaceAll("]\"]", "]]")`,
`.replaceAll("\",\"", ",")`; `MapApi.normalize()` lặp lại y hệt ba lệnh đó nên cả hai dạng
đọc/ghi đều an toàn. Cột `data` giữ nguyên chuỗi `[0,0,0,1,0]` và **không cho sửa** vì
không có đoạn code game nào đọc nó.

`GET /api/panel/map?q=&type=&planet=` → danh sách: `{ maps[], shown, total, limit: 400,
types[], planets[], genders[] }`. Mỗi dòng: `id, name, zones, maxPlayer, type, typeName,
planetId, planetName, bgType, tileId, bgId, genderType, genderName, mobCount, npcCount,
waypointCount`.

`GET /api/panel/map?id=<id>` → `{ map, catalog }`.

- `map`: thông tin bản đồ + `data`, `waypoints[]`, `mobs[]` (`tempName`, `tempHp`) và
  `npcs[]` (`npcName`) đã tách sẵn.
- `catalog`: `mobs[]`, `npcs[]`, `waypointTargets[]`, `types[]`, `planets[]`, `genders[]`,
  `maxZones: 120`, `maxPlayer: 120`.

`POST /api/panel/map?action=<tên>`, body có `id`:

| action | body | Việc thực hiện |
|--------|------|----------------|
| `map-update` | `{name?, zones?, maxPlayer?, type?, planetId?, bgType?, tileId?, bgId?, genderType?}` | sửa thông tin bản đồ, chỉ ghi ô được gửi lên |
| `waypoints` | `{waypoints:[…]}` | **thay toàn bộ** danh sách cổng dịch chuyển |
| `spawns` | `{mobs?:[…], npcs?:[…]}` | **thay toàn bộ** danh sách quái / NPC được gửi |
| `reload` | — | dựng lại `Manager.MAP_TEMPLATES` |

Quy tắc:

- Id `0` là bản đồ hợp lệ (`requireMap` chỉ từ chối `id < 0`).
- Kẹp giá trị: `zones` / `max_player` 1–120, `type` 0–14, `planet_id` 0–2,
  `bg_type` / `tile_id` / `bg_id` 0–127, `genderType` −1…3, tên bản đồ 1–40 ký tự.
- Tên cổng 1–32 ký tự và **không được chứa `"` `\` `[` `]`** vì cách nạp của game thay
  thế chuỗi thô, các ký tự này sẽ làm hỏng cột `waypoints`.
- `goMap` của cổng phải là bản đồ có thật; `tempId` phải có trong `mob_template`;
  `npcId` phải có trong `npc_template`.
- `reload` chỉ đổi dữ liệu cho bản đồ khởi động sau; bản đồ đang chạy trong game vẫn giữ
  bản cũ cho tới khi khởi động lại.

### Trang "Quản Lý Mob"

Bảng `mob_template` có 127 dòng, `id` 0–126 **liên tục, không có khoảng trống**. Cột:
`id, TYPE, NAME, hp, range_move, speed, dart_type, percent_dame, percent_tiem_nang,
percent_gold`. `TYPE` chỉ nhận 3 giá trị:

| TYPE | Tên hiển thị |
|------|--------------|
| 0 | Đặc biệt (không tấn công) |
| 1 | Quái mặt đất |
| 4 | Quái bay |

**Không có chức năng xoá.** `DataGame.updateMap` gửi danh sách quái xuống client **không
kèm id**, client đánh số mẫu quái theo đúng vị trí trong danh sách — xoá một dòng ở giữa
sẽ làm mọi quái phía sau hiển thị sai tên và HP. Vì vậy panel chỉ cho thêm và sửa.

`GET /api/panel/mob?q=&type=` → `{ mobs[], shown, total, limit: 1000, types[], nextId }`.
Mỗi dòng: `id, type, typeName, name, hp, rangeMove, speed, dartType, percentDame,
percentTiemNang, percentGold, mapCount` (`mapCount` = số bản đồ đang đặt mẫu quái này).

`GET /api/panel/mob?id=<id>` → `{ mob, catalog }`; `mob` có thêm `maps[{name}]`, `catalog`
có `types[]`, `nextId`, `maxHp`, `charts[]`.

`POST /api/panel/mob?action=<tên>`:

| action | body | Việc thực hiện |
|--------|------|----------------|
| `mob-create` | `{name, type, hp, rangeMove, speed, dartType, percentDame, percentTiemNang, percentGold}` | thêm mẫu quái mới |
| `mob-update` | `{id, …}` | sửa mẫu quái |
| `reload` | — | dựng lại `Manager.MOB_TEMPLATES` (sắp theo `id`) |

Quy tắc:

- `mob-create` **chỉ nhận đúng `id = maxId + 1`**. Gửi id khác sẽ báo lỗi, tránh tạo
  khoảng trống trong dãy id mà client đang dựa vào.
- Tên quái 1–50 ký tự.

### Trang "Quản Lý Nội Tại"

Bảng `intrinsic` có 27 dòng, `id` 0–26. Cột: `id, NAME, param_from_1, param_to_1,
param_from_2, param_to_2, icon, gender`.

- `id` phía game đọc bằng `getByte("id")` nên panel giới hạn **0–127**.
- `gender`: 0 Trái Đất, 1 Namếc, 2 Xayda, 3 cả ba hệ.
- `NAME` dùng chỗ dựng `p0` `p1` `p2` `p3`; game thay lần lượt bằng `param_from_1`,
  `param_to_1`, `param_from_2`, `param_to_2` khi hiện cho người chơi. Panel hiện cả chuỗi
  gốc và bản đã thay số, dựng theo đúng `Intrinsic.getDescription()`.
- `icon` là id icon trong `data/icon/x1/<icon>.png`; panel xem trước ảnh ngay khi nhập.

`GET /api/panel/intrinsic?q=&gender=` → `{ intrinsics[], shown, total, genders[], nextId,
players[] }`. Mỗi dòng: `id, name, shortName, description, paramFrom1, paramTo1,
paramFrom2, paramTo2, icon, gender, genderName, playerCount`.

`GET /api/panel/intrinsic?id=<id>` → `{ intrinsic, catalog }`; `intrinsic` có thêm
`players[{id, name, param1, param2}]`, `catalog` có `genders[]`, `nextId`, `players[]`.

`POST /api/panel/intrinsic?action=<tên>`:

| action | body | Việc thực hiện |
|--------|------|----------------|
| `intrinsic-create` | `{name, paramFrom1, paramTo1, paramFrom2, paramTo2, icon, gender}` | thêm nội tại |
| `intrinsic-update` | `{id, …}` | sửa nội tại |
| `intrinsic-delete` | `{id}` | xoá nội tại |
| `intrinsic-assign` | `{playerId, intrinsicId}` | gán nội tại cho một nhân vật |
| `reload` | — | dựng lại cả 4 danh sách `Manager.INTRINSICS` / `INTRINSIC_TD` / `INTRINSIC_NM` / `INTRINSIC_XD` |

Quy tắc:

- Không xoá được **nội tại #0** (ô "chưa mở" của mọi nhân vật).
- Không xoá được nội tại mà có nhân vật đang mang (`player.data_intrinsic[0]` bằng id đó);
  thông báo lỗi liệt kê rõ ai đang giữ.
- `intrinsic-assign` **kick nhân vật trước** nếu đang online rồi mới ghi, và đặt
  `data_intrinsic` thành `[id, param_from_1, param_from_2, …]` (đệm đủ 8 ô). Panel chỉ
  liệt kê ai đang mang nội tại; việc gán cho nhân vật làm ở trang "Quản Lý Nhân Vật".
- Tham số nhận giá trị `short` (−32768…32767) vì game đọc bằng `getShort`.

### Trang "Quản Lý Tài Khoản"

Bảng `account` (`id, username, password, email, accountCreatedTimes, accountAgeDays,
update_time, isFounder, is_jail, ban, point_post, last_post, role, isQuanTriVien, Vip,
last_time_login, last_time_logout, ip_address, active, thoi_vang, server_login, bd_player,
tongnap, vnd, coin, danap, mkc2`).

Lưu ý quan trọng:

- **Mật khẩu lưu dạng chuỗi thô, không băm.** `GodGK` đăng nhập bằng
  `where username = ? and password = ?`, nên panel ghi mật khẩu y nguyên và cảnh báo ngay
  trên giao diện.
- Cột `email` là **object JSON của website** (`{"email":"","verify":"false","code":"",
  "timecode":""}`) và không đoạn code Java nào đọc; khi tạo tài khoản mới panel ghi đúng
  khuôn mẫu đó và từ chối nếu quản trị nhập chuỗi không phải object JSON.
- `active` = trạng thái "Mở Thành Viên". Tài khoản do panel tạo có `active = 1`, giống hệt
  tài khoản game tự đăng ký (`Service` chỉ ghi `username`/`password` nên cột nhận default
  của bảng). Quản trị có thể tắt lại ở tab "Quyền & Ví".
- Không đặt được `ban = true` khi tài khoản còn `isFounder` hoặc `isQuanTriVien`.

`GET /api/panel/account?q=&filter=online|offline|banned|jail|admin|member` →
`{ accounts[], shown, total, online, filters[] }`. Mỗi dòng: `id, username, charCount,
online, ban, isJail, isFounder, isQuanTriVien, active, vip, role, tongnap, vnd, coin,
danap, thoiVang, ipAddress, lastLogin, lastLogout, created`.

`GET /api/panel/account?id=<id>` → `{ account, characters[], catalog }`.

- `account`: toàn bộ cột ở trên (kể cả `password`, `email`, `mkc2`).
- `characters[]`: nhân vật của tài khoản (`id, name, genderName, power, gold, gem, ruby,
  online`).
- `catalog`: `filters[]`, `moneyFields[]`, `vndMax: 2000000000`, `playerChildTables[]`.

`POST /api/panel/account?action=<tên>`:

| action | body | Việc thực hiện |
|--------|------|----------------|
| `account-create` | `{username, password, ipAddress}` | tạo tài khoản mới |
| `account-update` | `{id, username?, ipAddress?, accountAgeDays?, bdPlayer?, serverLogin?, mkc2?, email?, isFounder?, isQuanTriVien?, active?, isJail?, ban?, vip?, role?, vnd?, tongnap?, coin?, danap?, thoiVang?}` | sửa thông tin / quyền / ví, chỉ ghi ô được gửi lên |
| `password` | `{id, password}` | đặt lại mật khẩu (3–100 ký tự, chuỗi thô) |
| `money` | `{id, field, amount, add}` | cộng / trừ nhanh một cột tiền |
| `account-delete` | `{id}` | xoá tài khoản cùng toàn bộ dữ liệu con |
| `kick` | `{id}` | ngắt kết nối mọi nhân vật đang online của tài khoản |

Quy tắc:

- `username` 3–20 ký tự, chỉ `A-Za-z0-9_.`, không trùng; mật khẩu tối thiểu 3 ký tự.
- Tài khoản mới nhận `accountCreatedTimes = NOW()`, `accountAgeDays = 0`,
  `last_time_login = '2000-01-01 00:00:00'`, `last_time_logout = '2001-01-01 00:00:00'`,
  `mkc2 = '[0]'`, `role = -1`, `server_login = -1`.
- `money`: `field` chỉ nhận `vnd, tongnap, coin, danap, thoi_vang`; `amount` bị kẹp trong
  `0…2000000000` (riêng `tongnap` không kẹp trần); `add = true` là cộng, ngược lại là trừ.
  Nếu nhân vật đang online, panel **kick trước** rồi mới ghi để server không ghi đè lại.
- `account-delete` từ chối khi tài khoản còn ai đang online. Khi xoá, panel chạy trong
  **một transaction**: xoá các bảng con giữ `player_id` (`shop_ky_gui`,
  `history_receive_goldbar`, `giftcode_save`, `cvh_history_giftcode`) → `player` →
  `cvh_recharge` (theo `account_id`) → `account`.
- Modal có 4 tab: **Tài Khoản** (thông tin), **Quyền & Ví** (quyền + tiền + kick),
  **Nhân Vật** (danh sách nhân vật, bấm Sửa để mở luôn trang nhân vật),
  **Mật Khẩu** (đổi mật khẩu, hiện mật khẩu hiện tại để đối chiếu).

### Trang "Cài Đặt Website" và "Cấu Hình Nạp Tiền"

Hai trang này **sửa cấu hình website trong bảng `web_config` của MySQL** — cùng CSDL
với server game (mặc định `nro`), không sửa file PHP bằng tay nữa. Panel tự tạo bảng khi
chưa có:

```sql
CREATE TABLE web_config (
  name       VARCHAR(64) NOT NULL,   -- tên biến, ví dụ sv_code
  value      TEXT        NOT NULL,   -- giá trị
  updated_at DATETIME    NOT NULL,
  PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

Phía website, `hidden/server_config.php` chỉ còn là **loader**: đọc bảng rồi ghi ra
`hidden/web_config.cache.php` để `include`. Nhờ vậy mọi file PHP khác (`head.php`,
`index.php`, `qr-code.php`, `ajax/card.php`, `hidden/callback.php`…) **không phải sửa một
chữ** — chúng vẫn dùng `$sv_code`, `$partnerKey`, `$bank_type`… như trước.

- Panel ghi xong thì **xoá file cache**, nên lần truy cập kế tiếp của web thấy giá trị mới ngay.
- Panel chạy ở máy khác (không xoá được cache) thì loader tự hết hạn cache sau **60 giây**.
- Bảng lỗi hoặc bị thiếu thì loader dùng cache cũ; chưa có cache thì dùng giá trị tối thiểu
  để web không trắng trang. Lưu ý website vẫn cần MySQL để chạy (`hidden/config.php` kết nối
  trước loader), nên cache chỉ để nhanh và chịu lỗi riêng của bảng.
- Bảng còn trống mà website vẫn có file cấu hình kiểu cũ thì panel **nhập một lần** giá trị
  từ file đó vào bảng (chỉ đọc, không sửa file).

Thư mục web (dùng để định vị file cache và hiện trên thanh tiêu đề) tìm theo thứ tự:

1. `docRoot` trong `web/data/website.json` — để chỉ định tay;
2. thư mục quen thuộc của XAMPP/LAMPP **nếu** trong đó có `hidden/server_config.php`
   (`C:/xampp/htdocs`, `D:/xampp/htdocs`, `E:/xampp/htdocs`, `/opt/lampp/htdocs`, `/var/www/html`);
3. `htdocs` nằm cạnh panel trong project.

Nơi lưu thật đang dùng (`nro.web_config · 29 biến`) hiện ở góc phải tiêu đề mỗi trang.

`GET /api/panel/website` → `{ config, fields, groups, storage, backups }`.

- `config`: `{ tên_biến: giá_trị }` của mọi biến panel quản lý.
- `fields[]`: mô tả từng ô — `name, label, group, kind` (`string`/`int`), `secret`, `hint`.
- `groups[]`: `site` / `download` / `card` / `payos` / `bank` / `other`.
- Trang "Cài Đặt Website" sửa nhóm `site` + `download`; trang "Cấu Hình Nạp Tiền" sửa các nhóm còn lại.
- `storage`: `driver` (`mysql`), `database`, `table`, `rows`, `updated`, và
  `cache` (`path`, `exists`, `writable`, `mtime`).
- `backups[]`: các bản sao lưu, mới nhất trước.

Các biến được chia nhóm theo đúng nơi website dùng chúng:

| Nhóm | Biến | Website dùng ở đâu |
|------|------|--------------------|
| `site` | `sv_code`, `sv_name`, `title`, `description`, `keywords`, `author`, `DOMAIN`, `key_reCapcha`, `secret_reCapcha` | `head.php`, `index.php` — thẻ meta, tên server, tên miền |
| `download` | `linkAndroid`, `linkAndroid2`, `linkPC`, `linkPC2`, `linkIOS`, `linkIOS2`, `linkJar` | `head.php` — 4 nút tải game ở header |
| `card` | `partnerKey`, `partnerId` | `ajax/card.php` — nạp thẻ qua thesieure.com |
| `payos` | `client_Id`, `api_Key`, `checksum_Key` | `ajax/bank.php` — tạo link thanh toán PayOS |
| `bank` | `bank_type`, `bank_account`, `bank_owner`, `bank_password`, `bank_token`, `post_secret`, `bank_signature` | `hidden/bank-hook.php` (webhook), `ajax/mbbank.php` |
| `other` | `_vndTicket` | số VND cho 1 lượt quay |

`POST /api/panel/website?action=save` body `{ values: { tên_biến: giá_trị } }`.

Quy tắc ghi:

- Chỉ nhận những biến có trong danh sách trên; gửi tên khác sẽ báo lỗi.
- Chỉ những ô **thật sự đổi** mới được ghi, và tên các ô đó nằm trong `message` trả về
  (ví dụ `Đã lưu 1 ô vào bảng web_config (Tiêu đề SEO (title)) — đã xoá cache`).
- `_vndTicket` phải là số; các biến khác là chuỗi, tối đa 4000 ký tự.
- Trước mỗi lần ghi luôn sao lưu **toàn bộ** cấu hình vào
  `web/data/backups/web-config-<thời gian>.json`, giữ **20 bản** mới nhất.

`POST /api/panel/website?action=restore` body `{ backup? }` — không truyền tên thì lấy bản
mới nhất. Phục hồi ghi lại đúng các giá trị trong bản JSON đó vào bảng (biến nào không có
thì giữ nguyên), và vẫn sao lưu bản hiện tại trước khi ghi. Tên bản sao lưu phải là tên file
đơn giản, không cho đi ra ngoài thư mục backup.

`web/data/backups/` có thể còn file `server_config-*.php` từ phiên bản cũ (hồi panel
sửa trực tiếp file) — bản mới không đọc chúng nữa, xoá tay cũng được.

Trang "Cài Đặt Website" còn giữ form `settings.json` cũ (logo, thương hiệu, gmail CSKH,
zalo) — phần đó **chỉ là bản xem trước trong panel**, không ghi vào website thật.
Card **"Thông tin website đang chạy"** mới là phần ghi vào website thật.

Link tải game **không** nằm trong `settings.json`: 7 biến `linkAndroid`…`linkJar` là biến thật
của `server_config.php`, và `head.php` đọc chúng cho 4 nút tải ở header:

```php
$dl_android = $linkAndroid ?: ($linkAndroid2 ?: 'Downloads/Nrorun.apk');
```

Tức là: link chính trống thì lấy link dự phòng 2, trống cả hai thì quay về link mặc định cũ.
Sửa xong bấm **Lưu Vào Website** là nút tải trên web đổi ngay, không cần sửa PHP nữa.

### Trang "Quản Lý DNS Cloudflare"

Panel không lưu mật khẩu Cloudflare, chỉ lưu **API Token** và zone đã chọn vào
`web/data/cloudflare.json`. Token nên là loại quyền tối thiểu:

- **Zone → DNS → Edit**
- **Zone → Zone → Read** (để panel liệt kê được zone)

`GET /api/panel/cloudflare` → `{ configured, tokenSet, zoneId, zoneName, records[], zones[],
types[], ttlOptions[], proxyable[], storage, message?, error? }`.

Mỗi bản ghi: `id, type, name, content, ttl, proxied, priority, modified`.

`POST /api/panel/cloudflare?action=<tên>`:

| action | body | Việc thực hiện |
|--------|------|----------------|
| `token` | `{token}` | lưu token **sau khi** gọi `/user/tokens/verify` để kiểm tra |
| `zone` | `{zoneId, zoneName}` | chọn zone, có kiểm tra lại bằng `GET /zones/<id>` |
| `forget` | — | xoá `cloudflare.json` |
| `record-create` | `{type, name, content, ttl, proxied, priority}` | tạo bản ghi |
| `record-update` | `{id, ...}` | sửa bản ghi |
| `record-delete` | `{id}` | xoá bản ghi |
| `purge` | — | xoá toàn bộ cache của zone |

Quy tắc:

- Loại bản ghi cho chọn: `A, AAAA, CNAME, TXT, MX, NS, SRV, CAA, PTR`.
- Chỉ `A`, `AAAA`, `CNAME` mới bật được proxy cam; chọn loại khác thì ô proxy tự tắt.
- `ttl` chỉ nhận đúng các mốc Cloudflare hỗ trợ, ngoài danh sách thì về `1` (Tự động).
- `priority` chỉ gửi khi là `MX` hoặc `SRV`.
- Token sai hoặc thiếu quyền thì **không lưu** — panel trả về nguyên câu lỗi của Cloudflare.
- Máy chủ panel phải ra được internet (`https://api.cloudflare.com`), nếu không sẽ báo
  "Không kết nối được Cloudflare".

### Dữ liệu game

`/game-data/**` phục vụ trực tiếp thư mục `data/` của server, ví dụ
`/game-data/icon/x1/4028.png` → `data/icon/x1/4028.png` — đúng đường dẫn
`DataGame.sendIcon` đọc (`data/icon/x<zoom>/<iconID>.png`).
Icon của một item lấy theo `ItemTemplate.iconID`, **không phải** `Item.id`.

## Bảng giftcode

Cột `item` trong DB lưu `[{"id":457,"quantity":20,"option":[{"id":30,"param":0}]}]`
(`option` là tuỳ chọn, có thì mới ghi); cột `option` là option chung của cả code;
`listIdPlayers` là mảng id người đã nhận; `datecreate`/`expired` là text
`yyyy-MM-dd HH:mm:ss`.

Option server sẽ áp cho một item do `GiftCode.optionsFor(itemId)` quyết định:

1. option riêng của item trong cột `item`
2. riêng item `457` mặc định `ItemOption(30, 0)`
3. option chung của code (cột `option`)

`InventoryService` và panel dùng chung hàm này, nên nút **Option N** trên mỗi dòng
quà luôn hiện đúng số option server sẽ áp (kể cả option thừa hưởng), và khi mở ra
sẽ có dòng ghi rõ đang lấy từ đâu.

Bấm **Bấm để chọn vật phẩm / option** sẽ mở một modal riêng: tải cả danh sách
một lần rồi lọc, tick nhiều và **Thêm** một lượt. Danh sách vẽ dần
(`CHUNK = 200` trong `js/panel.js`), cuộn tới đáy tự nạp tiếp — nên mở ra là có
toàn bộ item/option chứ không bị giới hạn theo id.

Option của từng dòng quà nhập được cả hai cách: gõ thẳng `ID option` + `Param`,
hoặc bấm **Chọn từ bảng** để lấy từ danh sách option của server.

Cột `type` (Thường / VIP – MTV) được thêm tự động khi panel gọi API lần đầu:

```sql
ALTER TABLE `giftcode` ADD COLUMN `type` VARCHAR(32) NOT NULL DEFAULT 'Thường';
```

Mọi thao tác thêm/sửa/xoá đều gọi `GiftCodeManager.gI().reload()` để code mới có
hiệu lực ngay trong game.

## Thêm một trang mới

1. Thêm 1 dòng vào `NAV` trong `js/panel.js`.
2. Thêm `<section class="view" id="view-<key>">` vào `web/index.html`
   (chưa làm thì router tự hiện view "đang phát triển").
3. Thêm nhánh trong `WebPanelServer.handleApi` → lớp xử lý trong
   `src/nro/server/webpanel/`.

## Quy ước

- Field trong form có `id` trùng key trong `settings.json` → `readForm()`/`fillForm()`
  tự xử lý. Field nào là URL thì thêm vào `URL_FIELDS` để validate.
- Gọi server luôn qua `PanelAPI`, không gọi `fetch` trực tiếp trong view.
- Mỗi module API là 1 lớp trong `nro.server.webpanel`, nhận `HttpExchange` và
  trả JSON bằng `WebPanelServer.sendJson`.
