package server.web.panel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import database.AlyraManager;
import java.sql.*;
import java.util.*;
import models.intrinsic.Intrinsic;
import models.task.ClanTaskTemplate;
import models.task.SideTaskTemplate;
import models.task.TaskMain;
import server.Client;
import server.Manager;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.AccountPlayerStore.*;
import static server.web.panel.AccountDataApi.*;

final class PlayerDataApi {
    private static final List<String> RAW_COLUMNS = List.of("data_inventory", "data_location", "data_point", "data_magic_tree", "data_intrinsic", "data_task", "data_side_task", "data_clan_task", "data_charm", "skills", "skills_shortcut", "pet", "data_danh_hieu", "data_mabu_egg", "bill_data", "black_data", "data_black_ball");
    private static final List<String> CHARM_FIELDS = List.of("tdTriTue", "tdManhMe", "tdDaTrau", "tdOaiHung", "tdBatTu", "tdDeoDai", "tdThuHut", "tdDeTu", "tdTriTue3", "tdTriTue4");
    private PlayerDataApi() {}

    static Object handle(HttpExchange exchange) throws Exception {
        Map<String, String> q = query(exchange);
        if ("GET".equals(exchange.getRequestMethod())) return q.containsKey("id") ? detail(number(q.get("id"), 0)) : list(q);
        requireMethod(exchange, "POST");
        JsonObject b = body(exchange);
        String action = q.getOrDefault("action", "");
        int id = id(b);
        return switch (action) {
            case "basic" -> basic(id, b);
            case "point" -> point(id, b);
            case "inventory" -> inventory(id, b);
            case "location" -> location(id, b);
            case "magic-tree" -> magicTree(id, b);
            case "max-stats" -> maxStats(id, b);
            case "intrinsic" -> intrinsic(id, b, false);
            case "intrinsic-clear" -> intrinsic(id, b, true);
            case "cooldown-clear" -> cooldown(id, b);
            case "skills" -> skills(id, b);
            case "shortcut" -> shortcut(id, b);
            case "charms" -> charms(id, b);
            case "pet" -> pet(id, b, false);
            case "pet-delete" -> pet(id, b, true);
            case "task" -> task(id, b, "data_task");
            case "side-task" -> task(id, b, "data_side_task");
            case "clan-task" -> task(id, b, "data_clan_task");
            case "raw" -> raw(id, b);
            case "kick" -> kick(id);
            default -> throw error(400, "Tác vụ nhân vật không được hỗ trợ: " + action);
        };
    }

    private static Object list(Map<String, String> q) throws Exception {
        String search = q.getOrDefault("q", "").trim(); String filter = q.getOrDefault("filter", "");
        if (search.length() > 64 || !Set.of("", "online", "offline").contains(filter)) throw error(400, "Bộ lọc nhân vật không hợp lệ");
        String where = " WHERE 1=1"; List<Object> args = new ArrayList<>();
        if (!search.isEmpty()) { where += " AND (p.name LIKE ? OR CAST(p.id AS CHAR)=? OR a.username LIKE ?)"; String x = "%" + search + "%"; args.add(x); args.add(search); args.add(x); }
        try (Connection c = AlyraManager.getConnection()) {
            long total = scalar(c, "SELECT COUNT(*) FROM player p LEFT JOIN account a ON a.id=p.account_id" + where, args.toArray());
            List<Map<String, Object>> players = new ArrayList<>(); int online = 0;
            for (Map<String, Object> p : rows(c, "SELECT p.id,p.account_id,p.name,p.gender,p.head,p.rank,p.data_point,p.data_inventory,p.create_time,a.username,a.ban FROM player p LEFT JOIN account a ON a.id=p.account_id" + where + " ORDER BY p.id DESC LIMIT " + LIMIT, args.toArray())) {
                long pid = value(p, "id"); boolean isOnline = Client.gI().getPlayer(pid) != null; if (isOnline) online++;
                if (("online".equals(filter) && !isOnline) || ("offline".equals(filter) && isOnline)) continue;
                List<?> pt = array(p.get("data_point")); List<?> inv = array(p.get("data_inventory"));
                players.add(map("id", pid, "name", p.get("name"), "username", p.get("username"), "accountId", value(p, "account_id"),
                        "gender", value(p, "gender"), "genderName", gender(value(p, "gender")), "head", value(p, "head"), "rank", value(p, "rank"),
                        "power", at(pt, 1), "pointPvp", 0, "gold", at(inv, 0), "gem", at(inv, 1), "ruby", at(inv, 2),
                        "banned", value(p, "ban") != 0, "online", isOnline, "createTime", p.get("create_time")));
            }
            return map("players", players, "total", total, "online", online, "limit", LIMIT, "readInt", false);
        }
    }

