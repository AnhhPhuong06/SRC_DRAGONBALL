package server;

import Bot.BotManager;
import consts.ConstNpc;
import managers.GiftCodeManager;
import managers.boss.*;
import models.item.Item;
import models.player.Pet;
import models.player.Player;
import network.session.SessionManager;
import services.ItemService;
import services.PetService;
import services.Service;
import services.func.Input;
import services.map.ChangeMapService;
import services.map.NpcService;
import services.player.InventoryService;
import utils.SystemMetrics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import models.skill.Skill;
import services.SkillService;
import services.top.TopService;
import utils.ErrorRes;
import utils.Logger;
import utils.Util;
import database.daos.PlayerDAO;

public class Command {

    // ===== Mật khẩu lệnh admin - đọc từ data/config/web_admin.properties (khóa command_password) =====
    private static final String ADMIN_PASSWORD = loadAdminPassword();

    private static String loadAdminPassword() {
        try {
            java.util.Properties prop = new java.util.Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream("data/config/web_admin.properties")) {
                prop.load(fis);
            }
            String v = prop.getProperty("command_password", "").trim();
            if (!v.isEmpty()) {
                return v;
            }
        } catch (Exception ignored) {
        }
        return "1"; // fallback cũ nếu chưa cấu hình command_password
    }

    private static Command instance;

    private final Map<String, Consumer<Player>> adminCommands = new HashMap<>();
    private final Map<String, BiConsumer<Player, String>> parameterizedCommands = new HashMap<>();
    private final java.util.Set<Long> authedAdmins = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static Command gI() {
        if (instance == null) {
            instance = new Command();
        }
        return instance;
    }

    private Command() {
        initAdminCommands();
        initParameterizedCommands();
    }

    private void initAdminCommands() {
        adminCommands.put("s1c", player -> Service.gI().releaseCooldownSkill(player));
        adminCommands.put("code", player -> GiftCodeManager.gI().checkInfomationGiftCode(player));
        adminCommands.put("a", player -> BossManager.gI().showListBoss(player));
        adminCommands.put("b", player -> BrolyManager.gI().showListBoss(player));
        adminCommands.put("lbpbc", player -> OtherBossManager.gI().showListBoss(player));
        adminCommands.put("lbdtc", player -> RedRibbonHQManager.gI().showListBoss(player));
        adminCommands.put("lbbdkbc", player -> TreasureUnderSeaManager.gI().showListBoss(player));
        adminCommands.put("guruwar_open", player -> managers.tournament.GuruGuildWarManager.gI().adminForceOpenRegister(player));
        adminCommands.put("guruwar_match", player -> managers.tournament.GuruGuildWarManager.gI().adminForcePrepare(player));
        adminCommands.put("guruwar_start", player -> managers.tournament.GuruGuildWarManager.gI().adminForceStart(player));
        adminCommands.put("guruwar_end", player -> managers.tournament.GuruGuildWarManager.gI().adminForceEnd(player));
        adminCommands.put("bu2", player -> Input.gI().createFormSenditem1(player));
        adminCommands.put("lbcdrdc", player -> SnakeWayManager.gI().showListBoss(player));
        adminCommands.put("lbkghdc", player -> GasDestroyManager.gI().showListBoss(player));
        adminCommands.put("rongnhic", player -> Bktne.gI().showListBoss(player));
        adminCommands.put("boss12h", player -> Boss12hManager.gI().showListBoss12h(player));
        adminCommands.put("boss14h", player -> Boss14hManager.gI().showListBoss14h(player));
        adminCommands.put("s2c", player -> {
            SkillService.gI().learSkillSpecial(player, Skill.MA_PHONG_BA);
            SkillService.gI().learSkillSpecial(player, Skill.SUPER_KAME);
            SkillService.gI().learSkillSpecial(player, Skill.LIEN_HOAN_CHUONG);
        });
        adminCommands.put("toggleskill", player -> {
            Skill.ENABLE_SKILL_BIEN_HINH = !Skill.ENABLE_SKILL_BIEN_HINH;
            Service.gI().sendThongBao(player, "Đã " + (Skill.ENABLE_SKILL_BIEN_HINH ? "BẬT" : "TẮT")
                    + " hiển thị kỹ năng Biến hình Goku trên toàn server!");
        });
        adminCommands.put("skillnew", player -> {
            if (!Skill.ENABLE_SKILL_BIEN_HINH) {
                Service.gI().sendThongBao(player, "Kỹ năng này đang bị tắt trên toàn server! Dùng toggleskill để bật trước.");
                return;
            }
            SkillService.gI().learSkillSpecial(player, Skill.BIEN_HINH_GOKU);
        });
        adminCommands.put("r", player -> {
            Service.gI().releaseCooldownSkill(player);
        });
        adminCommands.put("lbtt", player -> TrungThuEventManager.gI().showListBoss(player));
        adminCommands.put("td", player -> showCoordinates(player, false));
        adminCommands.put("toado", player -> showCoordinates(player, false));
        adminCommands.put("pos", player -> showCoordinates(player, false));
        adminCommands.put("xy", player -> showCoordinates(player, false));
        adminCommands.put("loc", player -> showCoordinates(player, false));
        adminCommands.put("tdok", player -> showCoordinates(player, true));
        adminCommands.put("ad", player -> NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_ADMIN, -1,
                "|0|Time Start: " + ServerManager.timeStart + "\n|7|Hiện đang có: " + Client.gI().getPlayers().size()
                + " người chơi\n Sessions: " + SessionManager.gI().getNumSession() + "\n|0|Threads: " + Thread.activeCount()
                + " luồng" + "\n" + SystemMetrics.ToString(),
                "Ngọc rồng", "Đệ tử", "Bảo trì", "Tìm kiếm\nngười chơi", "Boss", "VND + Tổng nạp", "Tọa độ", "Đóng"));
        adminCommands.put("bot", player -> NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_BOT, -1,
                "Player online : " + Client.gI().getPlayers().size() + "\n"
                + "\b|1|Thread: " + Thread.activeCount() + "\n"
                + "Bot online : " + BotManager.gI().bot.size(),
                "Bot\nPem Quái", "Bot\nPem Nappa", "Bot\nPem Tương Lai", "Bot\nPem Cold", "Bot\nSăn Boss", "Đóng"));
        adminCommands.put("bu1", player -> Input.gI().createFormGiveItem(player));
        adminCommands.put("bu", player -> Input.gI().createFormGetItem(player));
        adminCommands.put("dc", player -> Service.gI().setPos(player, player.location.x, player.location.y + 0));
        adminCommands.put("reload", player -> {
            Manager.reloadShops();
            Service.gI().sendThongBao(player, "Đã reload " + Manager.SHOPS.size() + " shop từ DB. Mở lại shop để xem thay đổi.");
        });
        adminCommands.put("reload1", player -> {
            database.daos.GiftDataDAO.reload();
            services.func.GiftSystemService.gI().broadcastGiftUpdate();
            Service.gI().sendThongBao(player, "Đã reload + đẩy dữ liệu gift cho người online. Mở lại màn quà để xem.");
        });
        //    adminCommands.put("save", player -> ShopKyGuiManager.gI().save());
        adminCommands.put("testc", player -> {
            try {
            } catch (Exception e) {
                ErrorRes.howToFix(e.toString());
            }
        });
    }

    private void initParameterizedCommands() {
        parameterizedCommands.put("m ", (player, text) -> {
            int mapId = Integer.parseInt(text.replace("m ", ""));
            ChangeMapService.gI().changeMapInYard(player, mapId, -1, -1);
        });

        parameterizedCommands.put("i ", (player, text) -> {
            try {
                String[] txt = text.split(" ");
                int itemId = Integer.parseInt(txt[1]);
                int quan = 1;
                if (txt.length > 2) {
                    quan = Integer.parseInt(txt[2]);
                }
                Item item = ItemService.gI().createNewItem(((short) itemId), quan);
                List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) itemId);
                if (!ops.isEmpty()) {
                    item.itemOptions = ops;
                }
                InventoryService.gI().addItemBag(player, item);
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendThongBao(player, "Bú " + item.template.name + " [" + item.template.id + "] thành công !");
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Nhập sai cú pháp !");
            }
        });

        parameterizedCommands.put("diem ", (player, text) -> {
            try {
                String[] point = text.split(" ");
                int diem = Integer.parseInt(point[1]);
                if (point.length > 2) {
                    Player pl = Client.gI().getPlayer(point[2]);
                    if (pl != null) {
                        pl.inventory.coupon += diem;
                        Service.gI().sendThongBao(player, "Cộng thành công " + diem + " Sự kiện Quy lão cho " + pl.name);
                    } else {
                        Service.gI().sendThongBao(player, "Người chơi không tồn tại hoặc không trực tuyến !");
                    }
                } else {
                    player.inventory.coupon += diem;
                    Service.gI().sendThongBao(player, "Cộng thành công " + diem + " Sự kiện Quy lão !");
                }
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Nhập sai cú pháp !");
            }
        });

        parameterizedCommands.put("testeff ", (player, text) -> {
            try {
                int id = Integer.parseInt(text.replace("testeff ", ""));
                services.func.EffectMapService.gI().sendEffectMapToPlayer(player, id, 1, 1, player.location.x, player.location.y, 1);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Nhập sai cú pháp !");
            }
        });

        parameterizedCommands.put("testaura ", (player, text) -> {
            try {
                int id = Integer.parseInt(text.replace("testaura ", ""));
                player.idAuraTest = id;
                Service.gI().Send_Info_NV(player);
                services.map.ChangeMapService.gI().changeMapInYard(player, player.zone.map.mapId, player.zone.zoneId, player.location.x);
                Service.gI().sendThongBao(player, "Test aura: " + id);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Nhập sai cú pháp !");
            }
        });

        parameterizedCommands.put("eff ", (player, text) -> {
            try {
                int id = Integer.parseInt(text.replace("eff ", ""));
                services.func.EffectMapService.gI().sendEffectMapToAllInMap(player.zone, id, 1, 1, player.location.x, player.location.y, 1);
                Service.gI().sendThongBao(player, "Gửi effect " + id + " cho toàn map thành công !");
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Nhập sai cú pháp, vd: eff 1010");
            }
        });

        parameterizedCommands.put("tp ", (player, text) -> {
            try {
                String[] parts = text.trim().split("\\s+");
                if (parts.length == 3) {
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    Service.gI().setPos(player, x, y);
                    Service.gI().sendThongBao(player, "Đã tới tọa độ X=" + x + ", Y=" + y);
                } else if (parts.length >= 4) {
                    int mapId = Integer.parseInt(parts[1]);
                    int x = Integer.parseInt(parts[2]);
                    int y = Integer.parseInt(parts[3]);
                    ChangeMapService.gI().changeMapInYard(player, mapId, -1, x);
                    Service.gI().setPos(player, x, y);
                    Service.gI().sendThongBao(player, "Đã tới Map " + mapId + " (X=" + x + ", Y=" + y + ")");
                } else {
                    Service.gI().sendThongBao(player, "Cú pháp: tp <x> <y> hoặc tp <mapId> <x> <y>");
                }
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Tọa độ không hợp lệ! Cú pháp: tp <x> <y>");
            }
        });

        parameterizedCommands.put("buffvnd ", (player, text) -> handleBuffVndCommand(player, text));
        parameterizedCommands.put("vnd ", (player, text) -> handleBuffVndCommand(player, text));
        parameterizedCommands.put("sotien ", (player, text) -> handleBuffSoTienCommand(player, text));

    }

    private void handleBuffVndCommand(Player player, String text) {
        try {
            String[] parts = text.trim().split("\\s+");
            if (parts.length < 3) {
                Service.gI().sendThongBao(player, "Cú pháp: vnd <tên/tk> <vnđ> [tổng nạp] [số tiền]");
                return;
            }
            String targetInput = parts[1].trim();
            int vnd = Integer.parseInt(parts[2].trim());
            int totalVnd = (parts.length >= 4) ? Integer.parseInt(parts[3].trim()) : 0;
            int soTien = (parts.length >= 5) ? Integer.parseInt(parts[4].trim()) : 0;

            if (vnd <= 0 && totalVnd <= 0 && soTien <= 0) {
                Service.gI().sendThongBao(player, "Số tiền phải lớn hơn 0!");
                return;
            }

            PlayerDAO.AccountTarget target = PlayerDAO.resolveAccountTarget(targetInput);
            if (target.accountId == -1) {
                Service.gI().sendThongBao(player, "Không tìm thấy người chơi hoặc tài khoản: " + targetInput);
                return;
            }

            boolean ok = PlayerDAO.buffVnd(targetInput, vnd, totalVnd, soTien);
            if (ok) {
                String displayName = !target.playerName.isEmpty() ? target.playerName : target.username;
                StringBuilder sb = new StringBuilder();
                sb.append("Buff thành công cho ").append(displayName).append(" (TK: ").append(target.username).append("):\n");
                if (vnd > 0) sb.append("+ ").append(Util.format(vnd)).append(" VNĐ (Cash)\n");
                if (totalVnd > 0) sb.append("+ ").append(Util.format(totalVnd)).append(" Tổng Nạp\n");
                if (soTien > 0) sb.append("+ ").append(Util.format(soTien)).append(" Số Tiền\n");
                Service.gI().sendThongBao(player, sb.toString().trim());
            } else {
                Service.gI().sendThongBao(player, "Buff thất bại! Vui lòng thử lại.");
            }
        } catch (NumberFormatException e) {
            Service.gI().sendThongBao(player, "Số tiền không hợp lệ! Vui lòng chỉ nhập số.");
        } catch (Exception e) {
            Service.gI().sendThongBao(player, "Lỗi: " + e.getMessage());
        }
    }

    private void handleBuffSoTienCommand(Player player, String text) {
        try {
            String[] parts = text.trim().split("\\s+");
            if (parts.length < 3) {
                Service.gI().sendThongBao(player, "Cú pháp: sotien <tên/tk> <số tiền>");
                return;
            }
            String targetInput = parts[1].trim();
            int soTien = Integer.parseInt(parts[2].trim());

            if (soTien <= 0) {
                Service.gI().sendThongBao(player, "Số tiền phải lớn hơn 0!");
                return;
            }

            PlayerDAO.AccountTarget target = PlayerDAO.resolveAccountTarget(targetInput);
            if (target.accountId == -1) {
                Service.gI().sendThongBao(player, "Không tìm thấy người chơi hoặc tài khoản: " + targetInput);
                return;
            }

            boolean ok = PlayerDAO.buffVnd(targetInput, 0, 0, soTien);
            if (ok) {
                String displayName = !target.playerName.isEmpty() ? target.playerName : target.username;
                Service.gI().sendThongBao(player, "Buff thành công cho " + displayName + " (TK: " + target.username + "):\n+ "
                        + Util.format(soTien) + " Số Tiền (sotien)");
            } else {
                Service.gI().sendThongBao(player, "Buff thất bại! Vui lòng thử lại.");
            }
        } catch (NumberFormatException e) {
            Service.gI().sendThongBao(player, "Số tiền không hợp lệ! Vui lòng chỉ nhập số.");
        } catch (Exception e) {
            Service.gI().sendThongBao(player, "Lỗi: " + e.getMessage());
        }
    }

    public void showCoordinates(Player player, boolean showDialog) {
        if (player == null || player.location == null) {
            return;
        }
        int x = player.location.x;
        int y = player.location.y;
        int mapId = (player.zone != null && player.zone.map != null) ? player.zone.map.mapId : -1;
        String mapName = (player.zone != null && player.zone.map != null) ? player.zone.map.mapName : "Không xác định";
        int zoneId = (player.zone != null) ? player.zone.zoneId : -1;

        // 1. Thông báo nổi trên màn hình
        Service.gI().sendThongBao(player, "📍 [X: " + x + " | Y: " + y + "] - " + mapName + " (" + mapId + ") Khu: " + zoneId);

        // 2. Chat bong bóng trên đầu nhân vật
        Service.gI().chat(player, "📍 X: " + x + " | Y: " + y);

        // 3. Popup thông báo chi tiết
        if (showDialog) {
            String info = "|7|THÔNG TIN TỌA ĐỘ HIỆN TẠI\n"
                    + "|2|• Tọa độ X: " + x + "\n"
                    + "|2|• Tọa độ Y: " + y + "\n"
                    + "|1|• Bản đồ: " + mapName + " (ID: " + mapId + ")\n"
                    + "|1|• Khu vực: Khu " + zoneId + "\n"
                    + "|0|Cú pháp chat: tp " + x + " " + y + " để dịch chuyển.";
            Service.gI().sendThongBaoOK(player, info);
        }
    }

    public void chat(Player player, String text) {
        if (!check(player, text)) {
            Service.gI().chat(player, text);
        }
    }

    public boolean check(Player player, String text) {
        if (player.isAdmin()) {
            if (isAdminCommand(text)) {
                if (!isAuthed(player)) {
                    Input.gI().createFormAdminAuth(player);
                    return true;
                }
            }
            if (text.equals("lt")) {
                logAdminCommand(player, text);
                TopService.gI().loadListTop();
                Service.gI().sendThongBao(player, "Reload Top!");
                return true;
            }
            if (adminCommands.containsKey(text)) {
                logAdminCommand(player, text);
                adminCommands.get(text).accept(player);
                return true;
            }

            for (Map.Entry<String, BiConsumer<Player, String>> entry : parameterizedCommands.entrySet()) {
                if (text.startsWith(entry.getKey())) {
                    logAdminCommand(player, text);
                    entry.getValue().accept(player, text);
                    return true;
                }
            }
        }

        if (text.startsWith("ten con la ")) {
            PetService.gI().changeNamePet(player, text.replaceAll("ten con la ", ""));
        }

        if (player.pet != null) {
            switch (text) {
                case "di theo", "follow" ->
                    player.pet.changeStatus(Pet.FOLLOW);
                case "bao ve", "protect" ->
                    player.pet.changeStatus(Pet.PROTECT);
                case "tan cong", "attack" ->
                    player.pet.changeStatus(Pet.ATTACK);
                case "ve nha", "go home" ->
                    player.pet.changeStatus(Pet.GOHOME);
                case "bien hinh" ->
                    player.pet.transform();
            }
        }
        return false;
    }

    private boolean isAdminCommand(String text) {
        if (text == null || text.isEmpty()) return false;
        if (text.equals("lt")) return true;
        if (adminCommands.containsKey(text)) return true;
        for (String prefix : parameterizedCommands.keySet()) {
            if (text.startsWith(prefix)) return true;
        }
        return false;
    }

    private void logAdminCommand(Player player, String text) {
        try {
            String name = (player.name != null) ? player.name : "?";
            Logger.fileLog("command_admin", "[CMD] " + name + " (id=" + player.id + ") » " + text);
        } catch (Exception e) {
            // bỏ qua: ghi log không được ảnh hưởng luồng chạy lệnh
        }
    }

    public boolean isAuthed(Player player) {
        return authedAdmins.contains(player.id);
    }

    public void verifyAdminPassword(Player player, String input) {
        if (!player.isAdmin()) {
            return;
        }
        String pwd = input == null ? "" : input.trim();
        if (ADMIN_PASSWORD.equals(pwd)) {
            authedAdmins.add(player.id);
            logAdminCommand(player, "XÁC THỰC ADMIN THÀNH CÔNG");
            Service.gI().sendThongBao(player, "Xác thực admin thành công.\nGõ lại lệnh để thực thi. Sẽ phải nhập lại nếu thoát game.");
        } else {
            logAdminCommand(player, "XÁC THỰC ADMIN THẤT BẠI (sai mật khẩu)");
            Service.gI().sendThongBao(player, "Sai mật khẩu admin!");
        }
    }

    public void clearAuth(Player player) {
        if (player != null) {
            authedAdmins.remove(player.id);
        }
    }
}
