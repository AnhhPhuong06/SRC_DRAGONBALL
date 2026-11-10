import database.AccountWallet;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;

/** JDBC double verifies real store SQL/parameters, error propagation and resource closure. */
public final class AuditWalletTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static final class Database {
        String sql; Map<Integer,Object> parameters=new HashMap<>(); int rows=1; boolean fail,closed;
        Connection connection(){return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
            if(m.getName().equals("prepareStatement")){
                sql=(String)a[0];parameters.clear();closed=false;
                return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class[]{PreparedStatement.class},(x,n,v)->{
                    if(n.getName().startsWith("set")){parameters.put((Integer)v[0],v[1]);return null;}
                    if(n.getName().equals("executeUpdate")){if(fail)throw new SQLException("fixture failure");return rows;}
                    if(n.getName().equals("close")){closed=true;return null;}
                    return null;
                });
            }return null;
        });}
    }
    public static void main(String[] args)throws Exception{
        for(String currency:List.of("cash","sotien"))for(int amount:new int[]{1,50000,Integer.MAX_VALUE}){
            Database db=new Database();check(AccountWallet.debit(db.connection(),currency,7,amount,false),"Successful debit");
            check(db.sql.endsWith("WHERE id=? AND "+currency+">=?"),"Database enforces current balance");
            check(db.parameters.equals(Map.of(1,amount,2,7,3,amount))&&db.closed,"Exact debit binding and closure");
            db.rows=0;check(!AccountWallet.debit(db.connection(),currency,7,amount,false),"Missing row/insufficient balance fails closed");
            db.fail=true;try{AccountWallet.debit(db.connection(),currency,7,amount,false);throw new AssertionError("Accepted failed update");}
            catch(SQLException expected){check(db.closed,"SQL failure still closes statement");}
        }
        check(!AccountWallet.debit(null,"sotien",7,-1,false),"Negative debit rejected before JDBC");
        check(!AccountWallet.debit(null,"sotien",7,0,false),"Ordinary zero debit rejected");
        check(!AccountWallet.debit(null,"sotien",0,1,false),"Invalid account rejected");
        check(!AccountWallet.debit(null,"other",7,1,false),"SQL column allowlist");
        Database membership=new Database();check(AccountWallet.debit(membership.connection(),"sotien",7,0,true),"Existing free activation retained");
        check(membership.sql.contains("active=1"),"Activation belongs to successful update");
        for(int tier=1;tier<=3;tier++){
            Database db=new Database();check(AccountWallet.purchaseVip(db.connection(),7,tier,123456789L),"VIP payment success");
            int cost=tier==1?50000:tier==2?200000:500000;
            String suffix=tier==1?"":Integer.toString(tier-1);
            check(db.sql.contains("sotien=sotien-?")&&db.sql.contains("vip"+suffix+"=1")
                    &&db.sql.endsWith("AND sotien>=? AND COALESCE(hasReceivedVIP"+suffix+",0)=0"),"Payment and replay guard in one update");
            check(db.parameters.equals(Map.of(1,cost,2,123456789L,3,7,4,cost))&&db.closed,"VIP exact tier, timestamp bigint and price");
            db.rows=0;check(!AccountWallet.purchaseVip(db.connection(),7,tier,123L),"Already claimed VIP/insufficient balance grants nothing");
        }
        check(!AccountWallet.purchaseVip(null,7,0,0)&&!AccountWallet.purchaseVip(null,7,4,0),"Invalid tiers rejected before JDBC");
        System.out.println("PASS: "+checks+" wallet JDBC-double regressions (not a MySQL integration test)");
    }
}