    private static Object detail(int id) throws Exception {
        if (id <= 0) throw error(400, "ID nhân vật không hợp lệ");
        try (Connection c = AlyraManager.getConnection()) {
            Map<String, Object> p = one(c, "SELECT p.*,a.username,a.ban FROM player p LEFT JOIN account a ON a.id=p.account_id WHERE p.id=?", id);
            int genderId = (int) value(p, "gender");
            Map<String, Object> d = map("id", id, "online", Client.gI().getPlayer(id) != null,
                    "info", map("name", p.get("name"), "gender", genderId, "head", value(p, "head"), "rank", value(p, "rank"),
                            "pointPvp", 0, "accountId", value(p, "account_id"), "username", p.get("username"), "createTime", p.get("create_time")),
                    "inventory", inventoryData(array(p.get("data_inventory"))), "location", locationData(array(p.get("data_location"))),
                    "point", pointData(array(p.get("data_point"))), "magicTree", treeData(array(p.get("data_magic_tree"))),
                    "intrinsic", intrinsicData(array(p.get("data_intrinsic"))), "skills", skillsData(array(p.get("skills")), genderId),
                    "shortcut", array(p.get("skills_shortcut")), "charms", charmData(array(p.get("data_charm"))),
                    "pet", petData(array(p.get("pet"))), "task", taskData(array(p.get("data_task"))),
                    "sideTask", sideTaskData(array(p.get("data_side_task"))), "clanTask", sideTaskData(array(p.get("data_clan_task"))),
                    "raw", rawData(p));
            return map("player", d, "catalog", catalog(genderId));
        }
    }

    private static Map<String, Object> catalog(int gender) {
        List<Map<String, Object>> genders = List.of(map("id", 0, "name", "Trái Đất"), map("id", 1, "name", "Namếc"), map("id", 2, "name", "Xayda"));
        List<Map<String, Object>> intrinsics = new ArrayList<>(); for (Intrinsic i : Manager.INTRINSICS) intrinsics.add(map("id", i.id, "name", i.name, "from1", i.paramFrom1, "to1", i.paramTo1, "from2", i.paramFrom2, "to2", i.paramTo2));
        List<Map<String, Object>> taskMains = new ArrayList<>(); for (TaskMain t : Manager.TASKS) taskMains.add(map("id", t.id, "name", t.name));
        List<Map<String, Object>> side = new ArrayList<>(); for (SideTaskTemplate t : Manager.SIDE_TASKS_TEMPLATE) side.add(map("id", t.id, "name", t.name));
        List<Map<String, Object>> clan = new ArrayList<>(); for (ClanTaskTemplate t : Manager.CLAN_TASKS_TEMPLATE) clan.add(map("id", t.id, "name", t.name));
        List<Map<String, Object>> skills = new ArrayList<>(); if (gender >= 0 && gender < Manager.NCLASS.size()) for (models.Template.SkillTemplate s : Manager.NCLASS.get(gender).skillTemplatess) skills.add(map("id", (int) s.id, "name", s.name));
        List<Map<String, Object>> charms = new ArrayList<>(); int[] itemIds = {213,214,215,216,217,218,219,522,671,672}; for (int i = 0; i < itemIds.length; i++) charms.add(map("pos", i, "itemId", itemIds[i], "itemName", CHARM_FIELDS.get(i), "field", CHARM_FIELDS.get(i), "time", 0));
        List<Map<String, Object>> pointFields = List.of(map("key", "power", "label", "Sức mạnh"), map("key", "tiemNang", "label", "Tiềm năng"), map("key", "stamina", "label", "Thể lực"), map("key", "maxStamina", "label", "Thể lực tối đa"), map("key", "hpg", "label", "HP gốc +%"), map("key", "mpg", "label", "KI gốc +%"), map("key", "dameg", "label", "Sát thương +%"), map("key", "defg", "label", "Giáp +%"), map("key", "critg", "label", "Chí mạng +%"), map("key", "hp", "HP"), map("key", "mp", "label", "KI"));
        return map("genders", genders, "intrinsics", intrinsics, "taskMains", taskMains, "sideTasks", side, "clanTasks", clan, "skills", skills, "charms", charms, "pointFields", pointFields, "rawColumns", RAW_COLUMNS, "shortcutSlots", 10);
    }

