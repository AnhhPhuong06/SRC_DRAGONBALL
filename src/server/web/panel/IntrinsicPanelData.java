package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import database.AlyraManager;
import java.sql.*;
import java.util.*;
import models.intrinsic.Intrinsic;
import models.player.Player;
import server.Client;
import server.Manager;
import services.Service;
import services.player.IntrinsicService;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.WorldData.*;

/** data_intrinsic is [id,param1,countOpen,param2], not a foreign-key column. */
final class IntrinsicPanelData {
    static final String SELECT="SELECT id,name,param_from_1,param_to_1,param_from_2,param_to_2,icon,gender FROM intrinsic";
    private IntrinsicPanelData() {}

    static Object handle(HttpExchange e) throws Exception {
        var q=query(e);
        if("GET".equals(e.getRequestMethod())) {
            List<Map<String,Object>> all;
            try(Connection c=AlyraManager.getConnection_Data()){all=rows(c,SELECT+" ORDER BY id");}
            Map<Integer,List<Map<String,Object>>> players=carriers();
            if(q.containsKey("id")) {
                int id=number(q.get("id"),-1);
                for(var r:all) if(n(r,"id")==id) return dto("intrinsic",view(r,players),"catalog",dto("genders",GENDERS));
                throw error(404,"Không tìm thấy nội tại");
            }
            List<Map<String,Object>> found=new ArrayList<>();
            for(var r:all) if(matches(q,n(r,"id"),s(r,"name")) && filter(q,"gender",effectiveGender(r))) found.add(view(r,players));
            int total=found.size(),limit=limit(q),next=all.stream().mapToInt(r->n(r,"id")).max().orElse(-1)+1;
            return dto("intrinsics",found.subList(0,Math.min(limit,total)),"total",total,"limit",limit,"nextId",next,"genders",GENDERS,
                    "warning","Hiệu ứng chiến đấu dựa vào ID viết cứng trong NPoint/SkillService; thêm mô tả không tạo hiệu ứng mới.");
        }
        JsonObject b=body(e);String action=q.getOrDefault("action",text(b,"action",""));
        if("reload".equals(action)) return reload();
        if("intrinsic-assign".equals(action)) return assign(b);
        if("intrinsic-delete".equals(action)) {
            int id=id(b);
            if(id<=27) throw error(409,"Không xóa nội tại nền 0-27: sentinel và ID chiến đấu được gameplay tham chiếu trực tiếp");
            if(carriers().containsKey(id)) throw error(409,"Nội tại đang có người sử dụng (online hoặc DB)");
            transaction(true,c->{one(c,SELECT+" WHERE id=? FOR UPDATE",id); update(c,"DELETE FROM intrinsic WHERE id=?",id); return null;});
            return changed("Đã xóa bản ghi; danh sách quay nội tại đang chạy giữ nguyên cho đến restart.",true,"id",id,"liveApplied",false);
        }
        boolean create="intrinsic-create".equals(action);
        if(!create && !"intrinsic-update".equals(action)) throw error(400,"Tác vụ intrinsic không hợp lệ");
        int id=transaction(true,c->{
            List<Map<String,Object>> all=rows(c,SELECT+" ORDER BY id FOR UPDATE");
            int target=create?all.stream().mapToInt(r->n(r,"id")).max().orElse(-1)+1:id(b);
            range(target,0,127,"id nội tại (loader byte)");
            Map<String,Object> old=null;for(var r:all) if(n(r,"id")==target) old=r;
            if(!create && old==null) throw error(404,"Không tìm thấy nội tại");
            String name=name(b,"name",create?null:s(old,"name"),255);
            int from1=field(b,"paramFrom1",create?0:n(old,"param_from_1"),0,32767),to1=field(b,"paramTo1",create?0:n(old,"param_to_1"),0,32767);
            int from2=field(b,"paramFrom2",create?0:n(old,"param_from_2"),0,32767),to2=field(b,"paramTo2",create?0:n(old,"param_to_2"),0,32767);
            if(from1>to1 || from2>to2) throw error(400,"Khoảng tham số phải từ nhỏ đến lớn");
            int icon=field(b,"icon",create?0:n(old,"icon"),0,32767),gender=field(b,"gender",create?3:n(old,"gender"),0,3);
            if(name.contains("Kaioken")) gender=0; // exact Manager.loadDatabase override
            if(target==0 && (gender!=3 || from1!=0 || to1!=0 || from2!=0 || to2!=0)) throw error(409,"Nội tại #0 phải là sentinel chung, tham số 0");
            if(!create && target>0 && target<=27 && gender!=effectiveGender(old)) throw error(409,"Không chuyển hệ ID nội tại nền vì kỹ năng tác động theo ID");
            if(create) update(c,"INSERT INTO intrinsic (id,name,param_from_1,param_to_1,param_from_2,param_to_2,icon,gender) VALUES (?,?,?,?,?,?,?,?)",target,name,from1,to1,from2,to2,icon,gender);
            else update(c,"UPDATE intrinsic SET name=?,param_from_1=?,param_to_1=?,param_from_2=?,param_to_2=?,icon=?,gender=? WHERE id=?",name,from1,to1,from2,to2,icon,gender,target);
            return target;
        });
        return changed("Đã lưu DB. Reload cập nhật nội dung cùng hệ; thêm/xóa/chuyển hệ cần restart để giữ danh sách quay và sentinel an toàn.",true,"id",id,"liveApplied",false,
                "warning",create?"ID mới chỉ thêm template; không có hiệu ứng chiến đấu nếu chưa lập trình gameplay tương ứng.":"Tham số đã quay của người chơi được giữ nguyên.");
    }
    static int effectiveGender(Map<String,Object> r){return s(r,"name").contains("Kaioken")?0:n(r,"gender");}
    static Intrinsic template(Map<String,Object> r){
        Intrinsic t=new Intrinsic();t.id=range(n(r,"id"),0,127,"intrinsic id");t.name=s(r,"name");t.gender=(byte)range(effectiveGender(r),0,3,"gender");
        t.paramFrom1=(short)range(n(r,"param_from_1"),0,32767,"paramFrom1");t.paramTo1=(short)range(n(r,"param_to_1"),t.paramFrom1,32767,"paramTo1");
        t.paramFrom2=(short)range(n(r,"param_from_2"),0,32767,"paramFrom2");t.paramTo2=(short)range(n(r,"param_to_2"),t.paramFrom2,32767,"paramTo2");t.icon=(short)range(n(r,"icon"),0,32767,"icon");return t;
    }
    static Map<String,Object> view(Map<String,Object> r,Map<Integer,List<Map<String,Object>>> carriers){
        Intrinsic t=template(r);var players=carriers.getOrDefault(t.id,List.of());
        return dto("id",t.id,"name",t.name,"description",t.getDescription(),"paramFrom1",t.paramFrom1,"paramTo1",t.paramTo1,"paramFrom2",t.paramFrom2,"paramTo2",t.paramTo2,
                "icon",t.icon,"gender",t.gender,"genderName",gender(t.gender),"playerCount",players.size(),"players",players,"combatImplemented",t.id<=27);
    }
    static Map<Integer,List<Map<String,Object>>> carriers() throws Exception {
        Map<Long,Map<String,Object>> players=new LinkedHashMap<>();
        try(Connection c=AlyraManager.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,name,data_intrinsic FROM player");ResultSet r=p.executeQuery()){
            while(r.next()) {
                JsonArray a=playerData(r.getString("data_intrinsic"));
                if(a!=null) players.put(r.getLong("id"),dto("id",r.getLong("id"),"name",r.getString("name"),"intrinsicId",a.get(0).getAsInt(),"param1",a.get(1).getAsInt(),"param2",a.get(3).getAsInt()));
            }
        }
        // Online state is authoritative; DB may be one autosave behind.
        for(Player p:new ArrayList<>(Client.gI().getPlayers())) if(p!=null && !p.isBot && p.playerIntrinsic!=null && p.playerIntrinsic.intrinsic!=null){
            Intrinsic t=p.playerIntrinsic.intrinsic;players.put(p.id,dto("id",p.id,"name",p.name,"intrinsicId",t.id,"param1",t.param1,"param2",t.param2));
        }
        Map<Integer,List<Map<String,Object>>> out=new HashMap<>();
        for(var p:players.values()) out.computeIfAbsent(n(p,"intrinsicId"),k->new ArrayList<>()).add(p);
        return out;
    }
    static JsonArray playerData(String raw){
        if(raw==null || raw.isBlank()) return null;
        try {JsonArray a=new JsonParser().parse(raw).getAsJsonArray();if(a.size()<4) return null;for(int i=0;i<4;i++) a.get(i).getAsInt();return a;}
        catch(RuntimeException bad){throw error(409,"Có data_intrinsic lỗi trong DB; sửa dữ liệu trước khi chỉnh/xóa nội tại");}
    }
    static Object assign(JsonObject b) throws Exception {
        long playerId=longValue(b,"playerId",-1);if(playerId<1) throw error(400,"playerId không hợp lệ");
        int id=field(b,"intrinsicId",-1,0,127);Intrinsic t;
        try(Connection c=AlyraManager.getConnection_Data()){t=template(one(c,SELECT+" WHERE id=?",id));}
        if(IntrinsicService.gI().getIntrinsicById(id)==null) throw error(409,"Nội tại chưa có trong runtime; cần restart/reload trước khi gán");
        Player online=Client.gI().getPlayer(playerId);
        if(online!=null && (online.isBot || online.playerIntrinsic==null)) throw error(409,"Không gán nội tại cho bot/nhân vật chưa tải xong");
        int p1=field(b,"param1",t.paramFrom1,t.paramFrom1,t.paramTo1),p2=field(b,"param2",t.paramFrom2,t.paramFrom2,t.paramTo2);
        t.param1=(short)p1;t.param2=(short)p2;
        Object lock=online!=null?online:EDIT_LOCK;
        synchronized(lock){
            Intrinsic previous=online==null?null:online.playerIntrinsic.intrinsic;
            try {
                transaction(false,c->{
                    var row=one(c,"SELECT id,gender,data_intrinsic FROM player WHERE id=? FOR UPDATE",playerId);
                    int gender=n(row,"gender"); if(t.id!=0 && t.gender<3 && t.gender!=gender) throw error(400,"Nội tại không phù hợp hệ nhân vật");
                    JsonArray old=playerData(s(row,"data_intrinsic"));
                    int count=online!=null?online.playerIntrinsic.countOpen:old==null?0:old.get(2).getAsInt();
                    if(online==null && Client.gI().getPlayer(playerId)!=null) throw error(409,"Nhân vật vừa đăng nhập, hãy thử lại");
                    if(online!=null) online.playerIntrinsic.intrinsic=t;
                    update(c,"UPDATE player SET data_intrinsic=? WHERE id=?",tuple(t.id,p1,count,p2).toString(),playerId);
                    return null;
                });
            }catch(Exception failure){if(online!=null) online.playerIntrinsic.intrinsic=previous;throw failure;}
        }
        if(online!=null){online.nPoint.calPoint();IntrinsicService.gI().sendInfoIntrinsic(online);Service.gI().point(online);}
        return changed("Đã gán nội tại và lưu data_intrinsic; giữ nguyên số lần mở.",false,"playerId",playerId,"intrinsicId",id,"param1",p1,"param2",p2,"liveApplied",online!=null);
    }
    static Object reload() throws Exception {
        Map<Integer,Intrinsic> staged=new HashMap<>();
        try(Connection c=AlyraManager.getConnection_Data()){for(var r:rows(c,SELECT+" ORDER BY id")){Intrinsic t=template(r);staged.put(t.id,t);}}
        Intrinsic zero=staged.get(0);if(zero==null || zero.gender!=3 || zero.paramFrom1!=0 || zero.paramTo1!=0 || zero.paramFrom2!=0 || zero.paramTo2!=0) throw error(409,"Thiếu/sai nội tại sentinel #0");
        int applied=0,pending=0;
        Set<Integer> runtime=new HashSet<>();
        synchronized(Manager.INTRINSICS){
            for(Intrinsic old:Manager.INTRINSICS){
                runtime.add(old.id);Intrinsic t=staged.get(old.id);
                if(t==null || t.gender!=old.gender){pending++;continue;}
                // Keep object identities referenced by the three gender lists. No clear/add race.
                old.name=t.name;old.icon=t.icon;old.paramFrom1=t.paramFrom1;old.paramTo1=t.paramTo1;old.paramFrom2=t.paramFrom2;old.paramTo2=t.paramTo2;applied++;
            }
        }
        for(int id:staged.keySet()) if(!runtime.contains(id)) pending++;
        int refreshed=0;
        for(Player p:new ArrayList<>(Client.gI().getPlayers())) if(p!=null && !p.isBot && p.playerIntrinsic!=null && p.playerIntrinsic.intrinsic!=null){
            Intrinsic old=p.playerIntrinsic.intrinsic,t=staged.get(old.id);
            if(t!=null && t.gender==old.gender){Intrinsic replacement=new Intrinsic(t);replacement.param1=old.param1;replacement.param2=old.param2;p.playerIntrinsic.intrinsic=replacement;IntrinsicService.gI().sendInfoIntrinsic(p);refreshed++;}
        }
        return changed("Đã nạp nội dung nội tại cùng hệ; giữ nguyên tham số đã quay. Thay đổi thành viên/hệ của danh sách cần restart.",pending>0,"reloaded",applied,"playersRefreshed",refreshed,"pendingStructuralChanges",pending,"liveApplied",true);
    }
}
