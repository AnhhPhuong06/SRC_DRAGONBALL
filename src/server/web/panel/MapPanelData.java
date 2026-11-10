package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import database.AlyraManager;
import java.sql.*;
import java.util.*;
import models.Template.MapTemplate;
import models.map.EffectMap;
import models.map.WayPoint;
import server.Manager;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.WorldData.*;

/** map_template editing; topology/spawns are deliberately never rebuilt under occupied zones. */
final class MapPanelData {
    static final List<Map<String, Object>> TYPES = choices("Bình thường", "Offline", "Doanh trại", "Ngọc rồng đen", "Kho báu", "Ma Bư", "Đường rắn", "Khí gas", "Karin", "Ma Bư 14h");
    static final String SELECT = "SELECT * FROM map_template";
    private MapPanelData() {}

    static Object handle(HttpExchange e) throws Exception {
        Map<String, String> q = query(e);
        if ("GET".equals(e.getRequestMethod())) {
            try (Connection c = AlyraManager.getConnection_Data()) {
                List<Map<String, Object>> all = rows(c, SELECT + " ORDER BY id");
                Map<Integer, String> names = new HashMap<>();
                for (var r : all) names.put(n(r, "id"), s(r, "name"));
                Map<Integer, String> mobNames = new HashMap<>(), npcNames = new HashMap<>();
                var mobs = rows(c, "SELECT id,NAME AS name,hp FROM mob_template ORDER BY id");
                var npcs = rows(c, "SELECT id,NAME AS name FROM npc_template ORDER BY id");
                for (var r : mobs) mobNames.put(n(r, "id"), s(r, "name"));
                for (var r : npcs) npcNames.put(n(r, "id"), s(r, "name"));
                if (q.containsKey("id")) {
                    int id = number(q.get("id"), -1);
                    for (var r : all) if (n(r, "id") == id) {
                        Map<String, Object> out = view(r, names, mobNames, npcNames);
                        return dto("map", out, "catalog", dto("types", TYPES, "planets", PLANETS,
                                "genders", List.of(dto("id", -1, "name", "Không có trong schema")),
                                "mobs", mobs, "npcs", npcs, "waypointTargets", all.stream().map(a -> dto("id", n(a,"id"), "name", s(a,"name"))).toList()));
                    }
                    throw error(404, "Không tìm thấy bản đồ");
                }
                List<Map<String, Object>> filtered = new ArrayList<>();
                for (var r : all) if (matches(q, n(r,"id"), s(r,"name")) && filter(q,"type",n(r,"type")) && filter(q,"planet",n(r,"planet_id")))
                    filtered.add(view(r, names, mobNames, npcNames));
                int total = filtered.size(), limit = limit(q);
                return dto("maps", filtered.subList(0, Math.min(limit, total)), "total", total, "limit", limit,
                        "types", TYPES, "planets", PLANETS, "genderTypeSupported", false);
            }
        }
        JsonObject b = body(e);
        String action = q.getOrDefault("action", text(b,"action",""));
        if ("reload".equals(action)) return reload();
        if ("map-create".equals(action) || "map-delete".equals(action))
            throw error(409, "Không thêm/xóa map: vị trí danh sách client và tài nguyên tile phải giữ nguyên");
        if (!Set.of("map-update", "spawns", "waypoints").contains(action)) throw error(400, "Tác vụ map không hợp lệ");
        int id = id(b);
        transaction(true, c -> {
            Map<String, Object> old = one(c, SELECT + " WHERE id=? FOR UPDATE", id);
            if ("map-update".equals(action)) {
                if (b.has("genderType") && integer(b,"genderType",-1) != -1)
                    throw error(422,"map_template không có genderType; hãy bỏ trường này");
                String name = name(b,"name",s(old,"name"),55);
                int type = field(b,"type",n(old,"type"),0,9);
                int planet = field(b,"planetId",n(old,"planet_id"),0,3);
                int zones = field(b,"zones",n(old,"zones"),1,127);
                int max = field(b,"maxPlayer",n(old,"max_player"),1,127);
                int bgType = field(b,"bgType",n(old,"bg_type"),0,127);
                int tile = field(b,"tileId",n(old,"tile_id"),1,127);
                int bg = field(b,"bgId",n(old,"bg_id"),0,127);
                update(c,"UPDATE map_template SET name=?,type=?,planet_id=?,zones=?,max_player=?,bg_type=?,tile_id=?,bg_id=? WHERE id=?",
                        name,type,planet,zones,max,bgType,tile,bg,id);
            } else if ("spawns".equals(action)) {
                if (!b.has("mobs") && !b.has("npcs")) throw error(400,"Cần mobs hoặc npcs");
                if (b.has("mobs")) update(c,"UPDATE map_template SET mobs=? WHERE id=?", encodeMobs(c,b).toString(),id);
                if (b.has("npcs")) update(c,"UPDATE map_template SET npcs=? WHERE id=?", encodeNpcs(c,b,storedArray(s(old,"npcs"),false)).toString(),id);
            } else update(c,"UPDATE map_template SET waypoints=? WHERE id=?",encodeWaypoints(c,b).toString(),id);
            return null;
        });
        // Persist first. Never pretend a best-effort live reload succeeded.
        return changed("Đã lưu DB; cấu trúc map, NPC, quái và cổng chỉ áp dụng sau khi khởi động lại an toàn.", true,
                "id",id,"liveApplied",false,"reloadAction","reload");
    }

