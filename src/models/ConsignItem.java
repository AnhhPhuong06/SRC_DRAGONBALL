package models;

 

import models.item.Item.ItemOption;
import java.util.ArrayList;
import java.util.List;

public class ConsignItem {

    public int id;
    public short itemId;
    public int player_sell;
    public byte tab;
    public int goldSell;
    public int rubySell;  // Đổi từ gemSell thành rubySell
    public int quantity;
    public long lasttime;
    public List<ItemOption> options = new ArrayList<>();
    public boolean isBuy;

    public ConsignItem(int i, short id, int plId, byte t, int gold, int ruby, int q, long isTime, List<ItemOption> op, boolean b) {
        this.id = i;
        itemId = id;
        player_sell = plId;
        tab = t;
        goldSell = gold;
        rubySell = ruby;  // Đổi từ gem thành ruby
        quantity = q;
        lasttime = isTime;
        options = op;
        isBuy = b;
    }
}