    private static Object basic(int id, JsonObject b) throws Exception {
        keys(b, "name", "gender", "head", "rank");
        return editPlayer(id, (c, p) -> { Map<String, Object> f = new LinkedHashMap<>(); if (b.has("name")) f.put("name", limited(required(b, "name"), "name", 1, 20)); if (b.has("gender")) f.put("gender", bound(b, "gender", 0, 2)); if (b.has("head")) f.put("head", bound(b, "head", 0, 32767)); if (b.has("rank")) f.put("rank", bound(b, "rank", 0, Integer.MAX_VALUE)); update(c, "player", id, f); return ok("Đã lưu thông tin nhân vật"); });
    }

    private static Object point(int id, JsonObject b) throws Exception {
        keys(b, "limitPower", "power", "tiemNang", "stamina", "maxStamina", "hpg", "mpg", "dameg", "defg", "critg", "hp", "mp");
        return editPlayer(id, (c, p) -> { List<Object> a = mutableArray(p.get("data_point"), 13, "data_point"); set(a,b,"limitPower",0,0,10); set(a,b,"power",1,0,Long.MAX_VALUE); set(a,b,"tiemNang",2,0,Long.MAX_VALUE); set(a,b,"stamina",3,0,32767); set(a,b,"maxStamina",4,0,32767); set(a,b,"hpg",5,0,Long.MAX_VALUE); set(a,b,"mpg",6,0,Long.MAX_VALUE); set(a,b,"dameg",7,0,Long.MAX_VALUE); set(a,b,"defg",8,0,Integer.MAX_VALUE); set(a,b,"critg",9,0,127); set(a,b,"hp",11,0,Long.MAX_VALUE); set(a,b,"mp",12,0,Long.MAX_VALUE); AccountPlayerStore.execute(c,"UPDATE player SET data_point=? WHERE id=?", GSON.toJson(a),id); return ok("Đã lưu chỉ số nhân vật"); });
    }

    private static Object inventory(int id, JsonObject b) throws Exception { keys(b,"gold","gem","ruby"); return editPlayer(id,(c,p)->{ List<Object>a=mutableArray(p.get("data_inventory"),3,"data_inventory"); set(a,b,"gold",0,0,2_000_000_000_000L); set(a,b,"gem",1,0,Integer.MAX_VALUE); set(a,b,"ruby",2,0,Integer.MAX_VALUE); execute(c,"UPDATE player SET data_inventory=? WHERE id=?",GSON.toJson(a),id); return ok("Đã lưu tiền tệ"); }); }
    private static Object location(int id, JsonObject b) throws Exception { keys(b,"mapId","x","y"); return editPlayer(id,(c,p)->{ List<Object>a=mutableArray(p.get("data_location"),3,"data_location"); set(a,b,"mapId",0,0,10000); set(a,b,"x",1,-100000,100000); set(a,b,"y",2,-100000,100000); execute(c,"UPDATE player SET data_location=? WHERE id=?",GSON.toJson(a),id); return ok("Đã lưu vị trí nhân vật"); }); }
    private static Object magicTree(int id, JsonObject b) throws Exception { keys(b,"level","peas","upgrading"); return editPlayer(id,(c,p)->{ List<Object>a=mutableArray(p.get("data_magic_tree"),5,"data_magic_tree"); int level=(int)bound(b,"level",1,10); int max=(level-1)*2+5; int peas=(int)bound(b,"peas",0,max); a.set(0,level);a.set(1,peas);a.set(2,bool(b,"upgrading",false)?1:0); execute(c,"UPDATE player SET data_magic_tree=? WHERE id=?",GSON.toJson(a),id); return ok("Đã lưu đậu thần"); }); }
    private static Object maxStats(int id, JsonObject b) throws Exception { keys(b); return point(id, new JsonParser().parse("{\"power\":2147483647,\"tiemNang\":2147483647,\"hpg\":2147483647,\"mpg\":2147483647,\"dameg\":2147483647,\"defg\":2147483647,\"hp\":2147483647,\"mp\":2147483647}").getAsJsonObject()); }

