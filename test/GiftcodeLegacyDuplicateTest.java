package server.web.panel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import models.GiftCode;
import models.Template.ItemOptionTemplate;
import models.Template.ItemTemplate;
import server.Manager;

/** Legacy duplicate rewards must not block reads/sync/batches or inflate actual game rewards. */
public final class GiftcodeLegacyDuplicateTest {
    private static int checks;
    private static final CatalogData CATALOG = new CatalogData();
    private static final Timestamp CREATED = Timestamp.valueOf("2026-01-01 00:00:00");
    private static final Timestamp EXPIRED = Timestamp.valueOf("2030-01-01 00:00:00");
    private static Constructor<?> rowConstructor;
    private static Method read, dto, strict, active;

    public static void main(String[] args) throws Exception {
        for (int id=0;id<252;id++) Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"Option #",0));
        for (int id=0;id<=1474;id++) {
            ItemTemplate template = new ItemTemplate();
            template.id=(short)id;template.type=27;template.gender=3;template.name="Item "+id;
            Manager.ITEM_TEMPLATES.add(template);
        }
        for (int id : new int[]{568,1474}) CATALOG.items.put(id,Map.of("id",id,"name","Item "+id,"icon",123));
        CATALOG.options.put(50,Map.of("id",50,"name","SD #%"));
        Class<?> row = Class.forName("server.web.panel.GiftcodeApi$Row");
        rowConstructor=row.getDeclaredConstructors()[0];rowConstructor.setAccessible(true);
        read=method("storedRewards",row,CatalogData.class);
        dto=method("dto",row,CatalogData.class);
        strict=method("rewards",JsonArray.class,CatalogData.class);
        active=method("active",Connection.class,CatalogData.class);

        for (int id : new int[]{1474,568,-1,-2,-3}) for (int quantity : new int[]{1,75,Integer.MAX_VALUE}) {
            JsonArray input = new JsonArray();
            input.add(reward(id,9,1));input.add(reward(id,quantity,24));
            String original=input.toString();Object old=row(original);
            JsonArray normalized=(JsonArray)call(read,old,CATALOG);
            check(normalized.size()==1,"Repeated legacy item/currency renders exactly once");
            check(normalized.get(0).getAsJsonObject().get("quantity").getAsInt()==quantity,"Last quantity wins; never summed");
            JsonArray options=normalized.get(0).getAsJsonObject().getAsJsonArray("options");
            check(id<0 ? options.size()==0 : options.get(0).getAsJsonObject().get("param").getAsInt()==24,"Last options remain paired with last quantity");
            Map<?,?> rendered=(Map<?,?>)call(dto,old,CATALOG);
            List<?> items=(List<?>)rendered.get("items");
            check(items.size()==1&&((Map<?,?>)items.get(0)).get("quantity").equals(quantity),"Actual DTO no longer throws duplicate-ID error");
            check(rendered.get("countLeft").equals(7)&&rendered.get("code").equals("legacy")&&rendered.get("playerName").equals("allowed"),"Code stock and restrictions preserved");
            check(input.toString().equals(original),"Read does not edit stored JSON input");
            rejected(() -> call(strict,input,CATALOG),400,"Same duplicates still rejected in a new request");

            GiftcodeBatchTest.Memory db=new GiftcodeBatchTest.Memory();
            db.saved.add(GiftcodeBatchTest.row(1,"legacy",7,original));
            List<GiftCode> loaded=(List<GiftCode>)call(active,db.connection(),CATALOG);
            check(loaded.size()==1&&loaded.get(0).detail.size()==1&&loaded.get(0).detail.get(id)==quantity,"Reload stages exactly the reward already granted by game");
            check(id<0 ? loaded.get(0).option.get(id).isEmpty() : loaded.get(0).option.get(id).get(0).param==24,"Reload retains final options");
            check(db.saved.get(0).get("detail").equals(original)&&db.commits==0,"Reload performs no database write");
        }

        JsonArray mixed=new JsonArray();mixed.add(reward(1474,2,3));mixed.add(reward(568,4,6));
        mixed.add(reward(1474,8,12));mixed.add(reward(-2,100,0));mixed.add(reward(1474,9,15));
        JsonArray normalized=(JsonArray)call(read,row(mixed.toString()),CATALOG);
        check(normalized.size()==3&&normalized.get(0).getAsJsonObject().get("id").getAsInt()==1474
                &&normalized.get(1).getAsJsonObject().get("id").getAsInt()==568,"Other rewards and first-occurrence display order preserved");
        check(normalized.get(0).getAsJsonObject().get("quantity").getAsInt()==9,"Three occurrences use final value");
        check(((JsonArray)call(strict,normalized,CATALOG)).equals(normalized),"Displayed legacy rewards can be submitted as a normal edit");

        GiftcodeBatchTest.Memory db=new GiftcodeBatchTest.Memory();
        String original=mixed.toString();db.saved.add(GiftcodeBatchTest.row(1,"legacy",7,original));
        GiftcodeBatchTest.TestManager manager=new GiftcodeBatchTest.TestManager(db);
        Map<?,?> result=(Map<?,?>)GiftcodeApi.createBatch(GiftcodeBatchTest.payload(),CATALOG,db.connection(),manager,GiftcodeBatchTest.counter());
        check(((List<?>)result.get("codes")).size()==50&&db.saved.size()==51&&db.commits==1,"Existing duplicate rewards no longer prevent creating fifty single-use codes");
        check(db.saved.get(0).get("detail").equals(original)&&db.saved.get(0).get("count_left").equals(7),"Batch preserves legacy database row exactly");
        GiftCode legacy=manager.listGiftCode.stream().filter(g->g.id==1).findFirst().orElseThrow();
        check(legacy.detail.size()==3&&legacy.detail.get(1474)==9&&legacy.option.get(1474).get(0).param==15,"Atomic sync publishes same effective legacy rewards");

        JsonObject duplicateRequest=GiftcodeBatchTest.payload();duplicateRequest.getAsJsonArray("items").add(reward(568,99,12));
        GiftcodeBatchTest.Memory untouched=new GiftcodeBatchTest.Memory();
        rejected(() -> GiftcodeApi.createBatch(duplicateRequest,CATALOG,untouched.connection(),new GiftcodeBatchTest.TestManager(untouched),GiftcodeBatchTest.counter()),400,"New batch duplicate is rejected before writes");
        check(untouched.saved.isEmpty()&&untouched.commits==0,"Rejected request does not create codes");

        JsonArray bad=new JsonArray();bad.add(reward(1474,2,3));bad.add(reward(1474,0,9));
        rejected(() -> call(read,row(bad.toString()),CATALOG),400,"Invalid quantities remain rejected");
        JsonArray unknown=new JsonArray();unknown.add(reward(9999,1,3));
        rejected(() -> call(read,row(unknown.toString()),CATALOG),422,"Unavailable item validation remains enabled");
        JsonArray badOption=new JsonArray();badOption.add(reward(1474,1,3));
        badOption.get(0).getAsJsonObject().getAsJsonArray("options").add(new JsonParser().parse("{\"id\":999,\"param\":1}"));
        rejected(() -> call(read,row(badOption.toString()),CATALOG),422,"Unavailable option validation remains enabled");
        System.out.println("PASS: "+checks+" legacy duplicate DTO/reload/batch checks; final quantity/options preserved, strict writes retained, no live DB access");
    }

    private static JsonObject reward(int id,int quantity,int param) {
        JsonObject result=new JsonObject();result.addProperty("id",id);result.addProperty("quantity",quantity);
        JsonArray options=new JsonArray();
        if(id>=0){JsonObject option=new JsonObject();option.addProperty("id",50);option.addProperty("param",param);options.add(option);}
        result.add("options",options);return result;
    }
    private static Object row(String detail) throws Exception {return rowConstructor.newInstance(1,"legacy",7,detail,"all",CREATED,EXPIRED,"allowed");}
    private static Method method(String name,Class<?>...types) throws Exception {Method m=GiftcodeApi.class.getDeclaredMethod(name,types);m.setAccessible(true);return m;}
    private static Object call(Method m,Object...args) throws Exception {
        try{return m.invoke(null,args);}catch(InvocationTargetException e){if(e.getCause() instanceof Exception x)throw x;throw e;}
    }
    private interface Action {void run() throws Exception;}
    private static void rejected(Action action,int status,String message) throws Exception {
        try{action.run();throw new AssertionError(message);}catch(PanelSupport.PanelException e){check(e.status==status,message);}
    }
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
}
