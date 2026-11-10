import database.ConsignStore;
import models.ConsignItem;
import java.sql.*;
import java.lang.reflect.Proxy;
import java.util.*;

/** Transaction protocol against a JDBC double, including insert and rollback failures. */
public final class AuditConsignStoreTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static final class Database {
        final List<String> calls=new ArrayList<>(); boolean failBatch,failRollback;int prepared,closed;
        Connection connection(){return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
            switch(m.getName()){
                case "getAutoCommit":return true;
                case "setAutoCommit":calls.add("auto="+a[0]);return null;
                case "commit":calls.add("commit");return null;
                case "rollback":calls.add("rollback");if(failRollback)throw new SQLException("fixture rollback");return null;
                case "prepareStatement":{
                    String sql=(String)a[0];prepared++;calls.add(sql);
                    return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class[]{PreparedStatement.class},(q,n,v)->{
                        switch(n.getName()){
                            case "executeUpdate":return 1;
                            case "executeBatch":calls.add("batch");if(failBatch)throw new SQLException("fixture insert");return new int[]{1};
                            case "close":closed++;return null;
                            default:return null;
                        }
                    });
                }
                default:return null;
            }
        });}
    }
    public static void main(String[] args)throws Exception{
        var item=new ConsignItem(1,(short)533,7,(byte)3,100,-1,1,123,new ArrayList<>(),false);
        for(int scenario=0;scenario<3;scenario++){
            Database db=new Database();db.failBatch=scenario>0;db.failRollback=scenario==2;
            try{ConsignStore.save(db.connection(),List.of(item));check(scenario==0,"Only success can return normally");}
            catch(SQLException expected){check(scenario>0,"Failure propagated");if(scenario==2)check(expected.getSuppressed().length==1,"Rollback failure retained");}
            check(db.calls.get(0).equals("auto=false")&&db.calls.get(1).equals("DELETE FROM shop_ky_gui"),"DELETE inside transaction, never TRUNCATE");
            check(db.calls.get(2).contains("VALUES (?,?,?,?,?,?,?,?,?,?)"),"Prepared insert binds all values");
            check(db.prepared==2&&db.closed==2,"Statements closed on success/failure");
            check(db.calls.contains(scenario==0?"commit":"rollback"),"Correct transaction end");
            check(db.calls.contains("auto=true")==!db.failRollback,"Never enable auto-commit after failed rollback");
        }
        System.out.println("PASS: "+checks+" consign JDBC-double transaction/rollback regressions");
    }
}