    private static Object intrinsic(int id, JsonObject b, boolean clear) throws Exception { if (clear) keys(b); else keys(b,"intrinsicId","param1","param2","countOpen"); return editPlayer(id,(c,p)->{ List<Object>a=new ArrayList<>(List.of(0,0,0,0)); if(!clear){a.set(0,bound(b,"intrinsicId",0,255));a.set(1,bound(b,"param1",Short.MIN_VALUE,Short.MAX_VALUE));a.set(2,bound(b,"countOpen",0,127));a.set(3,bound(b,"param2",Short.MIN_VALUE,Short.MAX_VALUE));} execute(c,"UPDATE player SET data_intrinsic=? WHERE id=?",GSON.toJson(a),id); return ok(clear?"Đã xóa nội tại":"Đã lưu nội tại"); }); }
    private static Object cooldown(int id, JsonObject b) throws Exception { keys(b); return editPlayer(id,(c,p)->{ List<?> old=array(p.get("skills")); List<Object> out=new ArrayList<>(); for(Object x:old){List<Object>s=new ArrayList<>(array(x)); if(s.size()>=3)s.set(2,0);out.add(GSON.toJson(s));}execute(c,"UPDATE player SET skills=? WHERE id=?",GSON.toJson(out),id);return ok("Đã xóa cooldown kỹ năng");}); }
    private static Object skills(int id, JsonObject b) throws Exception { keys(b,"skills"); if(!b.has("skills")||!b.get("skills").isJsonArray())throw error(400,"skills phải là mảng"); return editPlayer(id,(c,p)->{List<?>old=array(p.get("skills"));List<JsonElement> in=new ArrayList<>(); b.getAsJsonArray("skills").forEach(in::add);if(in.size()!=old.size())throw error(400,"Không được thêm/bớt kỹ năng");List<Object>out=new ArrayList<>();for(int i=0;i<old.size();i++){List<Object>s=new ArrayList<>(array(old.get(i)));JsonObject v=in.get(i).getAsJsonObject();if(s.size()<4)throw error(400,"Dữ liệu kỹ năng hỏng");s.set(1,bound(v,"point",0,127));s.set(3,bound(v,"currLevel",0,32767));out.add(GSON.toJson(s));}execute(c,"UPDATE player SET skills=? WHERE id=?",GSON.toJson(out),id);return ok("Đã lưu kỹ năng");}); }
    private static Object shortcut(int id, JsonObject b) throws Exception { keys(b,"slots"); if(!b.has("slots")||!b.get("slots").isJsonArray())throw error(400,"slots phải là mảng"); return editPlayer(id,(c,p)->{List<JsonElement> in=new ArrayList<>(); b.getAsJsonArray("slots").forEach(in::add);if(in.size()>10)throw error(400,"Tối đa 10 ô nhanh");List<Object>out=new ArrayList<>();for(JsonElement e:in){long v=e.getAsLong();if(v<-1||v>127)throw error(400,"ID kỹ năng shortcut ngoài giới hạn");out.add(v);}while(out.size()<10)out.add(-1);execute(c,"UPDATE player SET skills_shortcut=? WHERE id=?",GSON.toJson(out),id);return ok("Đã lưu ô kỹ năng nhanh");}); }
    private static Object charms(int id, JsonObject b) throws Exception { keys(b,"charms"); if(!b.has("charms")||!b.get("charms").isJsonArray())throw error(400,"charms phải là mảng"); return editPlayer(id,(c,p)->{List<Object>a=mutableArray(p.get("data_charm"),10,"data_charm");for(JsonElement e:b.getAsJsonArray("charms")){JsonObject v=e.getAsJsonObject();int pos=(int)bound(v,"pos",0,9);a.set(pos,bound(v,"time",0,Long.MAX_VALUE));}execute(c,"UPDATE player SET data_charm=? WHERE id=?",GSON.toJson(a),id);return ok("Đã lưu bùa");}); }

