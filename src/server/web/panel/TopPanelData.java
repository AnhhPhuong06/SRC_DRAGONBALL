package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import database.AlyraManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.LongSupplier;
import models.item.Item;
import services.ItemService;
import services.top.*;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.WorldData.*;

/** Management of the server's existing top_template campaigns, not a separate reward system. */
final class TopPanelData {
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    record Row(int id, String name, String query, String date, String items, String users,
               boolean done, boolean auto, int limit) {}
    record Catalog(List<Map<String,Object>> items, List<Map<String,Object>> options) {}
    @FunctionalInterface interface Edit { Row apply(Row old) throws Exception; }
    interface Store {
        List<Row> list() throws Exception;
        Catalog catalog() throws Exception;
        List<Map<String,Object>> board(Row row) throws Exception;
        boolean edit(int id, String version, Edit edit) throws Exception;
        default List<Map<String,Object>> extraBoards() throws Exception {return List.of();}
    }
    @FunctionalInterface interface Connections { Connection open() throws SQLException; }
    private TopPanelData() {}
    static Object handle(HttpExchange exchange) throws Exception {
        return handle(exchange, new JdbcStore(), System::currentTimeMillis);
    }
    static Object handle(HttpExchange exchange, Store store, LongSupplier clock) throws Exception {
        requireMethod(exchange,"GET","POST");
        // This is also the lock used by the game refresh, manual awards and automatic awards.
        synchronized (TopService.gI()) {
            if (exchange.getRequestMethod().equals("GET")) {
                List<Map<String,Object>> tops = new ArrayList<>();
                for (Row row : store.list()) {
                    String problem=""; JsonArray gifts=new JsonArray(), users=new JsonArray();
                    try {gifts=rewards(parseArray(row.items(),"Quà top"),null);users=recipients(row.users());}
                    catch(PanelException invalid){problem=invalid.getMessage();}
                    List<Map<String,Object>> board=List.of();String boardError="";
                    try {board=store.board(row);} catch(Exception failure){boardError="Chưa đọc được bảng xếp hạng. Kiểm tra dữ liệu và truy vấn top trên server.";}
                    tops.add(dto("id",row.id(),"name",row.name(),"date",row.date(),"limit",row.limit(),"auto",row.auto(),"done",row.done(),
                            "version",version(row),"rewards",gifts,"recipients",users,"ranking",board,"boardError",boardError,"error",problem,
                            "supported",row.id()>=0&&row.id()<=2,"runtime",live(row.id())!=null,
                            "due",due(row,clock.getAsLong()),"sending",live(row.id())!=null&&live(row.id()).isSend));
                }
                Catalog catalog=store.catalog();
                return dto("tops",tops,"extraBoards",store.extraBoards(),"catalog",dto("items",catalog.items(),"options",catalog.options()),
                        "serverTime",LocalDateTime.ofInstant(Instant.ofEpochMilli(clock.getAsLong()),ZoneId.systemDefault()).format(DATE),
                        "timezone",ZoneId.systemDefault().getId(),
                        "notes","Top Nạp, Sức Mạnh và Nhiệm Vụ có thể đặt quà và lịch trao qua hòm thư trong game. "
                                + "Tiến độ đã nhận thuộc đợt hiện tại; server xóa danh sách này khi trao đủ. "
                                + "Các top Bóng Master, Lượt Quay, Săn Boss sự kiện và Máy Đấm được xem ở bảng riêng bên dưới.");
            }
            JsonObject body=body(exchange);
            int id=field(body,"id",-1,0,2);
            String action=query(exchange).getOrDefault("action",text(body,"action",""));
            if (action.equals("award")) {
                Row selected=store.list().stream().filter(row->row.id()==id).findFirst().orElseThrow(()->error(404,"Không tìm thấy top"));
                if(!version(selected).equals(required(body,"version")))throw error(409,"Cấu hình hoặc tiến độ top đã thay đổi. Tải lại trước khi trao.");
                if(selected.done()||!selected.auto()||!due(selected,clock.getAsLong())||live(id)==null)
                    throw error(409,"Chỉ trao top đã đến hạn, đang bật tự động, chưa hoàn tất và đã được game nạp.");
                coverage(rewards(parseArray(selected.items(),"Quà top"),store.catalog()),selected.limit());
                return ServerRuntimeApi.rewardTop(id);
            }
            if(!Set.of("save","toggle","new-cycle").contains(action))throw error(400,"Tác vụ top không hợp lệ");
            String expected=required(body,"version");
            Catalog catalog=action.equals("toggle")&&!bool(body,"enabled",false)?null:store.catalog();
            boolean applied=store.edit(id,expected,old->change(old,body,action,catalog,clock.getAsLong()));
            return changed(applied?"Đã lưu top vào DB và cập nhật lịch/quà trong game.":"Đã lưu top vào DB. Server chưa nạp top này; cấu hình dùng khi server khởi động.",!applied);
        }
    }
    static Row change(Row old,JsonObject body,String action,Catalog catalog,long now) {
        JsonArray users=recipients(old.users());
        if(action.equals("toggle")) {
            boolean enabled=bool(body,"enabled",false);
            if(enabled) {
                if(old.done())throw error(409,"Đợt top đã hoàn tất. Mở đợt mới trước khi bật tự động.");
                if(old.date().isEmpty())throw error(422,"Cần đặt ngày trao thưởng trước khi bật tự động.");
                if(date(old.date()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()<now-3L*86400000)
                    throw error(422,"Lịch đã quá hạn 3 ngày. Tạm dừng và đặt lại lịch khi chưa có người nhận.");
                coverage(rewards(parseArray(old.items(),"Quà top"),catalog),old.limit());
            }
            return new Row(old.id(),old.name(),old.query(),old.date(),old.items(),old.users(),old.done(),enabled,old.limit());
        }
        boolean newCycle=action.equals("new-cycle");
        if(newCycle && (!old.done()||users.size()!=0))throw error(409,"Chỉ mở đợt mới khi đợt trước đã hoàn tất và không còn tiến độ đang trao.");
        if(!newCycle && (old.done()||users.size()>0||old.auto()&&due(old,now)))
            throw error(409,"Đợt đã trao/đã đến hạn đang bật auto không thể đổi quà hoặc lịch. Tạm dừng trước khi sửa; không xóa tiến độ đã nhận.");
        String name=name(body,"name",null,25).trim();
        String when=required(body,"date");
        LocalDateTime parsed=date(when);
        if(parsed.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()<=now)
            throw error(422,"Ngày trao thưởng mới phải sau thời gian hiện tại của server.");
        int limit=field(body,"limit",-1,1,100);
        if(!body.has("rewards")||!body.get("rewards").isJsonArray())throw error(400,"Thiếu danh sách quà theo hạng");
        JsonArray gifts=rewards(body.getAsJsonArray("rewards"),catalog);
        boolean enabled=newCycle?false:old.auto();
        if(enabled)coverage(gifts,limit);
        return new Row(old.id(),name,old.query(),parsed.format(DATE),gifts.toString(),old.users(),false,enabled,limit);
    }
    static JsonArray rewards(JsonArray input,Catalog catalog) {
        if(input.size()>100)throw error(400,"Tối đa 100 hạng nhận quà");
        TreeMap<Integer,JsonObject> sorted=new TreeMap<>();
        for(JsonElement entry:input) {
            if(!entry.isJsonObject())throw error(400,"Quà top phải gồm hạng và vật phẩm");
            JsonObject group=entry.getAsJsonObject();
            int rank=field(group,"top",-1,1,100);
            if(sorted.containsKey(rank))throw error(400,"Trùng hạng nhận quà: "+rank);
            JsonArray items=new JsonArray();
            for(JsonElement item:arrayField(group,"items",20)) {
                if(!item.isJsonObject())throw error(400,"Vật phẩm thưởng không hợp lệ");
                JsonObject value=item.getAsJsonObject();
                int id=field(value,"id",-1,0,32767), quantity=field(value,"quantity",-1,1,99999);
                if(catalog!=null)requireId(catalog.items(),id,"Vật phẩm");
                Set<Integer> seen=new HashSet<>();JsonArray options=new JsonArray();
                for(JsonElement option:arrayField(value,"options",32)) {
                    if(!option.isJsonObject())throw error(400,"Option thưởng không hợp lệ");
                    JsonObject o=option.getAsJsonObject();int optionId=field(o,"id",-1,0,255);
                    if(!seen.add(optionId))throw error(400,"Trùng option trên cùng vật phẩm: "+optionId);
                    if(catalog!=null)requireId(catalog.options(),optionId,"Option");
                    options.add(GSON.toJsonTree(dto("id",optionId,"param",field(o,"param",-1,0,65535))));
                }
                items.add(GSON.toJsonTree(dto("id",id,"quantity",quantity,"options",options)));
            }
            sorted.put(rank,GSON.toJsonTree(dto("top",rank,"items",items)).getAsJsonObject());
        }
        JsonArray result=new JsonArray();sorted.values().forEach(result::add);return result;
    }
    private static void coverage(JsonArray rewards,int limit) {
        if(limit<1||limit>100)throw error(422,"Số hạng nhận quà phải từ 1 đến 100");
        Set<Integer> ranks=new HashSet<>();
        for(JsonElement element:rewards)if(element.getAsJsonObject().getAsJsonArray("items").size()>0)
            ranks.add(element.getAsJsonObject().get("top").getAsInt());
        for(int rank=1;rank<=limit;rank++)if(!ranks.contains(rank))throw error(422,"Cần cấu hình ít nhất 1 vật phẩm cho hạng "+rank+" trước khi bật tự động.");
    }
    private static void requireId(List<Map<String,Object>> rows,int id,String label) {
        if(rows.stream().noneMatch(row->n(row,"id")==id))throw error(422,label+" không tồn tại: "+id);
    }
    private static JsonArray arrayField(JsonObject object,String key,int max) {
        if(!object.has(key)||!object.get(key).isJsonArray()||object.getAsJsonArray(key).size()>max)throw error(400,"Danh sách không hợp lệ: "+key);
        return object.getAsJsonArray(key);
    }
    static JsonArray parseArray(String raw,String label) {
        try {JsonElement value=new JsonParser().parse(raw==null?"[]":raw);if(value.isJsonArray())return value.getAsJsonArray();}
        catch(RuntimeException ignored){}
        throw error(409,label+" trong DB không đúng định dạng JSON. Không ghi đè dữ liệu lỗi.");
    }
    static JsonArray recipients(String raw) {
        JsonArray result=parseArray(raw,"Tiến độ nhận thưởng");Set<Integer> seen=new HashSet<>();
        for(JsonElement value:result) {
            if(!value.isJsonObject())throw error(409,"Tiến độ nhận thưởng không hợp lệ");
            JsonObject user=value.getAsJsonObject();int id=field(user,"id",-1,1,Integer.MAX_VALUE);
            field(user,"top",-1,1,100);if(!seen.add(id))throw error(409,"Tiến độ trùng người nhận");
        }
        return result;
    }
    static LocalDateTime date(String input) {
        try {LocalDateTime date=LocalDateTime.parse(input);if(date.getYear()<2000||date.getYear()>9999)throw new IllegalArgumentException();return date;}
        catch(RuntimeException invalid){throw error(400,"Ngày giờ trao thưởng không hợp lệ");}
    }
    static boolean due(Row row,long now) {return !row.date().isEmpty()&&date(row.date()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()<=now;}
    static String version(Row row) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(GSON.toJson(row).getBytes(StandardCharsets.UTF_8)));}
        catch(Exception impossible){throw new IllegalStateException(impossible);}
    }
    static TOPAUTO live(int id) {for(TOPAUTO top:TopManager.AUTO_GIFT_TOPS)if(top.template!=null&&top.template.id==id)return top;return null;}

    static final class JdbcStore implements Store {
        private final Connections game, data;
        JdbcStore(){this(AlyraManager::getConnection,AlyraManager::getConnection_Data);}
        JdbcStore(Connections game,Connections data){this.game=game;this.data=data;}
        public List<Row> list() throws Exception {
            try(Connection c=game.open();PreparedStatement p=c.prepareStatement("SELECT * FROM top_template ORDER BY id");ResultSet r=p.executeQuery()) {
                List<Row> out=new ArrayList<>();while(r.next())out.add(read(r));return out;
            }
        }
        static Row read(ResultSet r)throws SQLException {
            Timestamp when=r.getTimestamp("date");
            return new Row(r.getInt("id"),r.getString("name"),r.getString("query"),when==null?"":when.toLocalDateTime().format(DATE),
                    r.getString("items"),r.getString("users"),r.getBoolean("isDone"),r.getBoolean("isAuto"),r.getInt("limit"));
        }
        public Catalog catalog() throws Exception {
            try(Connection c=data.open()) {
                var items=rows(c,"SELECT id,name,icon_id AS icon FROM item_template WHERE id BETWEEN 0 AND 32767 ORDER BY id");
                var options=rows(c,"SELECT id,name FROM item_option_template WHERE id BETWEEN 0 AND 255 ORDER BY id");
                for(var option:options)option.put("name",ItemService.resolveOptionName(n(option,"id"),s(option,"name")));
                return new Catalog(items,options);
            }
        }
        public List<Map<String,Object>> board(Row row)throws Exception {
            TOPAUTO active=live(row.id());
            if(active!=null) {
                List<Map<String,Object>> out=new ArrayList<>();int rank=0;
                for(TOP top:active.template.tops)if(top!=null&&top.getPlayer()!=null) {
                    var player=top.getPlayer();out.add(dto("rank",++rank,"id",player.id,"name",player.name,"gender",player.gender,
                            "score",row.id()==2?Objects.toString(top.getInfo1(),""):Long.toString(top.getValue()),"source","Game đang chạy"));
                }
                return out;
            }
            String query=row.query()==null?"":row.query().trim();
            if(!query.toUpperCase(Locale.ROOT).startsWith("SELECT ")||query.replaceFirst(";\\s*$","").contains(";"))throw error(409,"Truy vấn top phải là SELECT");
            try(Connection c=game.open()) {
                c.setReadOnly(true);
                List<Map<String,Object>> out=new ArrayList<>();
                try(PreparedStatement p=c.prepareStatement(query)) {
                    p.setMaxRows(100);p.setQueryTimeout(5);
                    try(ResultSet r=p.executeQuery()) {
                        int rank=0;
                        while(r.next()) {
                            String score=switch(row.id()){case 0->r.getString("tongnap");case 1->r.getString("sm");case 2->"NV "+r.getInt("nv")+" · bước "+r.getInt("nvp")+" · "+r.getInt("count");default->"";};
                            out.add(dto("rank",++rank,"id",r.getInt("id"),"name","","gender",3,"score",score,"source","DB · server chưa nạp top"));
                        }
                    }
                }
                if(!out.isEmpty()) {
                    String placeholders=String.join(",",Collections.nCopies(out.size(),"?"));
                    Object[] ids=out.stream().map(r->r.get("id")).toArray();
                    var players=rows(c,"SELECT id,name,gender FROM player WHERE id IN ("+placeholders+")",ids);
                    for(var entry:out)for(var player:players)if(n(entry,"id")==n(player,"id")){entry.put("name",s(player,"name"));entry.put("gender",n(player,"gender"));}
                }
                return out;
            }
        }
        public List<Map<String,Object>> extraBoards()throws Exception {
            String[][] definitions={
                    {"pokemon","Bóng Master","point_pokemon","0"},
                    {"quay","Lượt Quay","dien_sukien","2"},
                    {"boss-event","Săn Boss sự kiện","daubuoi","0"},
                    {"may-dam","Máy Đấm","loadtimetop","1"}};
            List<Map<String,Object>> boards=new ArrayList<>();
            try(Connection c=game.open()) {
                c.setReadOnly(true);
                for(String[] definition:definitions) {
                    List<Map<String,Object>> result=new ArrayList<>();String failure="";
                    String field=definition[2];
                    // These expressions are the same saved fields read by the game loaders.
                    String sql="SELECT id,name,gender,CAST(JSON_EXTRACT("+field+",'$["+definition[3]+"]') AS UNSIGNED) AS score FROM player"
                            +(definition[0].equals("may-dam")?" WHERE loadtimetop IS NOT NULL AND JSON_VALID(loadtimetop)":"")+" ORDER BY score DESC LIMIT 100";
                    try(PreparedStatement p=c.prepareStatement(sql)) {
                        p.setQueryTimeout(5);try(ResultSet r=p.executeQuery()){int rank=0;while(r.next())result.add(dto("rank",++rank,"id",r.getInt("id"),"name",r.getString("name"),"gender",r.getInt("gender"),"score",Objects.toString(r.getString("score"),"0")));}
                    }catch(SQLException invalid){failure="Không đọc được bảng top. Kiểm tra dữ liệu điểm trong DB.";}
                    boards.add(dto("key",definition[0],"name",definition[1],"ranking",result,"error",failure));
                }
            }
            return boards;
        }
        public boolean edit(int id,String expected,Edit edit)throws Exception {
            try(Connection c=game.open()) {
                c.setAutoCommit(false);
                try {
                    Row old;
                    try(PreparedStatement p=c.prepareStatement("SELECT * FROM top_template WHERE id=? FOR UPDATE")) {
                        p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw error(404,"Không tìm thấy top");old=read(r);}
                    }
                    if(!version(old).equals(expected))throw error(409,"Top hoặc tiến độ nhận thưởng đã thay đổi. Tải lại trước khi lưu.");
                    TOPAUTO active=live(id);
                    if(active!=null&&(active.isSend||!runtimeUsers(active).equals(recipientMap(recipients(old.users())))))
                        throw error(409,"Top đang trao hoặc tiến độ trong game chưa đồng bộ DB. Thử lại sau khi trao xong.");
                    Row next=edit.apply(old);TOPAUTO staged=null;
                    if(active!=null)for(JsonElement group:rewards(parseArray(next.items(),"Quà top"),null))
                        for(JsonElement element:group.getAsJsonObject().getAsJsonArray("items")) {
                            JsonObject item=element.getAsJsonObject();int itemId=item.get("id").getAsInt();
                            if(ItemService.gI().getTemplate(itemId)==null)throw error(422,"Vật phẩm "+itemId+" chưa được game nạp. Khởi động lại server trước khi dùng vật phẩm mới.");
                            for(var option:item.getAsJsonArray("options"))if(ItemService.gI().getItemOptionTemplate(option.getAsJsonObject().get("id").getAsInt())==null)
                                throw error(422,"Option chưa được game nạp. Khởi động lại server trước khi dùng option mới.");
                        }
                    try(PreparedStatement p=c.prepareStatement("UPDATE top_template SET name=?,date=?,items=?,isDone=?,isAuto=?,`limit`=? WHERE id=?")) {
                        p.setString(1,next.name());p.setTimestamp(2,next.date().isEmpty()?null:Timestamp.valueOf(date(next.date())));p.setString(3,next.items());
                        p.setString(4,Boolean.toString(next.done()));p.setString(5,Boolean.toString(next.auto()));p.setInt(6,next.limit());p.setInt(7,id);
                        if(p.executeUpdate()!=1)throw error(409,"Top đã thay đổi trong lúc lưu");
                    }
                    if(active!=null)try(PreparedStatement p=c.prepareStatement("SELECT * FROM top_template WHERE id=?")) {
                        p.setInt(1,id);try(ResultSet r=p.executeQuery()){r.next();staged=new TOPAUTO(r,new TopManager.TopTemplate(id,next.name(),old.query()));}
                    }
                    c.commit();
                    if(active!=null) {
                        active.template.name=next.name();active.date=staged.date;active.limit=staged.limit;
                        active.isAuto=staged.isAuto;active.isDone=staged.isDone;active.items=staged.items;active.info=staged.info;
                        TopManager.setSelectNpc();TopAutoService.gI().activeAuto();
                    }
                    return active!=null;
                } catch(Exception failure){c.rollback();throw failure;}
            }
        }
        private static Map<Integer,Integer> runtimeUsers(TOPAUTO top) {
            return new HashMap<>(top.users);
        }
    }
    static Map<Integer,Integer> recipientMap(JsonArray users) {
        Map<Integer,Integer> result=new HashMap<>();for(var element:users){var user=element.getAsJsonObject();result.put(user.get("id").getAsInt(),user.get("top").getAsInt());}return result;
    }
}
