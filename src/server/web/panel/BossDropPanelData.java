package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import database.AlyraManager;
import java.io.IOException;
import java.util.*;
import services.BossDropRules;
import services.BossSettings;
import services.ItemService;
import services.MobDropRules;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.WorldData.*;

final class BossDropPanelData {
    record Catalog(List<Map<String,Object>> maps,List<Map<String,Object>> bosses,
                   List<Map<String,Object>> items,List<Map<String,Object>> options) {}
    @FunctionalInterface interface Loader { Catalog load() throws Exception; }
    static Object handle(HttpExchange exchange) throws Exception { return handle(exchange,BossDropRules.get(),BossDropPanelData::catalog); }
    static Object handle(HttpExchange exchange,BossDropRules store,Loader loader) throws Exception {
        requireMethod(exchange,"GET","POST");
        try {
            if("GET".equals(exchange.getRequestMethod())) {
                var catalog=loader.load();var state=store.state();List<Map<String,Object>> rules=new ArrayList<>();
                for(var r:state.rules()) {
                    var item=catalog.items().stream().filter(i->n(i,"id")==r.itemId()).findFirst().orElse(Map.of());
                    var boss=catalog.bosses().stream().filter(b->s(b,"key").equals(r.bossKey())).findFirst().orElse(Map.of());
                    rules.add(dto("id",r.id(),"name",r.name(),"enabled",r.enabled(),"mapId",r.mapId(),
                            "mapName",r.mapId()==-1?"Tất cả map":catalog.maps().stream().filter(m->n(m,"id")==r.mapId()).findFirst().map(m->s(m,"name")).orElse("#"+r.mapId()),
                            "bossKey",r.bossKey(),"bossName",boss.isEmpty()?r.bossKey():s(boss,"name"),
                            "itemId",r.itemId(),"itemName",item.isEmpty()?"#"+r.itemId():s(item,"name"),"icon",item.get("icon"),
                            "chance",r.chance(),"minQuantity",r.minQuantity(),"maxQuantity",r.maxQuantity(),"options",r.options()));
                }
                return dto("rules",rules,"revision",state.revision(),"maxRules",BossDropRules.MAX_RULES,"loadError",store.loadError(),
                        "catalog",dto("maps",catalog.maps(),"bosses",catalog.bosses(),"items",catalog.items(),"options",catalog.options()),
                        "notes","Drop này rơi thêm cùng phần thưởng gốc. Chọn đúng mẫu/giai đoạn boss; mỗi dòng có tỷ lệ riêng, có thể rơi đồng thời. "
                                +"Lưu áp dụng từ lần diệt boss tiếp theo và giữ qua khởi động lại. Boss luyện tập không có thưởng khi chết vẫn theo cơ chế riêng.");
            }
            var body=body(exchange);String action=query(exchange).getOrDefault("action",text(body,"action",""));
            if(!body.has("revision"))throw error(400,"Thiếu phiên bản drop boss");
            long revision=longValue(body,"revision",-1);
            if("rule-delete".equals(action)) { store.delete(field(body,"id",-1,1,Integer.MAX_VALUE),revision);return changed("Đã xóa drop boss.",false); }
            if(!"rule-save".equals(action))throw error(400,"Tác vụ drop boss không hợp lệ");
            var catalog=loader.load();String boss=required(body,"bossKey");
            if(catalog.bosses().stream().noneMatch(b->s(b,"key").equals(boss)))throw error(422,"Mẫu boss không tồn tại");
            int map=field(body,"mapId",-1,-1,32767),item=field(body,"itemId",-1,0,32767);
            if(map!=-1)requireId(catalog.maps(),map,"Map");requireId(catalog.items(),item,"Vật phẩm");
            if(!body.has("chance") || !body.get("chance").isJsonPrimitive() || !body.getAsJsonPrimitive("chance").isNumber())throw error(400,"Tỷ lệ phải từ 0 đến 100%");
            List<MobDropRules.Option> options=new ArrayList<>();
            if(body.has("options"))for(var value:array(body,"options",32)) {
                var o=value.getAsJsonObject();int id=field(o,"id",-1,0,255);requireId(catalog.options(),id,"Option");
                options.add(new MobDropRules.Option(id,field(o,"param",-1,0,65535)));
            }
            var rule=new BossDropRules.Rule(field(body,"id",0,0,Integer.MAX_VALUE),name(body,"name",null,100),bool(body,"enabled",true),
                    map,boss,item,body.get("chance").getAsDouble(),field(body,"minQuantity",1,1,99999),field(body,"maxQuantity",1,1,99999),options);
            var saved=store.save(rule,revision);
            return changed("Đã lưu drop boss. Áp dụng từ lần diệt tiếp theo.",false,"id",saved.id());
        } catch(ConcurrentModificationException e) { throw error(409,e.getMessage()); }
        catch(NoSuchElementException e) { throw error(404,e.getMessage()); }
        catch(IllegalArgumentException e) { throw error(400,e.getMessage()); }
        catch(IOException e) { throw error(503,"Không đọc/ghi được file drop boss; cấu hình đang áp dụng được giữ nguyên."); }
    }
    private static void requireId(List<Map<String,Object>> rows,int id,String label) {
        if(rows.stream().noneMatch(r->n(r,"id")==id))throw error(422,label+" không tồn tại: "+id);
    }
    private static Catalog catalog() throws Exception {
        try(var connection=AlyraManager.getConnection_Data()) {
            var maps=rows(connection,"SELECT id,name FROM map_template ORDER BY id");
            var items=rows(connection,"SELECT id,name,icon_id AS icon FROM item_template WHERE id BETWEEN 0 AND 32767 ORDER BY id");
            var options=rows(connection,"SELECT id,name FROM item_option_template WHERE id BETWEEN 0 AND 255 ORDER BY id");
            for(var option:options)option.put("name",ItemService.resolveOptionName(n(option,"id"),s(option,"name")));
            List<Map<String,Object>> bosses=new ArrayList<>();
            for(var entry:BossSettings.get().state().templates())bosses.add(dto("key",entry.key(),"name",entry.config().name()));
            return new Catalog(maps,bosses,items,options);
        }
    }
}
