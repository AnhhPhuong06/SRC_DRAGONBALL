package models.npc.npc_list;

import consts.ConstNpc;
import consts.ConstTask;
import models.npc.Npc;
import models.npc.TaiXiu;
import models.npc.TaiXiuXocDiaProLuxuryVip999;
import models.player.Player;
import services.Service;
import services.TaskService;
import services.func.Input;
import services.func.minigame.CSMM;
import utils.TimeUtil;
import utils.Util;

public class LyTieuNuong extends Npc {

    public LyTieuNuong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                if (!player.isAdmin() && (player.getSession() == null || !player.getSession().actived)) {
                    createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "|7|Bạn chưa mở thành viên!\nVui lòng mở thành viên để sử dụng chức năng này.",
                            "Đóng");
                    return;
                }

                createOtherMenu(player, ConstNpc.BASE_MENU, "|5|Xin chào , Ta là Lý Tiểu Nương\n Ngươi bấm vào ta thì có làm tí Xóc đĩa không nhỉ ???\n Chơi đi ngại cái bầu đuồi gì\n|7|CỜ BẠC NGƯỜI KHÔNG CHƠI KHÔNG BAO GIỜ THẮNG.",
                        "Thể lệ", "Tài xỉu\nMĐ5", "Xóc đĩa\nMĐ5", "Con số\nMay mắn", "Đóng");

            }
        }
    }

    @Override
    public void confirmMenu(Player pl, int select) {
        if (canOpenNpc(pl)) {
            if (pl.idMark.getIndexMenu() == ConstNpc.IGNORE_MENU) {
                return;
            }
            if (!pl.isAdmin() && (pl.getSession() == null || !pl.getSession().actived)) {
                Service.gI().sendThongBao(pl, "Bạn chưa mở thành viên!");
                return;
            }
            switch (pl.idMark.getIndexMenu()) {
                case ConstNpc.BASE_MENU -> {
                    switch (select) {
                        case 0 ->
                            createOtherMenu(pl, ConstNpc.IGNORE_MENU, ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_HD),
                                    "Ok");
                        case 1 -> {
                            if (TaiXiu.baotri) {
                                createOtherMenu(pl, ConstNpc.MINIGAME_BAOTRI, ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU),
                                        "Cập nhập", "Đóng");
                                return;
                            }
                            if (pl.goldTai == 0 && pl.goldXiu == 0) {
                                createOtherMenu(pl, ConstNpc.MINIGAME_TAIXIU_TUYCHON,
                                        ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_EMPTY),
                                        "Cập nhập", "Đặt Tài", "Đặt Xỉu", "Đóng");
                            } else if (pl.goldTai > 0) {
                                if (pl.isAdmin()) {
                                    createOtherMenu(pl, ConstNpc.MINIGAME_TAIXIU_TAI,
                                            ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_TAI),
                                            "Auto Tài", "Auto Xỉu", "Tam Hoa", "Cập nhập", "Thêm Tài",
                                            "Đóng");
                                } else {
                                    createOtherMenu(pl, ConstNpc.MINIGAME_TAIXIU_TAI,
                                            ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_TAI),
                                            "Cập nhập", "Thêm Tài", "Đóng");
                                }
                            } else if (pl.goldXiu > 0) {
                                if (pl.isAdmin()) {
                                    createOtherMenu(pl, ConstNpc.MINIGAME_TAIXIU_XIU,
                                            ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_XIU),
                                            "Auto Tài", "Auto Xỉu", "Tam Hoa", "Cập nhập", "Thêm Xỉu", "Đóng");
                                } else {
                                    createOtherMenu(pl, ConstNpc.MINIGAME_TAIXIU_XIU,
                                            ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_XIU),
                                            "Cập nhập", "Thêm Xỉu", "Đóng");
                                }
                            }
                        }
                        case 2 -> {
                            createOtherMenu(pl, ConstNpc.XOC_DIA_VAI_LON_GAT, "\b|2|MINIGAME XÓC ĐĨA\n\n"
                                    + "|7|Chẵn [ x1.8 ]\n"
                                    + "Chẵn - Tứ tử đỏ [ x10.0 ]\n"
                                    + "Chẵn - Tứ tử trắng [ x10.0 ]\n"
                                    + "|5|Lẻ [ x1.8 ]\n"
                                    + "Lẻ - 3 đỏ 1 trắng [ x3.0 ]\n"
                                    + "Lẻ - 3 trắng 1 đỏ [ x3.0 ]\n"
                                    + "|2|Đặt tối thiểu: 1.000 HN, Tối đa: 1.000.000.000 HN"
                                    + "\n\n|4|Lưu ý: Thoát game khi chốt Kết quả sẽ Mất Tiền Cược và Tiền Thưởng", "Hướng Dẫn Chơi", "Tham gia");
                        }
                        case 3 -> {
                            this.createOtherMenu(pl, 2,
                                    "Chúc mừng các may mắn chọn trúng lần trước"
                                    + "\nTrò chơi sẽ bắt đầu sau : "
                                    + TimeUtil.getTimeLeft(CSMM.gI().lastTimeRollCSMM, CSMM.gI().SecondsTarget)
                                    + " nữa"
                                    + (CSMM.gI().NumRANDOM != -1
                                    ? ("\n Kết quả giải trước là : " + CSMM.gI().NumRANDOM + "")
                                    : "")
                                    + (CSMM.gI().listRegNumber.containsKey(pl)
                                    ? ("\n" + CSMM.gI().listRegNumber.get(pl))
                                    : ""),
                                    "Dự đoán", "Cập nhật", "Đóng");
                            break;
                        }
                    }
                }

                case 2 -> {
                    switch (select) {
                        case 0 ->
                            Input.gI().createFormCSMM(pl);
                        case 1 ->
                            this.createOtherMenu(pl, 2,
                                    "Chúc mừng các may mắn chọn trúng lần trước"
                                    + "\nTrò chơi sẽ bắt đầu sau : "
                                    + TimeUtil.getTimeLeft(CSMM.gI().lastTimeRollCSMM, CSMM.gI().SecondsTarget)
                                    + " nữa"
                                    + (CSMM.gI().NumRANDOM != -1
                                    ? ("\n Kết quả giải trước là : " + CSMM.gI().NumRANDOM + "")
                                    : "")
                                    + (CSMM.gI().listRegNumber.containsKey(pl)
                                    ? ("\n" + CSMM.gI().listRegNumber.get(pl))
                                    : ""),
                                    "Dự đoán", "Cập nhật", "Đóng");
                    }
                }

                case ConstNpc.MINIGAME_TAIXIU_TUYCHON -> {
                    if (pl.goldTai == 0 && pl.goldXiu == 0 && !TaiXiu.baotri) {
                        switch (select) {
                            case 0 ->
                                createOtherMenu(pl, ConstNpc.MINIGAME_TAIXIU_TUYCHON,
                                        ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_EMPTY),
                                        "Cập nhập", "Đặt Tài", "Đặt Xỉu", "Đóng");
                            case 1 -> {
                                if (pl.isAdmin()) {
                                    createOtherMenu(pl, ConstNpc.MINIGAME_TAIXIU_TAI,
                                            ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_TAI),
                                            "Auto Tài", "Auto Xỉu", "Tam Hoa", "Cập nhập", "Thêm Tài",
                                            "Đóng");
                                } else {
                                    if (TaskService.gI().getIdTask(pl) < ConstTask.TASK_20_0) {
                                        Service.gI().sendThongBao(pl,
                                                "Bạn phải làm tới nhiệm vụ TĐST mới có thể tham gia");
                                        return;
                                    }
                                    Input.gI().taixiu_Tai(pl);
                                }
                            }
                            case 2 -> {
                                if (pl.isAdmin()) {
                                    createOtherMenu(pl, ConstNpc.MINIGAME_TAIXIU_XIU,
                                            ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_XIU),
                                            "Auto Tài", "Auto Xỉu", "Tam Hoa", "Cập nhập", "Thêm Xỉu", "Đóng");
                                } else {
                                    if (TaskService.gI().getIdTask(pl) < ConstTask.TASK_20_0) {
                                        Service.gI().sendThongBao(pl,
                                                "Bạn phải làm tới nhiệm vụ TĐST mới có thể tham gia");
                                        return;
                                    }
                                    Input.gI().taixiu_Xiu(pl);
                                }
                            }
                        }
                    }
                }

                case ConstNpc.MINIGAME_TAIXIU_TAI -> {
                    if (pl.goldTai >= 0 && !TaiXiu.baotri) {
                        if (pl.isAdmin()) {
                            switch (select) {
                                case 0, 1, 2 -> {
                                    TaiXiu.gI().chinhCau((byte) select);
                                    Service.gI().sendThongBao(pl, "|7|Bạn đã can thiệp kết quả 100% ra: "
                                            + (select == 0 ? "TÀI" : select == 1 ? "XỈU" : "TAM HOA"));
                                    createOtherMenu(pl, ConstNpc.MINIGAME_TAIXIU_TAI,
                                            ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_TAI),
                                            "Auto Tài", "Auto Xỉu", "Tam Hoa", "Cập nhập", "Thêm Tài",
                                            "Đóng");
                                }
                                case 3 ->
                                    createOtherMenu(pl, ConstNpc.MINIGAME_TAIXIU_TAI,
                                            ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_TAI),
                                            "Auto Tài", "Auto Xỉu", "Tam Hoa", "Cập nhập", "Thêm Tài", "Đóng");
                                case 4 -> {
                                    if (TaskService.gI().getIdTask(pl) < ConstTask.TASK_20_0) {
                                        Service.gI().sendThongBao(pl,
                                                "Bạn phải làm tới nhiệm vụ TĐST mới có thể tham gia");
                                        return;
                                    }
                                    Input.gI().taixiu_Tai(pl);
                                }
                            }
                        } else {
                            switch (select) {
                                case 0 ->
                                    createOtherMenu(pl, ConstNpc.MINIGAME_TAIXIU_TAI,
                                            ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_TAI),
                                            "Cập nhập", "Thêm Tài", "Đóng");
                                case 1 -> {
                                    if (TaskService.gI().getIdTask(pl) < ConstTask.TASK_20_0) {
                                        Service.gI().sendThongBao(pl,
                                                "Bạn phải làm tới nhiệm vụ TĐST mới có thể tham gia");
                                        return;
                                    }
                                    Input.gI().taixiu_Tai(pl);
                                }
                            }
                        }
                    }
                }

                case ConstNpc.MINIGAME_TAIXIU_XIU -> {
                    if (pl.goldXiu >= 0 && !TaiXiu.baotri) {
                        if (pl.isAdmin()) {
                            switch (select) {
                                case 0, 1, 2 -> {
                                    TaiXiu.gI().chinhCau((byte) select);
                                    Service.gI().sendThongBao(pl, "|7|Bạn đã can thiệp kết quả 100% ra: "
                                            + (select == 0 ? "TÀI" : select == 1 ? "XỈU" : "TAM HOA"));
                                    createOtherMenu(pl, ConstNpc.MINIGAME_TAIXIU_XIU,
                                            ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_XIU),
                                            "Auto Tài", "Auto Xỉu", "Tam Hoa", "Cập nhập", "Thêm Xỉu", "Đóng");
                                }
                                case 3 ->
                                    createOtherMenu(pl, ConstNpc.MINIGAME_TAIXIU_XIU,
                                            ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_XIU),
                                            "Auto Tài", "Auto Xỉu", "Tam Hoa", "Cập nhập", "Thêm Xỉu", "Đóng");
                                case 4 -> {
                                    if (TaskService.gI().getIdTask(pl) < ConstTask.TASK_20_0) {
                                        Service.gI().sendThongBao(pl,
                                                "Bạn phải làm tới nhiệm vụ TĐST mới có thể tham gia");
                                        return;
                                    }
                                    Input.gI().taixiu_Xiu(pl);
                                }
                            }
                        } else {
                            switch (select) {
                                case 0 ->
                                    createOtherMenu(pl, ConstNpc.MINIGAME_TAIXIU_XIU,
                                            ConstNpc.TextNpc(pl, ConstNpc.TAI_XIU_XIU),
                                            "Cập nhập", "Thêm Xỉu", "Đóng");
                                case 1 -> {
                                    if (TaskService.gI().getIdTask(pl) < ConstTask.TASK_20_0) {
                                        Service.gI().sendThongBao(pl,
                                                "Bạn phải làm tới nhiệm vụ TĐST mới có thể tham gia");
                                        return;
                                    }
                                    Input.gI().taixiu_Xiu(pl);
                                }
                            }
                        }
                    }
                }
                case ConstNpc.XOC_DIA_VAI_LON_GAT -> {
                    switch (select) {
                        case 0 ->
                            createOtherMenu(pl, ConstNpc.IGNORE_MENU, "|7|---XÓC ĐĨA NROKami---\n\n|5|Có 6 cửa cược cho trò chơi Xóc Đĩa. Bạn có thể chọn cược vào các cửa:\n\n"
                                    + "|7|Chẵn [ x1.8 ]\n"
                                    + "Chẵn - Tứ tử đỏ [ x10.0 ]\n"
                                    + "Chẵn - Tứ tử trắng [ x10.0 ]\n"
                                    + "|5|Lẻ [ x1.8 ]\n"
                                    + "Lẻ - 3 đỏ 1 trắng [ x3.0 ]\n"
                                    + "Lẻ - 3 trắng 1 đỏ [ x3.0 ]\n"
                                    + "|2|Đặt tối thiểu: 1.000 HN, Tối đa: 1.000.000.000 HN"
                                    + "\n\n|4|Lưu ý: Trong quá trình diễn ra khi đặt cược nếu thoát game trong lúc phát thưởng phần quà sẽ bị hủy", "Ok");
                        case 1 -> {
                            if (!TaiXiuXocDiaProLuxuryVip999.baoTri) {
                                if (pl.isAdmin()) {
                                    createOtherMenu(pl, ConstNpc.XOC_DIA_VAI_LON_GAT_ADMIN_MODE, "|7|---XÓC ĐĨA NROKami---" + (TaiXiuXocDiaProLuxuryVip999.baoTri ? "\n[ĐANG BẢO TRÌ]" : "") + (!TaiXiuXocDiaProLuxuryVip999.gI().listKetQua.isEmpty() ? "\n\n|2|Kết quả kì trước:" + TaiXiuXocDiaProLuxuryVip999.gI().getKetQua() : "")
                                            + "\n\n|2|Hiện tại có:"
                                            + "\n|5|Cửa Chẵn (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersChan.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalChan) + " Hồng ngọc)"
                                            + "\nCửa Lẻ (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersLe.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalLe) + " Hồng ngọc)"
                                            + "\nCửa 3 Đỏ 1 Trắng (" + TaiXiuXocDiaProLuxuryVip999.gI().Players3Do1Trang.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().total3Do1Trang) + " Hồng ngọc)"
                                            + "\nCửa 3 Trắng 1 Đỏ (" + TaiXiuXocDiaProLuxuryVip999.gI().Players3Trang1Do.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().total3Trang1Do) + " Hồng ngọc)"
                                            + "\nCửa Tứ Tử Đỏ (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersTuTuDo.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalTuTuDo) + " Hồng ngọc)"
                                            + "\nCửa Tứ Tử Trắng (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersTuTuTrang.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalTuTuTrang) + " Hồng ngọc)"
                                            + "\n\n|7|Thời gian còn lại: " + Util.msToTimeNdung(TaiXiuXocDiaProLuxuryVip999.gI().lastTimeEnd + TaiXiuXocDiaProLuxuryVip999.TIME_END)
                                            + "\n|3|[ - ADMINISTRATOR BET RESULT MANAGER FORM - ]" + TaiXiuXocDiaProLuxuryVip999.gI().getKetQuaAdminChange(), "Cập nhật", "Chỉnh Cầu\nChẵn", "Chỉnh Cầu\nLẻ", "Chỉnh Cầu\n3 Đỏ\n1 Trắng", "Chỉnh Cầu\n3 Trắng\n1 Đỏ", "Chỉnh Cầu\nTứ Tử\nĐỏ", "Chỉnh Cầu\nTứ Tử\nTrắng", "Bảo trì\nAnd\nRefund");
                                } else {
                                    createOtherMenu(pl, ConstNpc.XOC_DIA_VAI_LON_GAT_DAT_CUOC, "|7|---XÓC ĐĨA NROKami---" + (!TaiXiuXocDiaProLuxuryVip999.gI().listKetQua.isEmpty() ? "\n\n|2|Kết quả kì trước:" + TaiXiuXocDiaProLuxuryVip999.gI().getKetQua() : "")
                                            + "\n\n|2|Hiện tại có:"
                                            + "\n|5|Cửa Chẵn (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersChan.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalChan) + " Hồng ngọc)"
                                            + "\nCửa Lẻ (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersLe.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalLe) + " Hồng ngọc)"
                                            + "\nCửa 3 Đỏ 1 Trắng (" + TaiXiuXocDiaProLuxuryVip999.gI().Players3Do1Trang.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().total3Do1Trang) + " Hồng ngọc)"
                                            + "\nCửa 3 Trắng 1 Đỏ (" + TaiXiuXocDiaProLuxuryVip999.gI().Players3Trang1Do.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().total3Trang1Do) + " Hồng ngọc)"
                                            + "\nCửa Tứ Tử Đỏ (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersTuTuDo.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalTuTuDo) + " Hồng ngọc)"
                                            + "\nCửa Tứ Tử Trắng (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersTuTuTrang.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalTuTuTrang) + " Hồng ngọc)"
                                            + "\n\n|7|Thời gian còn lại: " + Util.msToTimeNdung(TaiXiuXocDiaProLuxuryVip999.gI().lastTimeEnd + TaiXiuXocDiaProLuxuryVip999.TIME_END), "Cập nhật", "Đặt\nChẵn", "Đặt\nLẻ", "Đặt\n3 Đỏ\n1 Trắng", "Đặt\n3 Trắng\n1 Đỏ", "Đặt\nTứ Tử\nĐỏ", "Đặt\nTứ Tử\nTrắng", "Đóng");
                                }
                            } else {
                                createOtherMenu(pl, 1, "\n|7|---XÓC ĐĨA NROKami---\n\n|7|Hệ thống sắp bảo trì", "Đóng");
                            }
                        }
                    }
                }

                case ConstNpc.XOC_DIA_VAI_LON_GAT_DAT_CUOC -> {
                    if (TaiXiuXocDiaProLuxuryVip999.baoTri) {
                        return;
                    }
                    switch (select) {
                        case 0 ->
                            createOtherMenu(pl, ConstNpc.XOC_DIA_VAI_LON_GAT_DAT_CUOC, "|7|---XÓC ĐĨA NROKami---" + (!TaiXiuXocDiaProLuxuryVip999.gI().listKetQua.isEmpty() ? "\n\n|2|Kết quả kì trước:" + TaiXiuXocDiaProLuxuryVip999.gI().getKetQua() : "")
                                    + "\n\n|2|Hiện tại có:"
                                    + "\n|5|Cửa Chẵn (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersChan.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalChan) + " Hồng ngọc)"
                                    + "\nCửa Lẻ (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersLe.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalLe) + " Hồng ngọc)"
                                    + "\nCửa 3 Đỏ 1 Trắng (" + TaiXiuXocDiaProLuxuryVip999.gI().Players3Do1Trang.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().total3Do1Trang) + " Hồng ngọc)"
                                    + "\nCửa 3 Trắng 1 Đỏ (" + TaiXiuXocDiaProLuxuryVip999.gI().Players3Trang1Do.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().total3Trang1Do) + " Hồng ngọc)"
                                    + "\nCửa Tứ Tử Đỏ (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersTuTuDo.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalTuTuDo) + " Hồng ngọc)"
                                    + "\nCửa Tứ Tử Trắng (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersTuTuTrang.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalTuTuTrang) + " Hồng ngọc)"
                                    + "\n\n|7|Thời gian còn lại: " + Util.msToTimeNdung(TaiXiuXocDiaProLuxuryVip999.gI().lastTimeEnd + TaiXiuXocDiaProLuxuryVip999.TIME_END), "Cập nhật", "Đặt\nChẵn", "Đặt\nLẻ", "Đặt\n3 Đỏ\n1 Trắng", "Đặt\n3 Trắng\n1 Đỏ", "Đặt\nTứ Tử\nĐỏ", "Đặt\nTứ Tử\nTrắng", "Đóng");
                        case 1 ->
                            Input.gI().BETCHAN_xocdia(pl);
                        case 2 ->
                            Input.gI().BETLE_xocdia(pl);
                        case 3 ->
                            Input.gI().BET3D1T_xocdia(pl);
                        case 4 ->
                            Input.gI().BET3T1D_xocdia(pl);
                        case 5 ->
                            Input.gI().BET4D_xocdia(pl);
                        case 6 ->
                            Input.gI().BET4T_xocdia(pl);
                    }
                }
                case ConstNpc.XOC_DIA_VAI_LON_GAT_ADMIN_MODE -> {
                    switch (select) {
                        case 1 ->
                            TaiXiuXocDiaProLuxuryVip999.gI().changeKetQua(TaiXiuXocDiaProLuxuryVip999.CHAN);
                        case 2 ->
                            TaiXiuXocDiaProLuxuryVip999.gI().changeKetQua(TaiXiuXocDiaProLuxuryVip999.LE);
                        case 3 ->
                            TaiXiuXocDiaProLuxuryVip999.gI().changeKetQua(TaiXiuXocDiaProLuxuryVip999.BA_DO_MOT_TRANG);
                        case 4 ->
                            TaiXiuXocDiaProLuxuryVip999.gI().changeKetQua(TaiXiuXocDiaProLuxuryVip999.BA_TRANG_MOT_DO);
                        case 5 ->
                            TaiXiuXocDiaProLuxuryVip999.gI().changeKetQua(TaiXiuXocDiaProLuxuryVip999.TU_TU_DO);
                        case 6 ->
                            TaiXiuXocDiaProLuxuryVip999.gI().changeKetQua(TaiXiuXocDiaProLuxuryVip999.TU_TU_TRANG);
                        case 7 ->
                            TaiXiuXocDiaProLuxuryVip999.gI().baoTriAndRefund();
                    }
                    createOtherMenu(pl, ConstNpc.XOC_DIA_VAI_LON_GAT_ADMIN_MODE, "|7|---XÓC ĐĨA NROKami---" + (TaiXiuXocDiaProLuxuryVip999.baoTri ? "\n[ĐANG BẢO TRÌ]" : "") + (!TaiXiuXocDiaProLuxuryVip999.gI().listKetQua.isEmpty() ? "\n\n|2|Kết quả kì trước:" + TaiXiuXocDiaProLuxuryVip999.gI().getKetQua() : "")
                            + "\n\n|2|Hiện tại có:"
                            + "\n|5|Cửa Chẵn (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersChan.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalChan) + " Hồng ngọc)"
                            + "\nCửa Lẻ (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersLe.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalLe) + " Hồng ngọc)"
                            + "\nCửa 3 Đỏ 1 Trắng (" + TaiXiuXocDiaProLuxuryVip999.gI().Players3Do1Trang.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().total3Do1Trang) + " Hồng ngọc)"
                            + "\nCửa 3 Trắng 1 Đỏ (" + TaiXiuXocDiaProLuxuryVip999.gI().Players3Trang1Do.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().total3Trang1Do) + " Hồng ngọc)"
                            + "\nCửa Tứ Tử Đỏ (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersTuTuDo.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalTuTuDo) + " Hồng ngọc)"
                            + "\nCửa Tứ Tử Trắng (" + TaiXiuXocDiaProLuxuryVip999.gI().PlayersTuTuTrang.size() + " người - tổng " + Util.format(TaiXiuXocDiaProLuxuryVip999.gI().totalTuTuTrang) + " Hồng ngọc)"
                            + "\n\n|7|Thời gian còn lại: " + Util.msToTimeNdung(TaiXiuXocDiaProLuxuryVip999.gI().lastTimeEnd + TaiXiuXocDiaProLuxuryVip999.TIME_END)
                            + "\n|3|[ - ADMINISTRATOR BET RESULT MANAGER FORM - ]" + TaiXiuXocDiaProLuxuryVip999.gI().getKetQuaAdminChange(), "Cập nhật", "Chỉnh Cầu\nChẵn", "Chỉnh Cầu\nLẻ", "Chỉnh Cầu\n3 Đỏ\n1 Trắng", "Chỉnh Cầu\n3 Trắng\n1 Đỏ", "Chỉnh Cầu\nTứ Tử\nĐỏ", "Chỉnh Cầu\nTứ Tử\nTrắng", "Bảo trì\nAnd\nRefund");
                }
            }
        }
    }
}
