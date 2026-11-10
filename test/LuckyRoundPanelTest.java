package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import models.Template.*;
import models.item.Item;
import models.player.Player;
import server.Manager;
import services.*;
import services.LuckyRoundSettings.*;
import services.func.*;

/** Runs only in an external temporary working directory, with synthetic templates and no DB. */
public final class LuckyRoundPanelTest {
    static int checks;static final Gson JSON=new Gson();static final CatalogData catalog=new CatalogData();
    static final LuckyRoundSettings store=new LuckyRoundSettings();
    static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    static JsonObject json(Object value){return new JsonParser().parse(JSON.toJson(value)).getAsJsonObject();}
    static JsonObject payload(){var b=json(store.state());b.addProperty("revision",LuckyRoundSettings.revision(store.state()));return b;}
    static State modify(State s,Map<String,Integer> values,Map<String,List<Integer>> groups){var v=new LinkedHashMap<>(s.values());v.putAll(values);var g=new LinkedHashMap<>(s.groups());g.putAll(groups);return new State(v,g,s.milestones());}
    static void save(State state)throws Exception{store.save(state,LuckyRoundSettings.revision(store.state()));}
    public static void main(String[] args)throws Exception {
        for(int id=0;id<=2100;id++){var t=new ItemTemplate();t.id=(short)id;t.name=id==568?"Trứng Mabư":id==457?"Thỏi vàng":"Vật phẩm "+id;t.type=27;t.gender=3;t.iconID=(short)id;Manager.ITEM_TEMPLATES.add(t);catalog.items.put(id,Map.of("id",id,"name",t.name,"icon",id));}
        for(int id=0;id<=255;id++){var o=new ItemOptionTemplate(id,"Option #",0);Manager.ITEM_OPTION_TEMPLATES.add(o);catalog.options.put(id,Map.of("id",id,"name",o.name));}
        var original=store.state();LuckyRoundSettings.validate(original);
        check(original.values().size()==9&&original.groups().size()==6&&original.milestones().size()==4,"All existing settings exposed");
        check(original.values().get("idVatPhamQuay")==821&&original.milestones().get(0).spins()==200,"Existing deployed values preserved");
        Files.writeString(Path.of("api-fixture.json"),JSON.toJson(LuckyRoundPanelData.view(store,catalog)));
        Path file=Path.of("data/config/vong_quay_thoi_vang.properties");Files.writeString(file,"\nlegacy_test=preserve\n",StandardOpenOption.APPEND);
        var before=MocVongQuayService.MOCS;var p=new Player();p.lixi=1234;p.mocVongQuay=5;
        var milestones=new ArrayList<Milestone>();for(int i=0;i<4;i++)milestones.add(new Milestone(i,(i+1)*300,List.of(new Reward(568,10+i,List.of(new Option(50,25),new Option(30,0))))));
        var next=modify(original,Map.of("idVatPhamQuay",568,"soLuongVatPhamQuay",2,"mocMoQuaHiem",500,"tiLeMoiMonSieuHiem",100),Map.of("idNhomSieuHiem",List.of(1357),"idNhomSieuHiemSauMoc",List.of(1358)));
        next=new State(next.values(),next.groups(),milestones);String oldRevision=LuckyRoundSettings.revision(original);save(next);
        check(store.state().equals(next),"Save publishes complete validated snapshot");
        check(p.lixi==1234&&p.mocVongQuay==5,"Save preserves existing player spins/claimed bitmask");
        check(before.size()==4&&before.get(0).spinsRequired==200,"Old milestone snapshots stay intact");
        for(int i=0;i<4;i++){var m=MocVongQuayService.MOCS.get(i);check(m.id==i&&m.spinsRequired==(i+1)*300&&m.rewards.get(0).quantity==10+i&&m.rewards.get(0).options[0][1]==25,"New milestone reward/options applied: "+i);}
        var props=new Properties();try(var in=Files.newInputStream(file)){props.load(in);}check(props.getProperty("legacy_test").equals("preserve"),"Unknown legacy properties preserved");
        VongQuayThoiVangConfig.load();check(store.state().equals(next),"Persisted configuration reloads exactly after restart");
        try{store.save(original,oldRevision);throw new AssertionError("Stale save accepted");}catch(ConcurrentModificationException expected){check(store.state().equals(next),"Stale revision cannot overwrite changes");}
        Files.writeString(Path.of("not-a-directory"),"fixture");try{new LuckyRoundSettings(Path.of("not-a-directory/config.properties")).save(original,LuckyRoundSettings.revision(next));throw new AssertionError("IO failure accepted");}catch(java.io.IOException expected){check(store.state().equals(next),"Disk failure leaves runtime and milestones unchanged");}
        rewardAndPrice(next);
        save(original);http();
        System.out.println("PASS: "+checks+" settings validation, atomic persistence/reload, stale edits, runtime reward/price, player progress, milestone publication and authenticated API checks; no live DB access");
    }
    static void rewardAndPrice(State next)throws Exception {
        var p=new Player();p.lixi=0;for(var item:RewardService.gI().getListItemVongQuayThoiVang(p,100))check(item.template.id==1357,"Before threshold uses before rare group");
        p.lixi=500;for(var item:RewardService.gI().getListItemVongQuayThoiVang(p,100))check(item.template.id==1358,"After threshold uses after rare group");
        State rareOff=modify(next,Map.of("tiLeMoiMonSieuHiem",0,"tiLeNhom5",100,"tiLeNhom15",0),Map.of("idNhom5",List.of(18),"idNhom5SauMoc",List.of(17)));
        save(rareOff);p.lixi=0;for(var item:RewardService.gI().getListItemVongQuayThoiVang(p,100))check(item.template.id==18,"Hiem before group applies without restart");
        p.lixi=500;for(var item:RewardService.gI().getListItemVongQuayThoiVang(p,100))check(item.template.id==17,"Hiem after group applies without restart");
        save(modify(rareOff,Map.of("tiLeNhom5",0,"tiLeNhom15",100),Map.of("idNhom15",List.of(20))));
        for(var item:RewardService.gI().getListItemVongQuayThoiVang(p,100))check(item.template.id==20,"Special reward group applies live");
        save(modify(rareOff,Map.of("tiLeNhom5",0,"tiLeNhom15",0,"soLuongHongNgoc",37,"vangMin",12345,"vangMax",12345),Map.of("idVatPhamBoTro",List.of(381))));
        boolean ruby=false,gold=false,booster=false;
        for(var item:RewardService.gI().getListItemVongQuayThoiVang(p,3000)) {
            if(item.template.id==861){ruby=true;check(item.quantity==37,"Common ruby quantity applies live");}
            if(item.template.id==189){gold=true;check(item.quantity==12345,"Common gold min/max applies live");}
            if(item.template.id>=381&&item.template.id<=385){booster=true;check(item.template.id==381&&item.quantity>=1&&item.quantity<=5,"Common booster selection uses configured group");}
        }
        check(ruby&&gold&&booster,"Ordinary table retains all three configurable reward branches");
        save(next);p=new Player();p.mocVongQuay=3;int count=LuckyRound.SO_LAN_QUAY_NHANH;
        p.inventory.itemsBag.add(ItemService.gI().createNewItem((short)568,50));p.inventory.itemsBag.add(ItemService.gI().createNewItem((short)568,count*2-50));p.inventory.itemsBag.add(ItemService.gI().createNewItem((short)821,100));
        LuckyRound.gI().quayNhanhThoiVang(p);check(p.lixi==count&&p.mocVongQuay==3&&p.inventory.itemsBoxCrackBall.size()==count,"Real quick spin adds configured rewards/count and preserves claim mask");
        check(quantity(p,568)==0&&quantity(p,821)==100,"New price consumes exact total across stacks, preserves old ticket");
        check(p.inventory.itemsBoxCrackBall.stream().allMatch(i->i.template.id==1357&&i.isHaveOption(30)),"All actual quick-spin rewards use live table and normal item lock");
        int spins=p.lixi;LuckyRound.gI().quayNhanhThoiVang(p);check(p.lixi==spins&&p.inventory.itemsBoxCrackBall.size()==count,"Insufficient price never grants rewards");
        var full=new Player();full.inventory.itemsBag.add(ItemService.gI().createNewItem((short)568,count*2));for(int i=0;i<models.player.Inventory.MAX_ITEMS_BOX_LUCKY_ROUND;i++)full.inventory.itemsBoxCrackBall.add(new Item());
        LuckyRound.gI().quayNhanhThoiVang(full);check(quantity(full,568)==count*2&&full.lixi==0,"Full side chest never charges new price");
    }
    static int quantity(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
    static void http()throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/panel/",new PanelRouter("fixture-admin","fixture-password",(resource,e)->{check(resource.equals("lucky-round"),"New resource dispatched");return LuckyRoundPanelData.handle(e,store,()->catalog);},System::currentTimeMillis,false));server.start();
        try {
            var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();String base="http://127.0.0.1:"+server.getAddress().getPort()+"/api/panel/";
            check(request(client,base,"lucky-round","GET",null,null).statusCode()==401,"Login required");
            var login=request(client,base,"auth?action=login","POST","{\"username\":\"fixture-admin\",\"password\":\"fixture-password\"}",null);String csrf=new JsonParser().parse(login.body()).getAsJsonObject().get("csrfToken").getAsString();
            check(request(client,base,"lucky-round","POST",payload().toString(),null).statusCode()==403,"CSRF required for saves");
            var get=request(client,base,"lucky-round","GET",null,null);check(get.statusCode()==200&&new JsonParser().parse(get.body()).getAsJsonObject().getAsJsonArray("groupFields").size()==6,"Authenticated GET returns editor contract");
            check(request(client,base,"lucky-round","DELETE",null,csrf).statusCode()==405,"Unsupported methods rejected");
            check(request(client,base,"lucky-round?unexpected=1","GET",null,null).statusCode()==400,"Unknown query rejected");
            String unchanged=LuckyRoundSettings.revision(store.state());byte[] disk=Files.readAllBytes(Path.of("data/config/vong_quay_thoi_vang.properties"));
            for(int i=0;i<24;i++) {
                var b=payload();var v=b.getAsJsonObject("values");var g=b.getAsJsonObject("groups");var m=b.getAsJsonArray("milestones");var r=m.get(0).getAsJsonObject().getAsJsonArray("rewards").get(0).getAsJsonObject();
                switch(i) {
                    case 0->v.addProperty("unknown",1);case 1->v.remove("vangMin");case 2->v.addProperty("soLuongVatPhamQuay",0);case 3->v.addProperty("vangMin",20000000);
                    case 4->{v.addProperty("tiLeNhom5",90);v.addProperty("tiLeNhom15",90);}case 5->v.addProperty("tiLeMoiMonSieuHiem",100);case 6->g.add("idNhom15",new JsonArray());case 7->g.getAsJsonArray("idNhom15").add(20);
                    case 8->m.get(1).getAsJsonObject().addProperty("spins",1);case 9->m.get(0).getAsJsonObject().addProperty("id",3);case 10->r.addProperty("quantity",0);case 11->r.addProperty("itemId",32000);
                    case 12->r.getAsJsonArray("options").get(0).getAsJsonObject().addProperty("id",256);case 13->r.getAsJsonArray("options").get(0).getAsJsonObject().addProperty("param",65536);case 14->r.getAsJsonArray("options").add(r.getAsJsonArray("options").get(0));
                    case 15->v.addProperty("idVatPhamQuay",32000);case 16->g.getAsJsonArray("idNhom15").add(32000);case 17->b.addProperty("revision","stale");case 18->b.add("values",JsonNull.INSTANCE);case 19->v.addProperty("vangMax",1.5);
                    case 20->m.remove(3);case 21->r.add("options",JsonNull.INSTANCE);case 22->r.addProperty("quantity",2147483648L);case 23->b.addProperty("resetPlayers",true);
                }
                var response=request(client,base,"lucky-round","POST",b.toString(),csrf);check(response.statusCode()>=400&&response.statusCode()<500,"Invalid settings rejected: "+i+" response="+response.statusCode()+" "+response.body());
                check(unchanged.equals(LuckyRoundSettings.revision(store.state()))&&Arrays.equals(disk,Files.readAllBytes(Path.of("data/config/vong_quay_thoi_vang.properties"))),"Invalid save leaves runtime/file untouched: "+i);
            }
            check(request(client,base,"lucky-round","POST","{",csrf).statusCode()==400,"Malformed JSON rejected");
            var valid=payload();valid.getAsJsonObject("values").addProperty("soLuongVatPhamQuay",3);var response=request(client,base,"lucky-round","POST",valid.toString(),csrf);
            check(response.statusCode()==200&&new JsonParser().parse(response.body()).getAsJsonObject().get("liveApplied").getAsBoolean()&&VongQuayThoiVangConfig.spinPrice()[1]==3,"Valid authenticated save persists and applies live");
            check(request(client,base,"lucky-round","POST",valid.toString(),csrf).statusCode()==409,"Repeated stale update rejected over HTTP");
            var missing=catalog.items.remove(1618);
            var warned=request(client,base,"lucky-round","GET",null,null);var warnings=new JsonParser().parse(warned.body()).getAsJsonObject().getAsJsonArray("warnings");
            check(warned.statusCode()==200&&warnings.size()==1&&warnings.get(0).getAsString().contains("#1618"),"Actual missing legacy reward is visible as warning");
            var unrelated=payload();unrelated.getAsJsonObject("values").addProperty("soLuongVatPhamQuay",4);
            check(request(client,base,"lucky-round","POST",unrelated.toString(),csrf).statusCode()==200&&VongQuayThoiVangConfig.spinPrice()[1]==4,"Missing legacy reward no longer blocks editing price/rates");
            String legacyRevision=LuckyRoundSettings.revision(store.state());
            for(int i=0;i<5;i++) {
                var bad=payload();var milestones=bad.getAsJsonArray("milestones");var rewards=milestones.get(2).getAsJsonObject().getAsJsonArray("rewards");var legacy=rewards.get(1).getAsJsonObject();
                switch(i){case 0->legacy.addProperty("quantity",6);case 1->rewards.add(new JsonParser().parse(legacy.toString()));case 2->milestones.get(0).getAsJsonObject().getAsJsonArray("rewards").add(new JsonParser().parse(legacy.toString()));case 3->legacy.getAsJsonArray("options").get(0).getAsJsonObject().addProperty("param",1);case 4->legacy.addProperty("itemId",32000);}
                check(request(client,base,"lucky-round","POST",bad.toString(),csrf).statusCode()==422,"Cannot alter, duplicate, move or introduce missing reference: "+i);
                check(LuckyRoundSettings.revision(store.state()).equals(legacyRevision),"Rejected invalid edit preserves runtime: "+i);
            }
            var repaired=payload();repaired.getAsJsonArray("milestones").get(2).getAsJsonObject().getAsJsonArray("rewards").get(1).getAsJsonObject().addProperty("itemId",674);
            check(request(client,base,"lucky-round","POST",repaired.toString(),csrf).statusCode()==200&&MocVongQuayService.MOCS.get(2).rewards.get(1).tempId==674,"Legacy missing item can be replaced and applied live");
            var clean=new JsonParser().parse(request(client,base,"lucky-round","GET",null,null).body()).getAsJsonObject();check(clean.getAsJsonArray("warnings").size()==0,"Repair clears legacy warning");
            catalog.items.put(1618,missing);
            var oldOption=catalog.options.remove(30);var legacyOptions=payload();legacyOptions.getAsJsonObject("values").addProperty("soLuongVatPhamQuay",5);
            check(request(client,base,"lucky-round","POST",legacyOptions.toString(),csrf).statusCode()==200,"Unchanged missing legacy options also do not block unrelated edits");
            var badOption=payload();badOption.getAsJsonArray("milestones").get(0).getAsJsonObject().getAsJsonArray("rewards").get(0).getAsJsonObject().addProperty("quantity",999);
            check(request(client,base,"lucky-round","POST",badOption.toString(),csrf).statusCode()==422,"Cannot edit a reward retaining missing option");catalog.options.put(30,oldOption);
        }finally{server.stop(0);}
    }
    static HttpResponse<String> request(HttpClient c,String base,String path,String method,String body,String csrf)throws Exception{var b=HttpRequest.newBuilder(URI.create(base+path)).method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).header("Content-Type","application/json");if(csrf!=null)b.header("X-CSRF-Token",csrf);return c.send(b.build(),HttpResponse.BodyHandlers.ofString());}
}