    private static Map<String, Object> view(Map<String, Object> r, Map<Integer,String> maps, Map<Integer,String> mobs, Map<Integer,String> npcs) {
        int type=n(r,"type"), planet=n(r,"planet_id");
        List<Map<String,Object>> ms=new ArrayList<>(), ns=new ArrayList<>(), ws=new ArrayList<>();
        for (JsonElement el:storedArray(s(r,"mobs"),false)) {
            JsonArray a=el.getAsJsonArray();
            ms.add(dto("tempId",a.get(0).getAsInt(),"tempName",mobs.getOrDefault(a.get(0).getAsInt(),"?"),"level",a.get(1).getAsInt(),"hp",a.get(2).getAsLong(),"x",a.get(3).getAsInt(),"y",a.get(4).getAsInt()));
        }
        for (JsonElement el:storedArray(s(r,"npcs"),false)) {
            JsonArray a=el.getAsJsonArray();
            ns.add(dto("npcId",a.get(0).getAsInt(),"npcName",npcs.getOrDefault(a.get(0).getAsInt(),"?"),"x",a.get(1).getAsInt(),"y",a.get(2).getAsInt()));
        }
        for (JsonElement el:storedArray(s(r,"waypoints"),true)) {
            JsonArray a=el.getAsJsonArray();
            ws.add(dto("name",a.get(0).getAsString(),"minX",a.get(1).getAsInt(),"minY",a.get(2).getAsInt(),"maxX",a.get(3).getAsInt(),"maxY",a.get(4).getAsInt(),
                    "isEnter",a.get(5).getAsInt()==1,"isOffline",a.get(6).getAsInt()==1,"goMap",a.get(7).getAsInt(),"goMapName",maps.getOrDefault(a.get(7).getAsInt(),"?"),"goX",a.get(8).getAsInt(),"goY",a.get(9).getAsInt()));
        }
        return dto("id",n(r,"id"),"name",s(r,"name"),"type",type,"typeName", type>=0 && type<TYPES.size()?TYPES.get(type).get("name"):"Loại "+type,
                "planetId",planet,"planetName",planet>=0 && planet<PLANETS.size()?PLANETS.get(planet).get("name"):"Khác", "genderType",-1,"genderName","Không giới hạn (không có cột gender)",
                "zones",n(r,"zones"),"maxPlayer",n(r,"max_player"),"bgType",n(r,"bg_type"),"tileId",n(r,"tile_id"),"bgId",n(r,"bg_id"),"data",s(r,"data"),
                "mobs",ms,"npcs",ns,"waypoints",ws,"mobCount",ms.size(),"npcCount",ns.size(),"waypointCount",ws.size(),"needsRestart",true);
    }
    private static JsonArray encodeMobs(Connection c,JsonObject b) throws Exception {
        Set<Integer> ids=new HashSet<>();
        for(var r:rows(c,"SELECT id FROM mob_template")) ids.add(n(r,"id"));
        JsonArray out=new JsonArray();
        for(JsonElement e:array(b,"mobs",127)) {
            JsonObject o=e.getAsJsonObject(); int id=field(o,"tempId",-1,0,127);
            if(!ids.contains(id)) throw error(400,"Mẫu quái không tồn tại: "+id);
            out.add(tuple(id,field(o,"level",1,0,127),field(o,"hp",0,0,Integer.MAX_VALUE),field(o,"x",0,0,32767),field(o,"y",0,0,32767)));
        }
        return out;
    }
    private static JsonArray encodeNpcs(Connection c,JsonObject b,JsonArray old) throws Exception {
        Set<Integer> ids=new HashSet<>();
        for(var r:rows(c,"SELECT id FROM npc_template")) ids.add(n(r,"id"));
        JsonArray out=new JsonArray();
        // Preserve extra legacy columns (e.g. avatar) on matching rows; loader consumes first 3.
        for(JsonElement e:array(b,"npcs",127)) {
            JsonObject o=e.getAsJsonObject(); int id=field(o,"npcId",-1,0,127),x=field(o,"x",0,0,32767),y=field(o,"y",0,0,32767);
            if(!ids.contains(id)) throw error(400,"NPC không tồn tại: "+id);
            JsonArray row=tuple(id,x,y);
            for(JsonElement previous:old) {
                JsonArray a=previous.getAsJsonArray();
                if(a.get(0).getAsInt()==id && a.get(1).getAsInt()==x && a.get(2).getAsInt()==y) {
                    for(int j=3;j<a.size();j++) row.add(a.get(j));
                    break;
                }
            }
            out.add(row);
        }
        return out;
    }
    private static JsonArray encodeWaypoints(Connection c,JsonObject b) throws Exception {
        Set<Integer> ids=new HashSet<>(); for(var r:rows(c,"SELECT id FROM map_template")) ids.add(n(r,"id"));
        JsonArray out=new JsonArray();
        for(JsonElement e:array(b,"waypoints",127)) {
            JsonObject o=e.getAsJsonObject(); int dest=field(o,"goMap",-1,0,32767);
            if(!ids.contains(dest)) throw error(400,"Map đích không tồn tại: "+dest);
            int minX=field(o,"minX",0,0,32767),minY=field(o,"minY",0,0,32767),maxX=field(o,"maxX",0,0,32767),maxY=field(o,"maxY",0,0,32767);
            if(minX>maxX || minY>maxY) throw error(400,"Vùng cổng min phải <= max");
            out.add(tuple(name(o,"name",null,32),minX,minY,maxX,maxY,bool(o,"isEnter",false)?1:0,bool(o,"isOffline",false)?1:0,dest,field(o,"goX",0,0,32767),field(o,"goY",0,0,32767)));
        }
        return out;
    }

