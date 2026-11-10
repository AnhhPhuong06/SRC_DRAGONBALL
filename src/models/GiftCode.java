package models;

 


import models.item.Item.ItemOption;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import models.player.Player;

public class GiftCode {

    public String code;
    public int countLeft;
    public int id;
    public boolean allGender;
    public HashMap<Integer, Integer> detail = new HashMap<>();
    public HashMap<Integer, ArrayList<ItemOption>> option = new HashMap<>();
    public Timestamp datecreate;
    public Timestamp dateexpired;
    /** Nếu khác null/rỗng: chỉ player có tên trùng (case-insensitive) mới nhập được. */
    public String playerName;

    public boolean isUsedGiftCode(Player player) {
        return player.giftCode.isUsedGiftCode(code);
    }

    public boolean timeCode() {
        if (dateexpired == null) return false;
        return System.currentTimeMillis() > dateexpired.getTime();
    }
}
