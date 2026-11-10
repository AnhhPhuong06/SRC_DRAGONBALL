package server.web.panel;

import com.google.gson.JsonObject;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/** Synthetic JDBC only: never connects to or changes an actual account. */
public final class RevenueCashWalletTest {
    static int checks;
    public static void main(String[] args) throws Exception {
        Wallet w=new Wallet(1000,5_000_000);
        change(w,500,true,1500);change(w,1500,false,0);
        w=new Wallet(1000,0);change(w,500,false,500);
        w=new Wallet(50,5_000_000);reject(w,51,false);
        w=new Wallet(0,5_000_000);reject(w,1,false);
        w=new Wallet(Integer.MAX_VALUE,0);reject(w,1,true);
        w=new Wallet(0,17);change(w,Integer.MAX_VALUE,true,Integer.MAX_VALUE);
        change(w,Integer.MAX_VALUE,false,0);
        w=new Wallet(1000,17);w.changed=0;
        try {RevenueDataApi.adjustMoney(w.connection(),w.account,1,true);throw new AssertionError("Missing row accepted");}
        catch(PanelSupport.PanelException e){check(e.status==409,"Stale update rejected");}
        check(w.sotien()==1000,"Missing row does not alter wallet");
        w=new Wallet(1000,17);w.fail=true;
        try {RevenueDataApi.adjustMoney(w.connection(),w.account,1,true);throw new AssertionError("Failed write accepted");}
        catch(SQLException expected){check(w.sotien()==1000,"Write failure propagates without synthetic mutation");}

        Method money=RevenueDataApi.class.getDeclaredMethod("money",JsonObject.class,boolean.class);money.setAccessible(true);
        for(String amount:new String[]{"0","-1","2147483648","1.5","invalid"}) {
            JsonObject b=body();b.addProperty("amount",amount);badRequest(money,b);
        }
        JsonObject b=body();b.remove("amount");badRequest(money,b);
        b=body();b.addProperty("username","x");badRequest(money,b);
        b=body();b.addProperty("field","cash");badRequest(money,b);
        System.out.println("PASS: "+checks+" CASH-to-sotien add/subtract, bounds, SQL binding, unchanged cash/danap and request validation checks; no live DB access");
    }
    static JsonObject body(){var b=new JsonObject();b.addProperty("username","fixture-player");b.addProperty("amount",500);return b;}
    static void badRequest(Method method,JsonObject b)throws Exception {
        try {method.invoke(null,b,true);throw new AssertionError("Bad request accepted");}
        catch(InvocationTargetException e){check(e.getCause() instanceof PanelSupport.PanelException&&((PanelSupport.PanelException)e.getCause()).status==400,"Invalid request rejected before opening database connection");}
    }
    static void change(Wallet w,long amount,boolean add,long expected)throws Exception {
        long cash=w.number("cash"),danap=w.number("danap");int writes=w.writes;
        Object result=RevenueDataApi.adjustMoney(w.connection(),w.account,amount,add);
        check(w.sotien()==expected,"Read and update game balance sotien");
        check(w.number("cash")==cash&&w.number("danap")==danap,"Preserve separate cash and total-topup columns");
        check(w.writes==writes+1,"Exactly one prepared wallet update");
        check(Boolean.TRUE.equals(((Map<?,?>)result).get("success")),"Successful response returned");
    }
    static void reject(Wallet w,long amount,boolean add)throws Exception {
        Map<String,Object> before=new LinkedHashMap<>(w.account);
        try {RevenueDataApi.adjustMoney(w.connection(),w.account,amount,add);throw new AssertionError("Invalid balance accepted");}
        catch(PanelSupport.PanelException e){check(e.status==409,"Insufficient funds/overflow rejected");}
        check(w.account.equals(before)&&w.writes==0,"Rejected change does not issue a write");
    }
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static final class Wallet {
        final Map<String,Object> account=new LinkedHashMap<>();int writes,changed=1;boolean fail;
        Wallet(long balance,long cash){account.putAll(Map.of("id",7,"username","fixture-player","sotien",balance,"cash",cash,"danap",123456));}
        long number(String key){return ((Number)account.get(key)).longValue();}
        long sotien(){return number("sotien");}
        Connection connection(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
            if(m.getName().equals("prepareStatement")) {
                check(a[0].equals("UPDATE account SET sotien=? WHERE id=?"),"SQL targets only sotien, never cash/danap");
                Map<Integer,Object> params=new HashMap<>();
                return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(s,sm,sa)->{
                    if(sm.getName().equals("setObject")){params.put((Integer)sa[0],sa[1]);return null;}
                    if(sm.getName().equals("executeUpdate")) {
                        check(params.size()==2&&((Number)params.get(2)).intValue()==7,"Bind amount and locked account ID separately");
                        if(fail)throw new SQLException("Fixture write failure");
                        writes++;if(changed==1)account.put("sotien",params.get(1));return changed;
                    }
                    if(sm.getName().equals("close"))return null;
                    throw new AssertionError("Unexpected statement operation "+sm.getName());
                });
            }
            if(m.getName().equals("close"))return null;
            throw new AssertionError("Unexpected connection operation "+m.getName());
        });}
    }
}
