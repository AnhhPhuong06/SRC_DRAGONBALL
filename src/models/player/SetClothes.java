package models.player;

 
import models.item.Item;

public class SetClothes {

    private Player player;

    public SetClothes(Player player) {
        this.player = player;
    }

    public byte songoku;
    public byte thienXinHang;
    public byte kirin;
    public byte kaioken;

    public byte ocTieu;
    public byte pikkoroDaimao;
    public byte picolo;

    public byte kakarot;
    public byte cadic;
    public byte nappa;

    public byte thanVuTruKaio;
    public byte nail;
    public byte cadicM;

    public byte setTinhAn;
    public byte setNhatAn;
    public byte setNguyetAn;

    public byte worldcup;
    public byte setDHD;

    public boolean godClothes;
    public int ctHaiTac = -1;

    public void setup() {
        setDefault();
        setupSKT();
        setupSKHNew();
        this.godClothes = true;
        int bodySize = this.player.inventory.itemsBody != null ? this.player.inventory.itemsBody.size() : 0;
        for (int i = 0; i < 5; i++) {
            if (i < bodySize) {
                Item item = this.player.inventory.itemsBody.get(i);
                if (item != null && item.isNotNullItem()) {
                    if (item.template.id > 567 || item.template.id < 555) {
                        this.godClothes = false;
                        break;
                    }
                } else {
                    this.godClothes = false;
                    break;
                }
            } else {
                this.godClothes = false;
                break;
            }
        }
        if (bodySize > 5) {
            Item ct = this.player.inventory.itemsBody.get(5);
            if (ct != null && ct.isNotNullItem()) {
                switch (ct.template.id) {
                    case 618:
                    case 619:
                    case 620:
                    case 621:
                    case 622:
                    case 623:
                    case 624:
                    case 626:
                    case 627:
                        this.ctHaiTac = ct.template.id;
                        break;
                }
            }
        }
    }

    private void setupSKT() {
        if (this.player.inventory.itemsBody == null) {
            return;
        }
        int limit = Math.min(5, this.player.inventory.itemsBody.size());
        for (int i = 0; i < limit; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item != null && item.isNotNullItem() && item.itemOptions != null) {
                boolean isActSet = false;
                for (Item.ItemOption io : item.itemOptions) {
                    if (io == null || io.optionTemplate == null) {
                        continue;
                    }
                    switch (io.optionTemplate.id) {
                        case 129:
                        case 141:
                            if (!isActSet) {
                                isActSet = true;
                                songoku++;
                            }
                            break;
                        case 127:
                        case 139:
                            if (!isActSet) {
                                isActSet = true;
                                thienXinHang++;
                            }
                            break;
                        case 128:
                        case 140:
                            if (!isActSet) {
                                isActSet = true;
                                kirin++;
                            }
                            break;
                        case 131:
                        case 143:
                            if (!isActSet) {
                                isActSet = true;
                                ocTieu++;
                            }
                            break;
                        case 132:
                        case 144:
                            if (!isActSet) {
                                isActSet = true;
                                pikkoroDaimao++;
                            }
                            break;
                        case 130:
                        case 142:
                            if (!isActSet) {
                                isActSet = true;
                                picolo++;
                            }
                            break;
                        case 135:
                        case 138:
                            if (!isActSet) {
                                isActSet = true;
                                nappa++;
                            }
                            break;
                        case 133:
                        case 136:
                            if (!isActSet) {
                                isActSet = true;
                                kakarot++;
                            }
                            break;
                        case 134:
                        case 137:
                            if (!isActSet) {
                                isActSet = true;
                                cadic++;
                            }
                            break;
                        case 21:
                            if (io.param == 80) {
                                setDHD++;
                            }
                            break;
                        case 246:
                        case 215:
                            if (!isActSet) {
                                isActSet = true;
                                kaioken++;
                            }
                            break;
                        case 34:
                            if (io.param > 0) {
                                setTinhAn++;
                            }
                            break;
                        case 35:
                            if (io.param > 0) {
                                setNguyetAn++;
                            }
                            break;
                        case 36:
                            if (io.param > 0) {
                                setNhatAn++;
                            }
                            break;
                    }
                }
            }
        }
    }

    private void setupSKHNew() {
        if (this.player.inventory.itemsBody == null) {
            return;
        }
        int limit = Math.min(5, this.player.inventory.itemsBody.size());
        for (int i = 0; i < limit; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item != null && item.isNotNullItem() && item.itemOptions != null) {
                boolean isActSet = false;
                for (Item.ItemOption io : item.itemOptions) {
                    if (io == null || io.optionTemplate == null) {
                        continue;
                    }
                    switch (io.optionTemplate.id) {
                        case 242:
                        case 243:
                        case 244:
                        case 245:
                            if (!isActSet) {
                                isActSet = true;
                                thanVuTruKaio++;
                            }
                            break;
                        case 234:
                        case 235:
                        case 236:
                        case 237:
                            if (!isActSet) {
                                isActSet = true;
                                nail++;
                            }
                            break;
                        case 238:
                        case 239:
                        case 240:
                        case 241:
                            if (!isActSet) {
                                isActSet = true;
                                cadicM++;
                            }
                            break;
                    }
                }
            }
        }
    }

    private void setDefault() {
        this.songoku = 0;
        this.thienXinHang = 0;
        this.kirin = 0;
        this.kaioken = 0;
        this.ocTieu = 0;
        this.pikkoroDaimao = 0;
        this.picolo = 0;
        this.kakarot = 0;
        this.cadic = 0;
        this.nappa = 0;
        this.setDHD = 0;
        this.worldcup = 0;
        this.godClothes = false;
        this.ctHaiTac = -1;
        this.setNhatAn = 0;
        this.setTinhAn = 0;
        this.setNguyetAn = 0;
        this.thanVuTruKaio = 0;
        this.nail = 0;
        this.cadicM = 0;
    }

    public boolean checkSetGod() {
        if (this.player == null || this.player.inventory == null || this.player.inventory.itemsBody == null
                || this.player.inventory.itemsBody.size() < 5) {
            return false;
        }
        for (int i = 0; i < 5; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item != null && item.isNotNullItem()) {
                if (item.template.id < 555 || item.template.id > 567) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean checkSetDes() {
        if (this.player == null || this.player.inventory == null || this.player.inventory.itemsBody == null
                || this.player.inventory.itemsBody.size() < 5) {
            return false;
        }
        for (int i = 0; i < 5; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item != null && item.isNotNullItem()) {
                if (item.template.id < 650 || item.template.id > 662) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean checkOneItemDes() {
        if (this.player == null || this.player.inventory == null || this.player.inventory.itemsBody == null) {
            return false;
        }
        for (int i = 0; i < 5 && i < this.player.inventory.itemsBody.size(); i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item != null && item.isNotNullItem() && item.template.id >= 650 && item.template.id <= 662) {
                return true;
            }
        }
        return false;
    }

    public void dispose() {
        this.player = null;
    }
}