    private static Object pet(int id, JsonObject b, boolean remove) throws Exception { if(remove)keys(b); else keys(b,"name","typePet","status","limitPower","power","tiemNang","stamina","maxStamina","hpg","mpg","dameg","defg","critg","hp","mp"); return editPlayer(id,(c,p)->{if(remove){execute(c,"UPDATE player SET pet='[]' WHERE id=?",id);return ok("Đã xóa đệ tử");}List<Object>a=mutableArray(p.get("pet"),4,"pet");if(a.size()<4)throw error(400,"Dữ liệu đệ tử hỏng");List<Object>info=new ArrayList<>(array(a.get(0))),pt=new ArrayList<>(array(a.get(1)));if(info.size()<6||pt.size()<12)throw error(400,"Dữ liệu đệ tử hỏng");set(info,b,"typePet",0,0,6);if(b.has("name"))info.set(2,limited(required(b,"name"),"name",1,50));set(info,b,"status",5,0,127);set(pt,b,"limitPower",0,0,10);set(pt,b,"power",1,0,Long.MAX_VALUE);set(pt,b,"tiemNang",2,0,Long.MAX_VALUE);set(pt,b,"stamina",3,0,32767);set(pt,b,"maxStamina",4,0,32767);set(pt,b,"hpg",5,0,Long.MAX_VALUE);set(pt,b,"mpg",6,0,Long.MAX_VALUE);set(pt,b,"dameg",7,0,Long.MAX_VALUE);set(pt,b,"defg",8,0,Integer.MAX_VALUE);set(pt,b,"critg",9,0,Integer.MAX_VALUE);set(pt,b,"hp",10,0,Long.MAX_VALUE);set(pt,b,"mp",11,0,Long.MAX_VALUE);a.set(0,GSON.toJson(info));a.set(1,GSON.toJson(pt));execute(c,"UPDATE player SET pet=? WHERE id=?",GSON.toJson(a),id);return ok("Đã lưu đệ tử");}); }
    private static Object task(int id, JsonObject b, String column) throws Exception { boolean main="data_task".equals(column); keys(b,main?new String[]{"taskMainId","index","count","lastTime"}:new String[]{"templateId","level","receivedTime","count","maxCount","leftTask"}); return editPlayer(id,(c,p)->{List<Object>a=new ArrayList<>();if(main){int tid=(int)bound(b,"taskMainId",1,Integer.MAX_VALUE);TaskMain t=findTask(tid);if(t==null)throw error(400,"Nhiệm vụ chính không tồn tại");int ix=(int)bound(b,"index",0,Math.max(0,t.subTasks.size()-1));a.add(tid);a.add(ix);a.add(bound(b,"count",0,Short.MAX_VALUE));a.add(bound(b,"lastTime",0,Long.MAX_VALUE));}else{long tid=bound(b,"templateId",-1,Integer.MAX_VALUE);if(tid>0&&findTemplate(column,(int)tid)==null)throw error(400,"Mẫu nhiệm vụ không tồn tại");a=List.of(tid,bound(b,"receivedTime",0,Long.MAX_VALUE),bound(b,"count",0,Integer.MAX_VALUE),bound(b,"maxCount",0,Integer.MAX_VALUE),bound(b,"leftTask",0,Integer.MAX_VALUE),bound(b,"level",1,Integer.MAX_VALUE));}execute(c,"UPDATE player SET "+column+"=? WHERE id=?",GSON.toJson(a),id);return ok("Đã lưu nhiệm vụ");}); }
    private static Object raw(int id, JsonObject b) throws Exception { keys(b,"column","value");String col=required(b,"column");if(!RAW_COLUMNS.contains(col))throw error(400,"Cột JSON không được hỗ trợ");String v=required(b,"value");JsonElement parsed;try{parsed=new JsonParser().parse(v);}catch(Exception e){throw error(400,"JSON không hợp lệ");}if(!parsed.isJsonArray()||v.length()>100000)throw error(400,"Giá trị phải là mảng JSON không quá 100KB");return editPlayer(id,(c,p)->{execute(c,"UPDATE player SET "+col+"=? WHERE id=?",v,id);return ok("Đã ghi cột "+col);}); }
    private static Object kick(int id) throws Exception {int aid=accountForPlayer(id);return AccountPlayerStore.kick(aid);}

    @FunctionalInterface private interface Edit { Object run(Connection c, Map<String,Object> p) throws Exception; }
    private static Object editPlayer(int id, Edit action) throws Exception { if(id<=0)throw error(400,"ID nhân vật không hợp lệ");int aid=accountForPlayer(id);return AccountPlayerStore.edit(aid,(c,a)->{Map<String,Object>p=one(c,"SELECT * FROM player WHERE id=? FOR UPDATE",id);if(value(p,"account_id")!=aid)throw error(409,"Nhân vật vừa đổi tài khoản");return action.run(c,p);}); }

