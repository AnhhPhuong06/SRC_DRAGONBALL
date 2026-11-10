package server.web.panel;

import com.sun.net.httpserver.HttpExchange;
import database.DatabaseResetGuard;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import network.session.MySession;
import network.session.SessionManager;
import server.*;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.CatalogShopApi.fields;

final class DatabaseResetApi {
    static final String CONFIRMATION="RESET ACCOUNT PLAYER CLAN";
    record RuntimeState(int players,int authenticatedSessions,boolean ready,boolean maintenance) {
        List<String> blockers(){var out=new ArrayList<String>();if(!ready)out.add("Chờ server khởi động hoàn tất.");if(maintenance)out.add("Đang có lịch đóng/bảo trì server.");if(players>0)out.add("Còn "+players+" người chơi/bot online. Ngắt toàn bộ trước khi reset.");if(authenticatedSessions>0)out.add("Còn "+authenticatedSessions+" phiên game đã xác thực. Chờ ngắt hoàn tất.");return out;}
    }
    interface Runtime {RuntimeState state();void afterCommit();}
    private record Ticket(String owner,long expires,DatabaseResetStore.Snapshot snapshot) {}
    private static final Runtime LIVE_RUNTIME=new Runtime(){
        public RuntimeState state(){int sessions=0;for(var s:SessionManager.gI().getSessions())if(s instanceof MySession m&&(m.userId>0||m.player!=null||m.joinedGame))sessions++;return new RuntimeState(Client.gI().getOnlineCount(),sessions,ServerManager.isRunning,Maintenance.isRunning||AutoMaintenance.isRunning||ServerRuntimeMaintenance.enabled());}
        public void afterCommit(){AutoSaveManager.getInstance().stopAutoSave();Manager.CLANS.clear();}
    };
    private static final DatabaseResetApi LIVE=new DatabaseResetApi(DatabaseResetGuard.GLOBAL,DatabaseResetStore.LIVE,LIVE_RUNTIME,System::currentTimeMillis);
    private final DatabaseResetGuard guard;private final DatabaseResetStore.Store store;private final Runtime runtime;private final LongSupplier clock;
    private final Map<String,Ticket> tickets=new ConcurrentHashMap<>();private final SecureRandom random=new SecureRandom();private volatile Map<String,Object> result;
    DatabaseResetApi(DatabaseResetGuard guard,DatabaseResetStore.Store store,Runtime runtime,LongSupplier clock){this.guard=guard;this.store=store;this.runtime=runtime;this.clock=clock;}
    static Object handle(HttpExchange e)throws Exception{return LIVE.request(e);}
    Object request(HttpExchange e)throws Exception {
        requireMethod(e,"GET","POST");fields(query(e).keySet());String owner=Objects.toString(e.getAttribute("panel.resetOwner"),"");if(owner.isEmpty())throw error(401,"Cần đăng nhập panel.");
        if(e.getRequestMethod().equals("GET")) {
            if(guard.completed())return mapResult();
            DatabaseResetStore.Snapshot snapshot;
            try{snapshot=store.preview();}catch(Exception failure){if(guard.completed())return mapResult();throw failure;}
            if(guard.completed())return mapResult();
            var state=runtime.state();var blockers=new ArrayList<>(snapshot.blockers());blockers.addAll(state.blockers());
            long now=clock.getAsLong();tickets.entrySet().removeIf(entry->entry.getValue().expires()<now||entry.getValue().owner().equals(owner));
            if(tickets.size()>=1000)throw error(429,"Quá nhiều lượt xem trước reset. Thử lại sau.");
            byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            tickets.put(token,new Ticket(owner,now+120_000,snapshot));
            return AccountPlayerStore.map("completed",false,"database",snapshot.database(),"tables",snapshot.tables(),"blockers",blockers,"canReset",blockers.isEmpty(),"token",token,"expiresAt",now+120_000,"confirmation",CONFIRMATION,"players",state.players(),"authenticatedSessions",state.authenticatedSessions());
        }
        var body=body(e);fields(body,"confirmation","token");
        if(!CONFIRMATION.equals(text(body,"confirmation","")))throw error(400,"Nhập chính xác: "+CONFIRMATION);
        String token=required(body,"token");Ticket ticket=tickets.get(token);
        if(ticket==null||!ticket.owner().equals(owner)||ticket.expires()<clock.getAsLong())throw error(409,"Xác nhận reset đã hết hạn hoặc đã dùng. Tải lại số liệu trước khi reset.");
        try(var lease=guard.exclusive(10_000)) {
            var runtimeBlockers=runtime.state().blockers();if(!runtimeBlockers.isEmpty())throw error(409,String.join(" ",runtimeBlockers));
            if(!tickets.remove(token,ticket))throw error(409,"Lượt reset đã được xử lý.");
            Map<String,Long> deleted;
            try{deleted=store.erase(ticket.snapshot());}
            catch(DatabaseResetStore.UncertainCommit failure){lease.complete();result=AccountPlayerStore.map("completed",true,"uncertain",true,"restartRequired",true,"message",failure.getMessage());throw error(503,failure.getMessage());}
            catch(java.sql.SQLException failure){throw error(503,"Reset không hoàn tất; giao dịch đã được hủy. Tải lại để kiểm tra số liệu.");}
            lease.complete();var warnings=new ArrayList<String>();
            try{runtime.afterCommit();}catch(Exception e2){warnings.add("Đã xóa trong database; cần khởi động lại để nạp lại bộ nhớ server.");}
            result=AccountPlayerStore.map("success",true,"completed",true,"uncertain",false,"restartRequired",true,"database",ticket.snapshot().database(),"deleted",deleted,"warnings",warnings,"message","Đã xóa toàn bộ account, player và clan. Khởi động lại server trước khi mở game.");
            tickets.clear();return result;
        }catch(java.sql.SQLException failure){throw error(409,failure.getMessage());}
    }
    private Map<String,Object> mapResult(){var r=result;return r==null?AccountPlayerStore.map("completed",true,"restartRequired",true,"message",DatabaseResetGuard.RESTART_MESSAGE):r;}
}
