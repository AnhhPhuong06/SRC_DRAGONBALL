package server.web.panel;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import services.LoginAccessConfig;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.CatalogShopApi.fields;

/** Lightweight login-access control, independent of DB-backed runtime statistics. */
final class LoginAccessPanelData {
    private LoginAccessPanelData() {}
    static Object handle(HttpExchange e)throws Exception {
        requireMethod(e,"GET","POST");fields(query(e).keySet(),"action");
        if(e.getRequestMethod().equals("GET"))return state();
        var b=body(e);fields(b,"on");
        if(!b.has("on")||!b.get("on").isJsonPrimitive()||!b.getAsJsonPrimitive("on").isBoolean())throw error(400,"Cần trường on là true hoặc false.");
        boolean on=b.get("on").getAsBoolean();
        try{LoginAccessConfig.setAdminOnly(on);}
        catch(IOException failure){throw error(503,"Không lưu được chế độ đăng nhập. Trạng thái đang chạy được giữ nguyên.");}
        var result=ok(on?"Đã bật chế độ chỉ admin được đăng nhập.":"Đã mở đăng nhập cho tất cả tài khoản.");
        result.put("adminOnly",LoginAccessConfig.isAdminOnly());result.put("persistent",true);result.put("liveApplied",true);return result;
    }
    static Object state(){return AccountPlayerStore.map("adminOnly",LoginAccessConfig.isAdminOnly(),"persistent",true);}
}
