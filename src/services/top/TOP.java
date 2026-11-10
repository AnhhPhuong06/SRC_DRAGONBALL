package services.top;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import lombok.Builder;
import lombok.Data;
import models.player.Player;
import models.task.TaskMain;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import services.ItemService;
import services.TaskService;

@Data
@Builder
public class TOP {

    private Player player;
    protected String info1;
    protected String info2;
    private long lasttime;
    private int id_player;
    private int level;
    private long time;
    private long value;

    public synchronized TOP findPlayer(Connection con, int id) throws Exception {
        this.id_player = id;
        Player online = server.Client.gI().getPlayer(id);
        if (online != null) {
            this.player = online;
            return this;
        }
        try (PreparedStatement ps = con.prepareStatement("select * from player where id = '" + id + "' limit 1"); ResultSet rs = ps.executeQuery()) {
            if (rs.first()) {
                player = new Player();
                getInfo(rs);
                getBody(rs);
            }
        }
        return this;
    }

    private synchronized void getInfo(ResultSet rs) throws Exception {
        player.id = rs.getInt("id");
        player.name = rs.getString("name");
        player.head = rs.getShort("head");
        player.gender = rs.getByte("gender");
    }

    public synchronized TOP getPoint(ResultSet rs) throws Exception {
        Player online = (player != null) ? server.Client.gI().getPlayer((int) player.id) : null;
        if (online != null && online.nPoint != null) {
            return this;
        }
        if (player != null && player.nPoint != null) {
            player.nPoint.power = rs.getLong("sm");
        }
        return this;
    }

    public synchronized TOP getTask(ResultSet rs) {
        Player online = (player != null) ? server.Client.gI().getPlayer((int) player.id) : null;
        if (online != null && online.playerTask != null) {
            return this;
        }
        try {
            TaskMain taskMain = TaskService.gI().getTaskMainById(player, rs.getByte("nv"));
            if (taskMain != null) {
                taskMain.index = rs.getByte("nvp");
                if (taskMain.subTasks != null && taskMain.index >= 0 && taskMain.index < taskMain.subTasks.size()) {
                    taskMain.subTasks.get(taskMain.index).count = rs.getShort("count");
                }
                taskMain.lastTime = rs.getLong("time");
                player.playerTask.taskMain = taskMain;
            }
        } catch (Exception ignored) {
        }
        return this;
    }

    private synchronized void getBody(ResultSet rs) {
        // Đảm bảo luôn có ít nhất 10 ô null item để getHead/getBody/getLeg không bị IndexOutOfBounds
        for (int i = 0; i < 10; i++) {
            player.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
        try {
            String itemsBodyStr = rs.getString("items_body");
            if (itemsBodyStr != null) {
                JSONArray dataArray = (JSONArray) JSONValue.parse(itemsBodyStr);
                if (dataArray != null && dataArray.size() > 5) {
                    Object obj = dataArray.get(5);
                    if (obj != null) {
                        JSONArray dataCaiTrang = (JSONArray) JSONValue.parse(obj.toString());
                        if (dataCaiTrang != null && !dataCaiTrang.isEmpty()) {
                            short tempId = Short.parseShort(String.valueOf(dataCaiTrang.get(0)));
                            if (tempId != -1) {
                                player.inventory.itemsBody.set(5, ItemService.gI().createNewItem(tempId, Integer.parseInt(String.valueOf(dataCaiTrang.get(1)))));
                            }
                        }
                    }
                }
                if (dataArray != null && dataArray.size() > 0) {
                    Object obj0 = dataArray.get(0);
                    if (obj0 != null) {
                        JSONArray dataAo = (JSONArray) JSONValue.parse(obj0.toString());
                        if (dataAo != null && !dataAo.isEmpty()) {
                            short tempId = Short.parseShort(String.valueOf(dataAo.get(0)));
                            if (tempId != -1) {
                                player.inventory.itemsBody.set(0, ItemService.gI().createNewItem(tempId, 1));
                            }
                        }
                    }
                }
                if (dataArray != null && dataArray.size() > 1) {
                    Object obj1 = dataArray.get(1);
                    if (obj1 != null) {
                        JSONArray dataQuan = (JSONArray) JSONValue.parse(obj1.toString());
                        if (dataQuan != null && !dataQuan.isEmpty()) {
                            short tempId = Short.parseShort(String.valueOf(dataQuan.get(0)));
                            if (tempId != -1) {
                                player.inventory.itemsBody.set(1, ItemService.gI().createNewItem(tempId, 1));
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public synchronized TOP setInfo1(String info) {
        info1 = info;
        return this;
    }

    public synchronized TOP setInfo2(String info) {
        info2 = info;
        return this;
    }

    public void dispose() {
        // KHÔNG gọi player.dispose(): người chơi online trong Client.gI().getPlayers()
        // sẽ bị huỷ đồ, pet, và đánh dấu disposed nếu gọi dispose() tại đây!
        player = null;
        info1 = null;
        info2 = null;
    }
}