    private static List<Object> mutableArray(Object v,int min,String col){List<?>old=array(v);if(old.size()<min)throw error(409,"Dữ liệu "+col+" không hợp lệ");return new ArrayList<>(old);}
    private static void set(List<Object>a,JsonObject b,String key,int index,long min,long max){if(!b.has(key))return;long v=bound(b,key,min,max);a.set(index,v);}
    private static void set(List<Object>a,JsonObject b,String key,int index,int min,int max){set(a,b,key,index,(long)min,(long)max);}
    private static Map<String,Object> inventoryData(List<?>a){return map("gold",at(a,0),"gem",at(a,1),"ruby",at(a,2));}
    private static Map<String,Object> locationData(List<?>a){return map("mapId",at(a,0),"x",at(a,1),"y",at(a,2));}
    private static Map<String,Object> pointData(List<?>a){String[]k={"limitPower","power","tiemNang","stamina","maxStamina","hpg","mpg","dameg","defg","critg","unused","hp","mp"};Map<String,Object>m=new LinkedHashMap<>();for(int i=0;i<k.length;i++)m.put(k[i],at(a,i));return m;}
    private static Map<String,Object> treeData(List<?>a){return map("level",at(a,0),"peas",at(a,1),"upgrading",num(at(a,2))!=0);}
    private static Map<String,Object> intrinsicData(List<?>a){return map("id",at(a,0),"param1",at(a,1),"countOpen",at(a,2),"param2",at(a,3));}
    private static List<Map<String,Object>> skillsData(List<?>a,int gender){List<Map<String,Object>>out=new ArrayList<>();for(Object x:a){List<?>s=array(x);int tid=(int)num(at(s,0));String n="Skill #"+tid;if(gender>=0&&gender<Manager.NCLASS.size()){models.Template.SkillTemplate st=Manager.NCLASS.get(gender).getSkillTemplate(tid);if(st!=null)n=st.name;}out.add(map("tempId",tid,"name",n,"point",at(s,1),"lastTimeUse",at(s,2),"currLevel",at(s,3)));}return out;}
    private static List<Map<String,Object>> charmData(List<?>a){int[]ids={213,214,215,216,217,218,219,522,671,672};List<Map<String,Object>>out=new ArrayList<>();for(int i=0;i<10;i++)out.add(map("pos",i,"itemId",ids[i],"itemName",CHARM_FIELDS.get(i),"field",CHARM_FIELDS.get(i),"time",at(a,i)));return out;}
    private static Map<String,Object> petData(List<?>a){if(a.isEmpty())return map("has",false);List<?>i=array(at(a,0)),p=array(at(a,1));return map("has",true,"typePet",at(i,0),"gender",at(i,1),"name",at(i,2),"typeFusion",at(i,3),"timeLeftFusion",at(i,4),"status",at(i,5),"limitPower",at(p,0),"power",at(p,1),"tiemNang",at(p,2),"stamina",at(p,3),"maxStamina",at(p,4),"hpg",at(p,5),"mpg",at(p,6),"dameg",at(p,7),"defg",at(p,8),"critg",at(p,9),"hp",at(p,10),"mp",at(p,11),"bodyCount",array(at(a,2)).size(),"skillCount",array(at(a,3)).size());}
    private static Map<String,Object> taskData(List<?>a){return map("taskMainId",at(a,0),"index",at(a,1),"count",at(a,2),"lastTime",at(a,3));}
    private static Map<String,Object> sideTaskData(List<?>a){return map("templateId",at(a,0),"receivedTime",at(a,1),"count",at(a,2),"maxCount",at(a,3),"leftTask",at(a,4),"level",at(a,5));}
    private static Map<String,Object> rawData(Map<String,Object>p){Map<String,Object>m=new LinkedHashMap<>();for(String c:RAW_COLUMNS)m.put(c,array(p.get(c)));return m;}
    private static long num(Object v){return v instanceof Number?n((Number)v):v==null?0:Long.parseLong(v.toString());}
    private static long n(Number n){return n.longValue();}
    private static TaskMain findTask(int id){for(TaskMain t:Manager.TASKS)if(t.id==id)return t;return null;}
    private static Object findTemplate(String col,int id){if("data_side_task".equals(col))for(SideTaskTemplate t:Manager.SIDE_TASKS_TEMPLATE)if(t.id==id)return t;for(ClanTaskTemplate t:Manager.CLAN_TASKS_TEMPLATE)if(t.id==id)return t;return null;}
}
