# Ảnh avatar cho Boss (và nhân vật)

Thư mục này là nơi thả **ảnh head** để panel hiện avatar cạnh tên boss.

## Vì sao phải thả ảnh vào đây?

Thư mục `data/` của server **không có ảnh head/body/leg**. Server chỉ có:

| Thư mục | Nội dung |
|---------|----------|
| `data/icon/x<zoom>/<iconID>.png` | icon vật phẩm (item template) |
| `data/mob/x<zoom>/<mobID>.png` | ảnh mob (chỉ tới id ~126) |
| `data/img_by_name/x<zoom>/<tên>.png` | ảnh gọi theo tên (Skills_*, aura_*, mount_*, hat_*) |
| `data/map/…`, `data/effect/…` | dữ liệu map và hiệu ứng |

Ảnh đầu/nhân vật (head) nằm trong **client game (file APK / res của client)**, server không đọc file này.
Vì vậy panel không thể tự sinh avatar — nhưng đã chuẩn bị sẵn chỗ để bạn thả ảnh vào.

## Cách dùng

1. Xuất bộ ảnh head từ client (thường là `data/head/x1/<headID>.png`).
2. Copy vào đúng thư mục này, **giữ nguyên tên theo headID**, ví dụ:

```
web/assets/boss/180.png   ← head của Tiểu đội trưởng
web/assets/boss/129.png
```

3. Tải lại trang panel: boss nào có file ảnh tương ứng sẽ hiện avatar ngay, boss chưa có ảnh vẫn hiện vòng tròn chữ cái đầu.

## headID lấy từ đâu?

API `GET /api/panel/server` trả về cho mỗi boss:

```json
{ "id": -100001, "name": "Tiểu đội trưởng", "head": 180, "body": 181, "leg": 182, "hasAvatar": false }
```

`head`/`body`/`leg` là `BossData.outfit` (đúng bộ đồ server gửi cho client), và `hasAvatar`
cho biết file `assets/boss/<head>.png` đã có hay chưa — nên panel không gọi ảnh lỗi (404) khi chưa thả ảnh.
