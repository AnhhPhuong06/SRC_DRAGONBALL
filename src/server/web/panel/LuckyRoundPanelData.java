package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.*;
import server.Manager;
import services.ItemService;
import services.LuckyRoundSettings;
import services.LuckyRoundSettings.*;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.CatalogShopApi.*;

final class LuckyRoundPanelData {
    @FunctionalInterface interface CatalogLoader { CatalogData load(); }
    private static final LuckyRoundSettings STORE=new LuckyRoundSettings();
    private LuckyRoundPanelData() {}

    static Object handle(HttpExchange e)throws Exception {return handle(e,STORE,LuckyRoundPanelData::catalog);}
    static Object handle(HttpExchange e,LuckyRoundSettings store,CatalogLoader loader)throws Exception {
        requireMethod(e,"GET","POST");fields(query(e).keySet());
        try {
            CatalogData catalog=loader.load();
            if(e.getRequestMethod().equals("GET"))return view(store,catalog);
            JsonObject b=body(e);fields(b,"revision","values","groups","milestones");rejectNull(b);
            String expected=required(b,"revision");
            if(!b.has("values")||!b.get("values").isJsonObject()||!b.has("groups")||!b.get("groups").isJsonObject())throw error(400,"Thiếu thông số hoặc nhóm quà.");
            var values=new LinkedHashMap<String,Integer>();var groups=new LinkedHashMap<String,List<Integer>>();
            var v=b.getAsJsonObject("values");fields(v,LuckyRoundSettings.FIELDS.stream().map(Field::key).toArray(String[]::new));
            for(var f:LuckyRoundSettings.FIELDS){if(!v.has(f.key()))throw error(400,"Thiếu trường: "+f.key());values.put(f.key(),integer(v,f.key(),-1));}
            var g=b.getAsJsonObject("groups");fields(g,LuckyRoundSettings.GROUPS.stream().map(Group::key).toArray(String[]::new));
            for(var field:LuckyRoundSettings.GROUPS) {
                var ids=new ArrayList<Integer>();for(var item:array(g,field.key(),200)){var number=new JsonObject();number.add("id",item);ids.add(integer(number,"id",-1));}groups.put(field.key(),ids);
            }
            var milestones=new ArrayList<Milestone>();
            for(var element:array(b,"milestones",4)) {
                var m=object(element);fields(m,"id","spins","rewards");var rewards=new ArrayList<Reward>();
                for(var item:array(m,"rewards",100)) {
                    var r=object(item);fields(r,"itemId","quantity","options");var options=new ArrayList<Option>();
                    for(var option:array(r,"options",255)){var o=object(option);fields(o,"id","param");options.add(new Option(integer(o,"id",-1),integer(o,"param",-1)));}
                    rewards.add(new Reward(integer(r,"itemId",-1),integer(r,"quantity",-1),options));
                }
                milestones.add(new Milestone(integer(m,"id",-1),integer(m,"spins",-1),rewards));
            }
            var next=new State(values,groups,milestones);LuckyRoundSettings.validate(next);
            var current=store.state();
            if(!LuckyRoundSettings.revision(current).equals(expected))throw new ConcurrentModificationException("Cấu hình đã thay đổi. Tải lại trước khi lưu.");
            validateReferences(next,current,catalog);
            store.save(next,expected);var result=ok("Đã lưu và áp dụng Vòng quay Thượng Đế.");
            result.put("revision",LuckyRoundSettings.revision(store.state()));result.put("liveApplied",true);return result;
        }catch(ConcurrentModificationException ex){throw error(409,ex.getMessage());}
        catch(IllegalArgumentException ex){throw error(400,ex.getMessage());}
        catch(IOException ex){throw error(503,"Không lưu được cấu hình vòng quay; cấu hình đang chạy được giữ nguyên.");}
    }
    static Object view(LuckyRoundSettings store,CatalogData catalog) {
        var state=store.state();return map("state",state,"revision",LuckyRoundSettings.revision(state),"fields",LuckyRoundSettings.FIELDS,"warnings",warnings(state,catalog),
                "groupFields",LuckyRoundSettings.GROUPS,"catalog",map("items",catalog.items.values(),"options",catalog.options.values()),
                "notes","Áp dụng cho Quay bằng vé quay và Quay nhanh tại Thượng Đế. Nhóm siêu hiếm được xét trước; các nhóm còn lại được xét khi trượt siêu hiếm. "
                +"Quà thường giữ cơ chế gốc: hồng ngọc, vàng, bổ trợ và bảng trang bị/nguyên liệu của server. "
                +"Lưu không thay đổi số lượt đã quay hoặc mốc đã nhận. Bốn mốc giữ nguyên vị trí nhận thưởng.");
    }
    /** Retain legacy references only in their original location; never introduce or edit an invalid reward. */
    static void validateReferences(State next,State current,CatalogData catalog) {
        int price=next.values().get("idVatPhamQuay");
        if(!Objects.equals(price,current.values().get("idVatPhamQuay"))||availableItem(catalog,price))catalog.runtimeItem(price);
        for(var group:next.groups().entrySet())for(int id:group.getValue())
            if(!current.groups().get(group.getKey()).contains(id)||availableItem(catalog,id))catalog.runtimeItem(id);
        for(var milestone:next.milestones()) {
            var retained=new ArrayList<>(current.milestones().get(milestone.id()).rewards());
            for(var reward:milestone.rewards()) {
                boolean unchanged=retained.remove(reward);
                boolean available=availableItem(catalog,reward.itemId())&&reward.options().stream().allMatch(o->availableOption(catalog,o.id()));
                if(unchanged&&!available)continue;
                catalog.runtimeItem(reward.itemId());
                for(var option:reward.options())if(!availableOption(catalog,option.id()))throw error(422,"Option chưa tồn tại: "+option.id());
            }
        }
    }
    private static boolean availableItem(CatalogData catalog,int id) {
        var item=ItemService.gI().getTemplate(id);
        return catalog.items.containsKey(id)&&item!=null&&item.id==id;
    }
    private static boolean availableOption(CatalogData catalog,int id) {
        var option=ItemService.gI().getItemOptionTemplate(id);
        return catalog.options.containsKey(id)&&option!=null&&option.id==id;
    }
    static List<String> warnings(State state,CatalogData catalog) {
        var messages=new ArrayList<String>();int price=state.values().get("idVatPhamQuay");
        if(!availableItem(catalog,price))messages.add("Vật phẩm dùng để quay #"+price+" chưa có trong danh mục đang chạy.");
        for(var group:LuckyRoundSettings.GROUPS)for(int id:state.groups().get(group.key()))if(!availableItem(catalog,id))messages.add(group.label()+": vật phẩm #"+id+" chưa có trong danh mục đang chạy.");
        for(var m:state.milestones())for(var r:m.rewards()) {
            if(!availableItem(catalog,r.itemId()))messages.add("Mốc "+(m.id()+1)+": vật phẩm #"+r.itemId()+" chưa có trong danh mục đang chạy.");
            for(var o:r.options())if(!availableOption(catalog,o.id()))messages.add("Mốc "+(m.id()+1)+", vật phẩm #"+r.itemId()+": option #"+o.id()+" chưa có trong danh mục đang chạy.");
        }
        return messages;
    }
    private static CatalogData catalog() {
        var data=new CatalogData();
        for(var item:new ArrayList<>(Manager.ITEM_TEMPLATES))if(item!=null&&item.id>=0&&item.name!=null&&!item.name.isBlank())data.items.put((int)item.id,map("id",(int)item.id,"name",item.name,"icon",(int)item.iconID));
        for(var option:new ArrayList<>(Manager.ITEM_OPTION_TEMPLATES))if(option!=null&&option.id>=0&&option.id<=255)data.options.put(option.id,map("id",option.id,"name",ItemService.resolveOptionName(option.id,option.name)));
        if(data.items.isEmpty()||data.options.isEmpty())throw error(503,"Máy chủ chưa nạp xong vật phẩm. Chờ game khởi động rồi tải lại.");
        return data;
    }
}
