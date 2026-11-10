package services.dungeon;

 


import models.dungeon.TreasureUnderSea;
import models.item.Item;
import java.util.ArrayList;
import java.util.List;
import models.map.Zone;
import models.player.Player;
import services.player.InventoryService;
import services.Service;
import utils.Util;

public class TreasureUnderSeaService {

    private static TreasureUnderSeaService instance;

    public static TreasureUnderSeaService gI() {
        if (instance == null) {
            instance = new TreasureUnderSeaService();
        }
        return instance;
    }

    public List<TreasureUnderSea> banDoKhoBaus;

    private TreasureUnderSeaService() {
        this.banDoKhoBaus = new ArrayList<>();
        for (int i = 0; i < TreasureUnderSea.AVAILABLE; i++) {
            this.banDoKhoBaus.add(new TreasureUnderSea(i));
        }
    }

    public void addMapBanDoKhoBau(int id, Zone zone) {
        this.banDoKhoBaus.get(id).getZones().add(zone);
    }

    public void openBanDoKhoBau(Player player, byte level) {
        if (level >= 1 && level <= 110) {
            if (player.clan != null && player.clan.BanDoKhoBau == null) {
                Item item = InventoryService.gI().findItemBag(player, 611);
                if (item != null && item.quantity > 0) {
                    TreasureUnderSea banDoKhoBau = null;
                    for (TreasureUnderSea bdkb : this.banDoKhoBaus) {
                        if (!bdkb.isOpened) {
                            banDoKhoBau = bdkb;
                            break;
                        }
                    }
                    if (banDoKhoBau != null) {
                        // Kiểm tra và reset counter nếu qua ngày mới
                        if (Util.isAfterMidnight(player.lastTimeJoinBDKB)) {
                            player.timesPerDayBDKB = 0;
                        }
                        
                        // Kiểm tra giới hạn 3 lần/ngày
                        if (player.timesPerDayBDKB >= 3) {
                            Service.gI().sendThongBao(player, "Bạn đã vào hang kho báu 3 lần trong hôm nay, hẹn gặp lại ngày mai");
                            return;
                        }

                        InventoryService.gI().subQuantityItemsBag(player, item, 1);
                        InventoryService.gI().sendItemBags(player);
                        banDoKhoBau.openBanDoKhoBau(player, player.clan, level);
                        
                        // Tăng counter sau khi mở thành công
                        player.timesPerDayBDKB++;
                        player.lastTimeJoinBDKB = player.clan.lastTimeOpenBanDoKhoBau;
                    } else {
                        Service.gI().sendThongBao(player, "Hang kho báu đã đầy, hãy quay lại sau 30 phút");
                    }
                } else {
                    Service.gI().sendThongBao(player, "Không tìm thấy bản đồ kho báu");
                }
            } else {
                Service.gI().sendThongBao(player, "Không thể thực hiện");
            }
        }
    }
    public boolean finishNow(Player admin) {
        if (admin == null || admin.clan == null) {
            return false;
        }
        TreasureUnderSea bdkb = admin.clan.BanDoKhoBau;
        if (bdkb == null || !bdkb.isOpened()) {
            return false;
        }
        try {
            bdkb.finish();
            bdkb.dispose();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
