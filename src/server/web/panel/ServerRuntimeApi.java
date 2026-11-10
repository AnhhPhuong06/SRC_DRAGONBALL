package server.web.panel;

import Bot.Bot;
import Bot.BotManager;
import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import consts.AppearType;
import consts.BossStatus;
import database.AlyraManager;
import database.daos.ShopDAO;
import event.EventManager;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import managers.GiftCodeManager;
import managers.ConsignShopManager;
import managers.boss.*;
import models.GiftCode;
import models.Template.NpcTemplate;
import models.boss.Boss;
import models.item.Item;
import models.player.Player;
import network.session.SessionManager;
import server.*;
import services.MobMeCleanerService;
import services.Service;
import services.top.*;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.WorldData.*;

/** Operations use headless managers/services; no Swing controls or process launcher. */
final class ServerRuntimeApi {
    private static final String[] EVENT_NAMES={"Không chọn","Halloween","Quốc tế Phụ nữ","Giáng Sinh","Tết Nguyên Đán","Trung Thu","Giỗ Tổ","Top Up"};
    private ServerRuntimeApi() {}

    static Map<String,Object> status(){
        long uptime=Math.max(0,System.currentTimeMillis()-ServerManager.timeStartMillis);
        return dto("port",ServerManager.PORT,"playersOnline",Client.gI().getRealPlayerCount(),"players",Client.gI().getRealPlayerCount(),
                "name",ServerManager.NAME,"online",ServerManager.isRunning,"uptime",uptime/1000,"uptimeText",uptime(uptime));
    }
    static Object handle(HttpExchange e) throws Exception {
        var q=query(e);
        if("login-access".equals(q.get("action")))return LoginAccessPanelData.handle(e);
        if("GET".equals(e.getRequestMethod())) {
            if(q.containsKey("action") && !"bots".equals(q.get("action"))) throw error(400,"Tác vụ GET server không hợp lệ");
            return "bots".equals(q.get("action"))?bots(q):snapshot();
        }
        JsonObject b=body(e);String action=q.getOrDefault("action",text(b,"action",""));
        return switch(action){
            case "exp-rate" -> {int rate=field(b,"rate",-1,1,32766);Manager.RATE_EXP_SERVER=rate;Service.gI().sendThongBaoAllPlayer("Hệ số EXP máy chủ: x"+rate);yield changed("Đã đổi EXP trong runtime (không sửa cấu hình trên đĩa).",false,"rate",rate);}
            case "autosave" -> {boolean on=bool(b,"on",false);if(on) AutoSaveManager.getInstance().startAutoSave();else AutoSaveManager.getInstance().stopAutoSave();yield changed("Đã "+(on?"bật":"tắt")+" AutoSave.",false,"on",AutoSaveManager.getInstance().isRunning());}
            case "save-all", "save" -> {int count=AutoSaveManager.getInstance().handleAutoSave();yield changed("Đã gọi dịch vụ lưu người chơi; dịch vụ legacy chỉ báo số lần thử, không xác nhận từng commit.",false,"attempted",count,"verified",false);}
            case "kick" -> {long id=longValue(b,"id",-1);Player p=Client.gI().getPlayer(id);if(p==null || p.isBot || p.getSession()==null) throw error(404,"Người chơi không online");Client.gI().kickSession(p.getSession());yield changed("Đã ngắt phiên người chơi.",false,"id",id);}
            case "kick-all" -> {int count=0;for(Player p:players()) if(!p.isBot && p.getSession()!=null){Client.gI().kickSession(p.getSession());count++;}yield changed("Đã ngắt "+count+" phiên người chơi thật.",false,"kicked",count);}
            case "broadcast" -> {String message=name(b,"message",null,500);Service.gI().sendThongBaoAllPlayer(message);yield changed("Đã gửi thông báo.",false);}
            case "maintenance-now" -> {int seconds=field(b,"seconds",60,5,86400);ServerRuntimeMaintenance.start(seconds);yield changed("Đã lên lịch đóng máy chủ sau "+seconds+" giây; KHÔNG tự chạy tiến trình restart.",false,"seconds",seconds,"shutdownScheduled",true,"restartSupported",false);}
            case "auto-maintenance", "auto-maintenance-time" -> {boolean on=bool(b,"on",false);int h=field(b,"hour",ServerRuntimeMaintenance.hour(),0,23),m=field(b,"minute",ServerRuntimeMaintenance.minute(),0,59);ServerRuntimeMaintenance.configure(on,h,m);yield changed("Đã cập nhật lịch bảo trì chỉ shutdown trong runtime; không khởi chạy run.bat và không lưu qua restart.",false,"on",on,"time",ServerRuntimeMaintenance.time(),"persistent",false);}
            case "restart" -> throw error(501,"Không hỗ trợ restart tiến trình: không có runtime launcher an toàn, không gọi System.exit/run.bat từ panel.");
            case "currency" -> throw error(422,"Không có chế độ tiền tệ toàn server: gold là long; wire encoding phụ thuộc phiên bản client (214). Không thay đổi bằng boolean int.");
            case "bot-create" -> createBots(b);
            case "bot-delete" -> deleteBots(b);
            case "boss-summon" -> summon(b);
            case "reset-boss" -> resetBosses(false);
            case "respawn-boss" -> resetBosses(true);
            case "reload-giftcodes" -> reloadGiftcodes();
            case "reload-shops" -> reloadShops();
            case "reload-npcs" -> reloadNpcs();
            case "reload-top" -> reloadTop();
            case "reward-top" -> rewardTop();
            case "clean-trash" -> {int count=MobMeCleanerService.gI().cleanAll();yield changed("Đã dọn "+count+" mobMe (trứng/đối tượng sót) bằng dịch vụ gameplay.",false,"cleared",count);}
            case "set-event" -> setEvent(b);
            default -> throw error(400,"Tác vụ server không hợp lệ: "+action);
        };
    }
    static List<Player> players(){return new ArrayList<>(Client.gI().getPlayers());}
    static String uptime(long millis){long s=millis/1000;return (s/86400)+"d "+String.format(Locale.ROOT,"%02d:%02d:%02d",s/3600%24,s/60%60,s%60);}
    static List<BossManager> bossManagers(){return List.of(BossManager.gI(),BrolyManager.gI(),FinalBossManager.gI(),YardartManager.gI(),OtherBossManager.gI(),RedRibbonHQManager.gI(),TreasureUnderSeaManager.gI(),SnakeWayManager.gI(),GasDestroyManager.gI(),TrungThuEventManager.gI(),HitEventManager.gI(),ChristmasEventManager.gI(),HungVuongEventManager.gI(),LunarNewYearEventManager.gI(),TeaEventManager.gI(),SkillSummonedManager.gI(),Boss12hManager.gI(),Boss14hManager.gI(),Bktne.gI());}
    static List<Boss> allBosses(){
        Set<Boss> seen=Collections.newSetFromMap(new IdentityHashMap<>());List<Boss> out=new ArrayList<>();
        for(BossManager m:bossManagers()) for(Boss b:new ArrayList<>(m.getBosses())) if(b!=null && seen.add(b)) out.add(b);
        return out;
    }
    static Map<String,Object> playerView(Player p){
        return dto("id",p.id,"name",Objects.toString(p.name,""),"power",p.nPoint==null?0:p.nPoint.power,
                "mapId",p.zone==null?-1:p.zone.map.mapId,"mapName",p.zone==null?"":p.zone.map.mapName,"zoneId",p.zone==null?-1:p.zone.zoneId,
                "task",p.playerTask!=null && p.playerTask.taskMain!=null?Objects.toString(p.playerTask.taskMain.name,""):"");
    }
    static short bossHead(Boss boss) {
        // Resting bosses start at level -1, and some leave-map transitions advance past the
        // last level. Reading their normal getHead() would index outside the boss data array.
        var levels = boss.data;
        int level = boss.currentLevel;
        if (levels == null) return -1;
        if (level >= 0 && level < levels.length && levels[level] != null) {
            try { return boss.getHead(); }
            catch (IndexOutOfBoundsException | NullPointerException changedDuringRead) {
                // The boss tick can change levels while the read-only panel takes its snapshot.
            }
        }
        for (var data : levels) {
            if (data != null && data.getOutfit() != null && data.getOutfit().length > 0) return data.getOutfit()[0];
        }
        return -1;
    }
    static Object snapshot(){
        Map<String,Object> out=status();Runtime rt=Runtime.getRuntime();
        out.put("ramUsedMb",(rt.totalMemory()-rt.freeMemory())/1048576);out.put("ramTotalMb",rt.maxMemory()/1048576);
        var os=ManagementFactory.getOperatingSystemMXBean();double cpu=os instanceof com.sun.management.OperatingSystemMXBean m?m.getProcessCpuLoad():-1;
        out.put("cpuPercent",cpu<0?0:Math.round(cpu*10000)/100.0);out.put("cpuAvailable",cpu>=0);out.put("threads",ManagementFactory.getThreadMXBean().getThreadCount());
        out.put("sessions",SessionManager.gI().getNumSession());out.put("expRate",Manager.RATE_EXP_SERVER);out.put("giftcodes",GiftCodeManager.gI().listGiftCode.size());
        out.put("consignItems",ConsignShopManager.gI().listItem.size());out.put("autoMaintenance",ServerRuntimeMaintenance.enabled());out.put("autoMaintenanceTime",ServerRuntimeMaintenance.time());
        out.put("legacyAutoMaintenance",AutoMaintenance.AutoMaintenance);out.put("autoSave",AutoSaveManager.getInstance().isRunning());out.put("currencyInt",false);
        out.put("adminOnly",services.LoginAccessConfig.isAdminOnly());
        out.put("playerList",players().stream().filter(p->p!=null && !p.isBot && !p.isBoss && !p.isPet).map(ServerRuntimeApi::playerView).toList());
        List<Map<String,Object>> bosses=new ArrayList<>();int alive=0,dead=0,resting=0;
        for(Boss b:allBosses()){
            if(b.bossStatus==BossStatus.REST)resting++;else if(b.bossStatus==null || b.isDie() || Set.of(BossStatus.RESPAWN,BossStatus.DIE,BossStatus.CHAT_E,BossStatus.LEAVE_MAP).contains(b.bossStatus))dead++;else alive++;
            var row=playerView(b);row.putAll(dto("status",b.bossStatus==null?"UNKNOWN":b.bossStatus.name(),"hpMax",b.nPoint==null?0:b.nPoint.hpMax,"dame",b.nPoint==null?0:b.nPoint.dame,
                    "head",bossHead(b),"summonSupported",summonable(b)));bosses.add(row);
        }
        out.put("bossList",bosses);out.put("bosses",dto("alive",alive,"dead",dead,"resting",resting));
        int classic=0,player=0,ba=0,bd=0;for(Bot b:new ArrayList<>(BotManager.gI().bot)){if(b.type==Bot.TYPE_NHU_PLAYER)player++;else classic++;if(b.isDie())bd++;else ba++;}
        out.put("botStats",dto("total",classic+player,"classic",classic,"player",player,"event",0,"alive",ba,"dead",bd,"spawning",BotManager.gI().isSpawning()));
        List<Map<String,Object>> events=events();out.put("events",events);int id=Manager.SU_KIEN;
        out.put("event",dto("id",id,"name",id>=0 && id<EVENT_NAMES.length?EVENT_NAMES[id]:"Mã legacy "+id));
        out.put("capabilities",dto("restart",false,"currency",false,"eventExclusive",false,"eventToggle",true,"eventIds",List.of(1,2,3,4,5,6,7),"botGroups",List.of("classic","player"),"botTypes",List.of(0,1,2,3,4,5),"autoMaintenanceCustomTime",true,"autoMaintenancePersistent",false));
        return out;
    }
    static String group(Bot b){return b.type==Bot.TYPE_NHU_PLAYER?"player":"classic";}
    static void validateGroup(String group){if(!Set.of("","classic","player","event").contains(group))throw error(400,"Nhóm bot không hợp lệ");}
    static boolean botMatches(Bot b,String group,String type,String needle,Long id){return (group.isEmpty() || group.equals(group(b))) && (type.isEmpty() || b.type==number(type,-1)) && (id==null || b.id==id) && (needle.isEmpty() || Objects.toString(b.name,"").toLowerCase(Locale.ROOT).contains(needle) || Long.toString(b.id).contains(needle));}
    static Object bots(Map<String,String> q){
        String group=q.getOrDefault("group",""),type=q.getOrDefault("type",""),needle=q.getOrDefault("q","").trim().toLowerCase(Locale.ROOT);validateGroup(group);if(!type.isEmpty())range(number(type,-1),0,5,"type");
        List<Map<String,Object>> found=new ArrayList<>();int limit=limit(q);
        for(Bot b:new ArrayList<>(BotManager.gI().bot)) if(botMatches(b,group,type,needle,null)){
            var r=playerView(b);r.putAll(dto("group",group(b),"groupName",group(b).equals("player")?"Bot như người chơi":"Bot cổ điển","type",b.type,"hp",b.nPoint==null?0:b.nPoint.hp,"mp",b.nPoint==null?0:b.nPoint.mp,"x",b.location==null?0:b.location.x,"y",b.location==null?0:b.location.y,"dead",b.isDie()));found.add(r);
        }
        return dto("rows",found.subList(0,Math.min(limit,found.size())),"total",found.size(),"shown",Math.min(limit,found.size()),"limit",limit);
    }
    static Object createBots(JsonObject b){
        int type=field(b,"type",-1,0,5),count=field(b,"quantity",-1,1,500);
        if(Maintenance.isRunning || !ServerManager.isRunning) throw error(409,"Máy chủ không nhận bot mới khi dừng/bảo trì");
        try{int accepted=BotManager.gI().spawnBots(type,count,null);return changed("Đã nhận tạo "+accepted+" bot bất đồng bộ; theo dõi botStats.spawning và danh sách để biết kết quả thực tế.",false,"accepted",accepted,"requested",count,"completed",false,"async",true);}
        catch(IllegalStateException busy){throw error(409,busy.getMessage());}
    }
    static Object deleteBots(JsonObject b){
        String group=text(b,"group",""),type=text(b,"type",""),needle=text(b,"keyword",text(b,"q","")).trim().toLowerCase(Locale.ROOT);validateGroup(group);if(!type.isEmpty())range(number(type,-1),0,5,"type");
        Long id=b.has("id")?longValue(b,"id",0):null;
        if(id==null && group.isEmpty() && type.isEmpty() && needle.isEmpty()) throw error(400,"Cần ID hoặc bộ lọc cụ thể để xóa bot");
        if(BotManager.gI().isSpawning()) throw error(409,"Chờ lô bot đang tạo xong trước khi xóa theo bộ lọc");
        int removed=0;for(Bot bot:new ArrayList<>(BotManager.gI().bot)) if(botMatches(bot,group,type,needle,id)){BotManager.gI().xoaBot(bot);removed++;}
        if(removed==0) throw error(404,"Không có bot khớp bộ lọc");
        return changed("Đã gỡ "+removed+" bot khỏi manager, client và map.",false,"deleted",removed);
    }
    static boolean summonable(Boss b){return b.data!=null && b.data.length>0 && b.data[0]!=null && b.data[0].getTypeAppear()==AppearType.DEFAULT_APPEAR;}
    static Object summon(JsonObject b){
        if(!b.has("id"))throw error(400,"Thiếu id boss");long id=longValue(b,"id",0);
        List<Boss> matches=allBosses().stream().filter(boss->boss.id==id).toList();
        if(matches.isEmpty())throw error(404,"Không có boss ID này trong runtime; không tạo mù boss phụ/phó bản");
        Boss target=null;
        for(Boss boss:matches) if(boss.bossStatus==BossStatus.REST && summonable(boss)){target=boss;break;}
        if(target==null)throw error(409,"Boss chưa nghỉ hoặc thuộc loại gọi bởi boss khác/phó bản; không ép spawn để tránh phá chuỗi gameplay");
        synchronized(target){if(target.bossStatus!=BossStatus.REST)throw error(409,"Trạng thái boss đã thay đổi");target.lastTimeRest=0;target.changeStatus(BossStatus.RESPAWN);}
        return changed("Đã đưa boss nghỉ vào chu trình RESPAWN tự nhiên; chờ tick manager cho vào map.",false,"id",id,"status","RESPAWN","completed",false);
    }
    static Object resetBosses(boolean respawn){
        int eligible=0;
        for(Boss b:allBosses()) if(respawn?b.bossStatus==BossStatus.REST && summonable(b):b.bossStatus!=null && b.bossStatus!=BossStatus.REST)eligible++;
        for(BossManager manager:bossManagers()) if(respawn)manager.respawnAllRestingBosses();else manager.resetAllBosses();
        return changed(respawn?"Đã yêu cầu hồi sinh boss nghỉ DEFAULT_APPEAR qua managers.":"Đã đưa boss đang chạy vào chu trình DIE/LEAVE_MAP của managers, không gọi reward hay xóa registry.",false,"requested",eligible,"completed",false);
    }
    static Object reloadShops() throws Exception {
        try(Connection c=AlyraManager.getConnection_Data()){
            var loaded=ShopDAO.getShops(c);if(loaded==null)throw error(500,"ShopDAO không tải được shop; giữ runtime cũ");
            int expected=one(c,"SELECT COUNT(*) AS n FROM tab_shop").get("n") instanceof Number n?n.intValue():-1;
            int actual=loaded.stream().mapToInt(s->s.tabShops.size()).sum();
            if(actual!=expected)throw error(409,"ShopDAO tải thiếu tab; không công bố snapshot không đầy đủ");
            Manager.SHOPS=loaded;
            return changed("Đã nạp shop qua ShopDAO; người chơi mở lại shop để nhận dữ liệu mới.",false,"reloaded",loaded.size());
        }
    }
    static Object reloadGiftcodes() throws Exception {
        List<GiftCode> staged=new ArrayList<>();
        try(Connection c=AlyraManager.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM giftcode WHERE count_left>0 AND (expired IS NULL OR expired>NOW())");ResultSet r=p.executeQuery()){
            while(r.next()){
                GiftCode g=new GiftCode();g.id=r.getInt("id");g.code=r.getString("code");g.countLeft=r.getInt("count_left");g.datecreate=r.getTimestamp("datecreate");g.dateexpired=r.getTimestamp("expired");
                try{String ag=r.getString("allGender");g.allGender=ag==null || Set.of("all","true","1").contains(ag.toLowerCase(Locale.ROOT));}catch(SQLException absent){g.allGender=true;}
                try{g.playerName=r.getString("player_name");}catch(SQLException absent){g.playerName=null;}
                String raw=r.getString("detail");
                if(raw!=null && !raw.isBlank()) for(JsonElement el:new JsonParser().parse(raw).getAsJsonArray()){
                    JsonObject o=el.getAsJsonObject();int id=integer(o,"id",0),quantity=integer(o,"quantity",1);ArrayList<Item.ItemOption> options=new ArrayList<>();
                    if(o.has("options"))for(JsonElement option:o.getAsJsonArray("options")){JsonObject op=option.getAsJsonObject();options.add(new Item.ItemOption(integer(op,"id",0),integer(op,"param",0)));}
                    g.detail.put(id,quantity);g.option.put(id,options);
                }
                staged.add(g);
            }
        }
        GiftCodeManager manager=GiftCodeManager.gI();synchronized(manager){manager.listGiftCode.clear();manager.listGiftCode.addAll(staged);}
        return changed("Đã nạp giftcode theo cùng điều kiện sử dụng của GiftCodeManager.",false,"reloaded",staged.size());
    }
    static Object reloadNpcs() throws Exception {
        List<NpcTemplate> staged=new ArrayList<>();
        try(Connection c=AlyraManager.getConnection_Data()){
            for(var r:rows(c,"SELECT id,name,head,body,leg,avatar FROM npc_template ORDER BY id")){
                NpcTemplate t=new NpcTemplate();t.id=range(n(r,"id"),0,127,"npc id");t.name=s(r,"name");t.head=n(r,"head");t.body=n(r,"body");t.leg=n(r,"leg");t.avatar=n(r,"avatar");staged.add(t);
            }
        }
        if(staged.size()!=Manager.NPC_TEMPLATES.size())throw error(409,"Thay đổi số NPC cần restart vì ID là offset client");
        for(int i=0;i<staged.size();i++)if(staged.get(i).id!=Manager.NPC_TEMPLATES.get(i).id)throw error(409,"Thứ tự ID NPC khác runtime; không reindex, cần restart");
        synchronized(Manager.NPC_TEMPLATES){for(int i=0;i<staged.size();i++)Manager.NPC_TEMPLATES.set(i,staged.get(i));}
        return changed("Đã nạp NPC template. NPC trên map và client đã cache không được tái tạo nóng; cần restart/resync để đổi hình/vị trí.",true,"reloaded",staged.size(),"clientResyncRequired",true);
    }
    static Object reloadTop() throws Exception {
        try(Connection c=AlyraManager.getConnection()){
            // Verify source queries before invoking legacy service, which logs rather than propagates failures.
            synchronized(TopService.gI()){
                for(var top:new ArrayList<>(TopManager.TOP_TEMPLATE))try(PreparedStatement p=c.prepareStatement(top.query);ResultSet r=p.executeQuery()){r.getMetaData();}
                TopService.gI().loadListTop(c);
            }
        }
        return changed("Đã chạy TopService.loadListTop; giữ nguyên bộ hẹn giờ/tiến độ trao quà. Dịch vụ legacy có thể bỏ qua từng dòng lỗi (xem log).",false,"reloaded",TopManager.TOP_TEMPLATE.size(),"rowErrorsAvailableInServerLog",true);
    }
    static Object rewardTop() throws Exception {
        return rewardTop(null);
    }
    static Object rewardTop(Integer selectedId) throws Exception {
        int eligible=0,awarded=0;
        synchronized(TopService.gI()){
            try(Connection c=AlyraManager.getConnection()){
                for(TOPAUTO top:new ArrayList<>(TopManager.AUTO_GIFT_TOPS)){
                    if(selectedId!=null && top.template.id!=selectedId)continue;
                    if(top.isDone || !top.isAuto || top.isSend || top.date==null || !top.isToDate() || top.limit<=0 || top.items==null || top.items.isEmpty())continue;
                    eligible++;TopService.gI().loadTop(c,top.template);
                    top.isSend=true;
                    try{
                        for(int i=0;i<Math.min(top.limit,top.template.tops.size());i++){
                            Player p=top.template.tops.get(i).getPlayer();
                            if(p==null || !top.isNonReceive((int)p.id) || top.items.get(i+1)==null || top.items.get(i+1).isEmpty())continue;
                            top.addItemAutoGift(c,i+1,p);
                            if(top.isNonReceive((int)p.id))throw error(500,"Dịch vụ trao top không ghi nhận người nhận; xem log và thử lại an toàn");
                            awarded++;
                        }
                        top.update();
                    }finally{top.isSend=false;}
                }
            }
        }
        if(eligible==0)throw error(409,"Không có top đã tới ngày, bật auto, còn chưa hoàn tất và có cấu hình quà");
        return changed("Đã chạy trao top qua TOPAUTO theo lịch và danh sách chưa nhận; không ép ngày/đua top hoặc phát quà lặp.",false,"eligibleTops",eligible,"recipientsRecorded",awarded,"persistenceVerification","legacy-service-logs");
    }
    static boolean activeEvent(int id){return switch(id){case 1->EventManager.HALLOWEEN;case 2->EventManager.INTERNATIONAL_WOMANS_DAY;case 3->EventManager.CHRISTMAS;case 4->EventManager.LUNNAR_NEW_YEAR;case 5->EventManager.TRUNG_THU;case 6->EventManager.HUNG_VUONG;case 7->EventManager.TOP_UP;default->false;};}
    static List<Map<String,Object>> events(){List<Map<String,Object>> out=new ArrayList<>();for(int id=1;id<EVENT_NAMES.length;id++)out.add(dto("id",id,"name",EVENT_NAMES[id],"active",activeEvent(id)));return out;}
    static Object setEvent(JsonObject b) throws Exception {
        int id=field(b,"id",-1,1,7);boolean on=bool(b,"on",true);boolean restart=id!=1;
        if(id==1){
            Path file=Path.of("active_event.txt");String previous=Files.exists(file)?Files.readString(file):"";
            LinkedHashSet<String> ids=new LinkedHashSet<>();for(String part:previous.split("[-\\s]+"))if(!part.isBlank())ids.add(part.trim());
            if(on)ids.add("1");else ids.remove("1");
            Path parent=file.toAbsolutePath().getParent();Path temporary=Files.createTempFile(parent,"panel-events-",".tmp");
            try{Files.writeString(temporary,String.join("-",ids)+System.lineSeparator());try{Files.move(temporary,file.toAbsolutePath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException noAtomic){Files.move(temporary,file.toAbsolutePath(),StandardCopyOption.REPLACE_EXISTING);}}
            finally{Files.deleteIfExists(temporary);}
            EventManager.setHalloweenEnabled(on);
        }else switch(id){case 2->EventManager.INTERNATIONAL_WOMANS_DAY=on;case 3->EventManager.CHRISTMAS=on;case 4->EventManager.LUNNAR_NEW_YEAR=on;case 5->EventManager.TRUNG_THU=on;case 6->EventManager.HUNG_VUONG=on;case 7->EventManager.TOP_UP=on;default->throw error(400,"Sự kiện không hợp lệ");}
        Manager.SU_KIEN=(byte)id;
        return changed(id==1?"Đã đổi Halloween qua EventManager và lưu active_event.txt.":"Đã đổi cờ EventManager trong runtime; không tái tạo/xóa NPC và boss sự kiện đang chạy. Cờ này không có loader cấu hình bền vững.",restart,
                "id",id,"on",on,"exclusive",false,"persistent",id==1,"liveApplied",true,"events",events());
    }
}