    static Object reload() throws Exception {
        if(Manager.MAP_TEMPLATES==null) throw error(409,"Map chưa được khởi tạo");
        List<Map<String,Object>> data;
        try(Connection c=AlyraManager.getConnection_Data()) { data=rows(c,SELECT+" ORDER BY id"); }
        Map<Integer,Map<String,Object>> byId=new HashMap<>(); for(var r:data) byId.put(n(r,"id"),r);
        MapTemplate[] current=Manager.MAP_TEMPLATES, staged=new MapTemplate[current.length];
        if(data.size()!=current.length) throw error(409,"Số map đã thay đổi; không thể reload vị trí client, cần restart");
        for(int i=0;i<current.length;i++) {
            MapTemplate old=current[i]; var r=byId.get(old.id);
            if(r==null) throw error(409,"ID map thay đổi; cần restart");
            MapTemplate t=new MapTemplate();
            t.id=old.id;t.name=s(r,"name");t.type=(byte)range(n(r,"type"),0,9,"type");t.planetId=(byte)range(n(r,"planet_id"),0,3,"planet");
            t.bgType=(byte)range(n(r,"bg_type"),0,127,"bgType");t.tileId=(byte)range(n(r,"tile_id"),1,127,"tileId");t.bgId=(byte)range(n(r,"bg_id"),0,127,"bgId");
            t.zones=(byte)range(n(r,"zones"),1,127,"zones"); t.maxPlayerPerZone=(byte)range(n(r,"max_player"),1,127,"maxPlayer");
            if(t.id>=21 && t.id<=23) t.maxPlayerPerZone=(byte)Math.min(t.maxPlayerPerZone,Manager.TRAN_NGUOI_MAP_NHA);
            JsonArray mobs=storedArray(s(r,"mobs"),false),npcs=storedArray(s(r,"npcs"),false);
            t.mobTemp=new byte[mobs.size()];t.mobLevel=new byte[mobs.size()];t.mobHp=new long[mobs.size()];t.mobX=new short[mobs.size()];t.mobY=new short[mobs.size()];
            for(int j=0;j<mobs.size();j++){ JsonArray a=mobs.get(j).getAsJsonArray(); t.mobTemp[j]=Byte.parseByte(a.get(0).getAsString());t.mobLevel[j]=Byte.parseByte(a.get(1).getAsString());t.mobHp[j]=Integer.parseInt(a.get(2).getAsString());t.mobX[j]=Short.parseShort(a.get(3).getAsString());t.mobY[j]=Short.parseShort(a.get(4).getAsString()); }
            t.npcId=new byte[npcs.size()];t.npcX=new short[npcs.size()];t.npcY=new short[npcs.size()];
            for(int j=0;j<npcs.size();j++){JsonArray a=npcs.get(j).getAsJsonArray();t.npcId[j]=Byte.parseByte(a.get(0).getAsString());t.npcX[j]=Short.parseShort(a.get(1).getAsString());t.npcY[j]=Short.parseShort(a.get(2).getAsString());}
            for(JsonElement el:storedArray(s(r,"waypoints"),true)){JsonArray a=el.getAsJsonArray();WayPoint w=new WayPoint();w.name=a.get(0).getAsString();w.minX=Short.parseShort(a.get(1).getAsString());w.minY=Short.parseShort(a.get(2).getAsString());w.maxX=Short.parseShort(a.get(3).getAsString());w.maxY=Short.parseShort(a.get(4).getAsString());w.isEnter=a.get(5).getAsInt()==1;w.isOffline=a.get(6).getAsInt()==1;w.goMap=Short.parseShort(a.get(7).getAsString());w.goX=Short.parseShort(a.get(8).getAsString());w.goY=Short.parseShort(a.get(9).getAsString());t.wayPoints.add(w);}
            JsonArray effects=new JsonParser().parse(s(r,"effect")).getAsJsonArray();
            for(JsonElement el:effects){JsonObject o=el.getAsJsonObject();EffectMap effect=new EffectMap();effect.setKey(o.get("key").getAsString());effect.setValue(o.get("value").getAsString());t.effectMaps.add(effect);}
            staged[i]=t;
        }
        // Atomic array publication; do not clear shared wayPoints or mutate zones/dungeon registries.
        Manager.MAP_TEMPLATES=staged;
        return changed("Đã nạp template map; map/khu đang chạy giữ nguyên, cần restart để áp dụng cấu trúc/spawn. Client cần đồng bộ lại dữ liệu map.",true,"reloaded",staged.length,"liveApplied",false,"clientResyncRequired",true);
    }
}
