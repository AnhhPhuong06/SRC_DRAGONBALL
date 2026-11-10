package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import database.AlyraManager;
import java.sql.*;
import java.util.*;
import models.Template.MobTemplate;
import server.Manager;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.WorldData.*;

/** Mob IDs are client list offsets; only append is supported, never delete/reindex. */
final class MobPanelData {
    static final List<Map<String,Object>> TYPES=choices("Đứng yên", "Đi bộ", "Loại 2", "Loại 3", "Bay");
    static final String SELECT="SELECT id,type,name,hp,range_move,speed,dart_type,percent_dame,percent_tiem_nang FROM mob_template";
    private MobPanelData() {}

    static Object handle(HttpExchange e) throws Exception {
        var q=query(e);
        if("GET".equals(e.getRequestMethod())) {
            try(Connection c=AlyraManager.getConnection_Data()) {
                List<Map<String,Object>> all=rows(c,SELECT+" ORDER BY id");
                Map<Integer,List<Map<String,Object>>> uses=usage(c);
                int next=all.stream().mapToInt(r->n(r,"id")).max().orElse(-1)+1;
                if(q.containsKey("id")) {
                    int id=number(q.get("id"),-1);
                    for(var r:all) if(n(r,"id")==id) return dto("mob",view(r,uses),"catalog",dto("types",TYPES,"percentGoldSupported",false));
                    throw error(404,"Không tìm thấy mẫu quái");
                }
                List<Map<String,Object>> found=new ArrayList<>();
                for(var r:all) if(matches(q,n(r,"id"),s(r,"name")) && filter(q,"type",n(r,"type"))) found.add(view(r,uses));
                int limit=limit(q), total=found.size();
                return dto("mobs",found.subList(0,Math.min(total,limit)),"total",total,"limit",limit,"nextId",next,"types",TYPES,"percentGoldSupported",false);
            }
        }
        JsonObject b=body(e);String action=q.getOrDefault("action",text(b,"action",""));
        if("reload".equals(action)) return reload();
        if("mob-delete".equals(action)) throw error(409,"Không xóa mẫu quái: ID là offset client và được gameplay tham chiếu trực tiếp");
        boolean create="mob-create".equals(action);
        if(!create && !"mob-update".equals(action)) throw error(400,"Tác vụ mob không hợp lệ");
        if(b.has("percentGold") && integer(b,"percentGold",100)!=100) throw error(422,"mob_template không có percentGold; tỉ lệ vàng do gameplay quyết định");
        int id=transaction(true,c->{
            List<Map<String,Object>> locked=rows(c,SELECT+" ORDER BY id FOR UPDATE");
            int target=create?locked.stream().mapToInt(r->n(r,"id")).max().orElse(-1)+1:id(b);
            range(target,0,254,"id (số mẫu client tối đa 255)");
            Map<String,Object> old=null;
            for(var r:locked) if(n(r,"id")==target) old=r;
            if(!create && old==null) throw error(404,"Không tìm thấy mẫu quái");
            String name=name(b,"name",create?null:s(old,"name"),50);
            int type=field(b,"type",create?1:n(old,"type"),0,4);
            int hp=field(b,"hp",create?100:n(old,"hp"),1,Integer.MAX_VALUE);
            int move=field(b,"rangeMove",create?33:n(old,"range_move"),0,127);
            int speed=field(b,"speed",create?1:n(old,"speed"),0,127);
            int dart=field(b,"dartType",create?25:n(old,"dart_type"),0,127);
            int dame=field(b,"percentDame",create?5:n(old,"percent_dame"),0,127);
            int tn=field(b,"percentTiemNang",create?50:n(old,"percent_tiem_nang"),0,127);
            if(create) update(c,"INSERT INTO mob_template (id,type,name,hp,range_move,speed,dart_type,percent_dame,percent_tiem_nang) VALUES (?,?,?,?,?,?,?,?,?)",target,type,name,hp,move,speed,dart,dame,tn);
            else update(c,"UPDATE mob_template SET type=?,name=?,hp=?,range_move=?,speed=?,dart_type=?,percent_dame=?,percent_tiem_nang=? WHERE id=?",type,name,hp,move,speed,dart,dame,tn,target);
            return target;
        });
        return changed("Đã lưu mẫu quái vào DB. Reload cập nhật template có sẵn; quái đang sống và mẫu mới cần restart để đồng bộ an toàn.",true,"id",id,"liveApplied",false,
                "warning",create?"Mẫu mới cần tài nguyên hình ảnh client phù hợp; ID >127 không thể đặt bằng loader map signed-byte hiện tại.":"percentGold không có trong schema, không được lưu.");
    }
    static Map<Integer,List<Map<String,Object>>> usage(Connection c) throws Exception {
        Map<Integer,List<Map<String,Object>>> result=new HashMap<>();
        for(var r:rows(c,"SELECT id,name,mobs FROM map_template ORDER BY id")) {
            Set<Integer> seen=new HashSet<>();
            for(JsonElement e:storedArray(s(r,"mobs"),false)) seen.add(e.getAsJsonArray().get(0).getAsInt());
            for(int id:seen) result.computeIfAbsent(id,k->new ArrayList<>()).add(dto("id",n(r,"id"),"name",s(r,"name")));
        }
        return result;
    }
    static Map<String,Object> view(Map<String,Object> r,Map<Integer,List<Map<String,Object>>> usage) {
        int type=n(r,"type"),id=n(r,"id");List<Map<String,Object>> maps=usage.getOrDefault(id,List.of());
        return dto("id",id,"name",s(r,"name"),"type",type,"typeName",type>=0 && type<TYPES.size()?TYPES.get(type).get("name"):"Loại "+type,
                "hp",l(r,"hp"),"rangeMove",n(r,"range_move"),"speed",n(r,"speed"),"dartType",n(r,"dart_type"),"percentDame",n(r,"percent_dame"),
                "percentTiemNang",n(r,"percent_tiem_nang"),"percentGold",100,"percentGoldSupported",false,"mapCount",maps.size(),"maps",maps);
    }
    static Object reload() throws Exception {
        List<Map<String,Object>> rows;
        try(Connection c=AlyraManager.getConnection_Data()){rows=rows(c,SELECT+" ORDER BY id");}
        Map<Integer,MobTemplate> staged=new HashMap<>();
        for(var r:rows){
            MobTemplate t=new MobTemplate();t.id=range(n(r,"id"),0,254,"mob id");t.name=s(r,"name");t.type=(byte)range(n(r,"type"),0,4,"type");t.hp=l(r,"hp");
            if(t.hp<=0 || t.hp>Integer.MAX_VALUE) throw error(409,"HP mẫu quái vượt schema int");
            t.rangeMove=(byte)range(n(r,"range_move"),0,127,"rangeMove");t.speed=(byte)range(n(r,"speed"),0,127,"speed");t.dartType=(byte)range(n(r,"dart_type"),0,127,"dartType");
            t.percentDame=(byte)range(n(r,"percent_dame"),0,127,"percentDame");t.percentTiemNang=(byte)range(n(r,"percent_tiem_nang"),0,127,"percentTiemNang");staged.put(t.id,t);
        }
        int applied=0,pending=0;
        synchronized(Manager.MOB_TEMPLATES){
            for(var entry:staged.entrySet()) {
                int id=entry.getKey();
                if(id>=Manager.MOB_TEMPLATES.size()){pending++;continue;}
                if(Manager.MOB_TEMPLATES.get(id).id!=id) throw error(409,"Offset mob runtime không khớp ID, cần restart");
            }
            // No list-size changes: online clients and index-based references keep their offsets.
            for(var entry:staged.entrySet()) if(entry.getKey()<Manager.MOB_TEMPLATES.size()){Manager.MOB_TEMPLATES.set(entry.getKey(),entry.getValue());applied++;}
        }
        return changed("Đã reload template hiện có. Không thay máu/chỉ số quái đang sống; mẫu mới chờ restart, client cần đồng bộ tài nguyên.",true,
                "reloaded",applied,"pendingNewTemplates",pending,"clientResyncRequired",true,"existingLiveMobsChanged",false);
    }
}
