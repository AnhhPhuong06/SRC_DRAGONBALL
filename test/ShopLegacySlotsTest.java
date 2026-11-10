package server.web.panel;

import com.google.gson.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import models.Template.*;
import models.shop.*;
import server.Manager;

/** Exercise the real editor mutation and ShopDAO transaction staging with legacy ignored slots. */
public final class ShopLegacySlotsTest {
    static int checks;static CatalogData catalog=new CatalogData();
    static Method snapshot,stage,mutate,detail,index;
    public static void main(String[] args)throws Exception{
        for(int id=0;id<=1449;id++){ItemTemplate t=new ItemTemplate();t.id=(short)id;t.name="Item "+id;t.gender=3;t.type=21;Manager.ITEM_TEMPLATES.add(t);}
        for(int id=0;id<252;id++){Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"Option #",0));catalog.options.put(id,Map.of("id",id,"name","Option #"));}
        for(int id:new int[]{1,674,1449})catalog.items.put(id,Map.of("id",id,"name","Item "+id,"icon",6467));
        Class<?> cls=Class.forName("server.web.panel.ShopApi$Snapshot");
        snapshot=method("snapshot",Connection.class);stage=method("stage",Connection.class,cls,CatalogData.class);
        mutate=method("mutate",Connection.class,String.class,JsonObject.class,CatalogData.class);
        detail=method("detail",cls,int.class,CatalogData.class);index=method("index",cls);
        Memory db=new Memory();String legacy=db.saved.get(30);
        try(Connection c=db.connection()){
            Object graph=call(snapshot,c);Map<?,?> dto=(Map<?,?>)call(detail,graph,30,catalog);
            Map<?,?> tab=(Map<?,?>)((List<?>)dto.get("tabs")).get(0);List<?> rows=(List<?>)tab.get("items");
            check(rows.size()==2&&tab.get("itemCount").equals(2),"Legacy null/primitive/array slots do not count as actual items");
            check(((Map<?,?>)rows.get(0)).get("id").equals(2)&&((Map<?,?>)rows.get(1)).get("id").equals(7),"Panel IDs preserve original physical positions including hidden items");
            Map<?,?> list=(Map<?,?>)call(index,graph);List<?> shops=(List<?>)list.get("shops");
            check(((Map<?,?>)shops.get(0)).get("itemCount").equals(2),"Shop sidebar count ignores skipped slots");
            List<Shop> loaded=(List<Shop>)call(stage,c,graph,catalog);
            check(loaded.size()==3&&loaded.get(0).tabShops.get(0).itemShops.size()==1,"Real DAO staging agrees with ignored slots and is_sell flags");
        }
        JsonObject add=new JsonParser().parse("{\"tabId\":64,\"tempId\":1449,\"cost\":100,\"typeSell\":0,\"iconSpec\":6467,\"isNew\":false,\"isSell\":true,\"options\":[{\"id\":30,\"param\":0}]}").getAsJsonObject();
        add.addProperty("itemsVersion",ShopApi.version("[]"));
        List<Shop> loaded=save(db,"item-create",add);
        ItemShop created=loaded.stream().filter(s->s.id==49).findFirst().orElseThrow().tabShops.get(0).itemShops.get(0);
        check(created.temp.id==1449&&created.cost==100&&created.iconSpec==6467&&created.options.get(0).optionTemplate.id==30&&created.options.get(0).param==0,"Exact screenshot item, price, icon and option load in live shop objects");
        check(db.saved.get(30).equals(legacy)&&db.commits==1,"Adding to Tori-bot commits without rewriting unrelated legacy tab");
        check(loaded.stream().filter(s->s.id==38).findFirst().orElseThrow().tabShops.get(0).itemShops.get(0).cost==15625000000L,"Existing skill cost uses full 64-bit value during complete staging");
        JsonObject high=add.deepCopy();high.addProperty("tabId",32);high.addProperty("itemsVersion",ShopApi.version(db.saved.get(32)));high.addProperty("cost",15625000000L);
        save(db,"item-create",high);
        check(new JsonParser().parse(db.saved.get(32)).getAsJsonArray().get(1).getAsJsonObject().get("cost").getAsLong()==15625000000L,"New skill-shop cost can exceed signed 32-bit without truncation");
        high.addProperty("tabId",64);high.addProperty("itemsVersion",ShopApi.version(db.saved.get(64)));
        rejected(()->save(db,"item-create",high),400,"Normal shops retain signed-32-bit protocol cost limit");
        high.addProperty("tabId",32);high.addProperty("itemsVersion",ShopApi.version(db.saved.get(32)));high.addProperty("cost",9007199254740992L);
        rejected(()->save(db,"item-create",high),400,"Skill prices beyond exact browser integer range are rejected");

        db.saved.put(64,"[null]");add.addProperty("itemsVersion",ShopApi.version("[null]"));
        loaded=save(db,"item-create",add);String afterAdd=db.saved.get(64);
        JsonArray saved=new JsonParser().parse(afterAdd).getAsJsonArray();
        check(saved.size()==2&&saved.get(0).isJsonNull()&&saved.get(1).isJsonObject(),"Append preserves legacy slot; it never compacts or renumbers records");
        check(loaded.stream().filter(s->s.id==49).findFirst().orElseThrow().tabShops.get(0).itemShops.get(0).id==1,"Game purchase ID still comes from visible shop order");
        JsonObject update=new JsonObject();update.addProperty("tabId",64);update.addProperty("id",2);update.addProperty("itemsVersion",ShopApi.version(afterAdd));update.addProperty("cost",123);
        save(db,"item-update",update);saved=new JsonParser().parse(db.saved.get(64)).getAsJsonArray();
        check(saved.get(0).isJsonNull()&&saved.get(1).getAsJsonObject().get("cost").getAsInt()==123,"Physical ID updates correct item after skipped slot");
        String current=db.saved.get(64);
        rejected(()->save(db,"item-update",update),409,"Stale itemsVersion still prevents overwriting another change");
        check(db.saved.get(64).equals(current),"Stale rejection leaves database unchanged");
        update.addProperty("id",1);update.addProperty("itemsVersion",ShopApi.version(current));
        rejected(()->save(db,"item-update",update),404,"Ignored slot cannot be edited as a real item");
        update.remove("cost");
        rejected(()->save(db,"item-delete",update),404,"Ignored slot cannot be deleted accidentally");
        JsonObject bad=add.deepCopy();bad.addProperty("itemsVersion",ShopApi.version(current));bad.add("options",new JsonParser().parse("[null]").getAsJsonArray());
        rejected(()->save(db,"item-create",bad),400,"New option input remains strict; null options are rejected");
        check(db.saved.get(64).equals(current),"Invalid new payload rolls back without losing existing items");
        update.addProperty("id",2);save(db,"item-delete",update);
        check(db.saved.get(64).equals("[null]"),"Deleting real item retains ignored legacy filler");
        check(save(db,"sync",new JsonObject()).size()==3,"Explicit shop sync succeeds with old ignored slots");
        db.saved.put(64,current);db.failCommit=true;
        JsonObject retry=add.deepCopy();retry.addProperty("itemsVersion",ShopApi.version(current));
        try{save(db,"item-create",retry);throw new AssertionError("Commit failure ignored");}catch(SQLException expected){}
        check(db.saved.get(64).equals(current),"Commit failure preserves complete old tab");
        System.out.println("PASS: "+checks+" shop add/update/delete/sync, physical IDs, actual ShopDAO, stale revisions and rollback checks; no live DB writes");
    }
    static List<Shop> save(Memory db,String action,JsonObject request)throws Exception{
        try(Connection c=db.connection()){c.setAutoCommit(false);try{call(mutate,c,action,request,catalog);Object graph=call(snapshot,c);List<Shop> shops=(List<Shop>)call(stage,c,graph,catalog);c.commit();return shops;}catch(Exception e){c.rollback();throw e;}}
    }
    static Method method(String name,Class<?>...types)throws Exception{Method m=ShopApi.class.getDeclaredMethod(name,types);m.setAccessible(true);return m;}
    static Object call(Method m,Object...args)throws Exception{try{return m.invoke(null,args);}catch(InvocationTargetException e){if(e.getCause()instanceof Exception x)throw x;throw e;}}
    interface Task{void run()throws Exception;}
    static void rejected(Task task,int status,String message)throws Exception{try{task.run();throw new AssertionError(message);}catch(PanelSupport.PanelException e){check(e.status==status,message);}}
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static JsonObject item(boolean sell){JsonObject row=new JsonParser().parse("{\"temp_id\":1,\"cost\":10,\"type_sell\":0,\"item_spec\":0,\"is_new\":false,\"is_sell\":true,\"options\":[{\"id\":30,\"param\":0}]}").getAsJsonObject();row.addProperty("is_sell",sell);return row;}
    static Map<String,Object> row(Object...values){Map<String,Object> row=new LinkedHashMap<>();for(int n=0;n<values.length;n+=2)row.put((String)values[n],values[n+1]);return row;}
    static final class Memory {
        Map<Integer,String> saved=new LinkedHashMap<>();int commits,rollbacks;boolean failCommit;
        Memory(){JsonArray items=new JsonArray();items.add(JsonNull.INSTANCE);items.add(item(true));items.add(false);items.add("legacy");items.add(new JsonArray());items.add(JsonNull.INSTANCE);items.add(item(false));saved.put(30,items.toString());JsonObject skill=item(true);skill.addProperty("cost",15625000000L);saved.put(32,"["+skill+"]");saved.put(64,"[]");}
        Connection connection(){Map<Integer,String> working=new LinkedHashMap<>(saved);
            return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
                switch(m.getName()){
                    case "close","setAutoCommit"->{return null;}
                    case "commit"->{if(failCommit)throw new SQLException("Fixture commit failure");saved=new LinkedHashMap<>(working);commits++;return null;}
                    case "rollback"->{working.clear();working.putAll(saved);rollbacks++;return null;}
                    case "prepareStatement"->{String sql=((String)a[0]).toLowerCase(Locale.ROOT);Map<Integer,Object> params=new HashMap<>();return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(s,sm,sa)->{
                        switch(sm.getName()){
                            case "close"->{return null;}
                            case "setObject","setInt"->{params.put((Integer)sa[0],sa[1]);return null;}
                            case "executeUpdate"->{check(sql.equals("update tab_shop set items=? where id=?"),"Only selected tab.items is updated");int id=((Number)params.get(2)).intValue();if(!working.containsKey(id))return 0;working.put(id,(String)params.get(1));return 1;}
                            case "executeQuery"->{List<Map<String,Object>> rows=new ArrayList<>();
                                if(sql.contains("from npc_template")){rows.add(row("id",1,"name","Old NPC"));rows.add(row("id",13,"name","Skill NPC"));rows.add(row("id",74,"name","Tori-bot"));}
                                else if(sql.contains("from tab_shop")){
                                    for(int id:working.keySet()){int shop=id==30?30:id==32?38:49;if(sql.contains("where id=?")&&id!=((Number)params.get(1)).intValue())continue;if(sql.contains("where shop_id = ?")&&shop!=((Number)params.get(1)).intValue())continue;rows.add(row("id",id,"shop_id",shop,"tab_name","Vật phẩm","tab_index",0,"items",working.get(id)));}
                                }else if(sql.contains("from shop")){
                                    for(int id:new int[]{30,38,49}){if(sql.contains("where id=?")&&id!=((Number)params.get(1)).intValue())continue;rows.add(row("id",id,"npc_id",id==30?1:id==38?13:74,"tag_name",id==30?"OLD":id==38?"SKILLS":"TORI_BOT_ITEMS","type_shop",id==38?1:0));}
                                }else throw new AssertionError("Unexpected query "+sql);
                                return result(rows);
                            }
                            default->throw new AssertionError("Unexpected statement method "+sm.getName());
                        }
                    });}
                    default->throw new AssertionError("Unexpected connection method "+m.getName());
                }
            });
        }
        ResultSet result(List<Map<String,Object>> rows){int[] index={-1};return(ResultSet)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ResultSet.class},(p,m,a)->{
            if(m.getName().equals("next"))return ++index[0]<rows.size();if(m.getName().equals("close"))return null;
            Map<String,Object> row=rows.get(index[0]);Object value=a[0]instanceof Integer n?new ArrayList<>(row.values()).get(n-1):row.get(a[0]);
            return switch(m.getName()){case "getInt"->((Number)value).intValue();case "getByte"->((Number)value).byteValue();case "getString"->value==null?null:value.toString();default->throw new AssertionError("Unexpected result method "+m.getName());};
        });}
    }
}
