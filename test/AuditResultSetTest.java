import database.ResultSetImpl;
import java.sql.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;

/** Exercise the actual disconnected adapter, no DB, over JDBC numeric wrappers and cursors. */
public final class AuditResultSetTest {
    static int checks;
    static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    static final class Rows {
        int cursor=-1;final Object[] values;boolean closed;
        Rows(Object... values){this.values=values;}
        ResultSet result(){
            ResultSetMetaData meta=(ResultSetMetaData)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ResultSetMetaData.class},(p,m,a)->switch(m.getName()){
                case "getColumnCount"->1;case "getColumnName"->"value";case "getTableName"->"fixture";default->null;
            });
            return (ResultSet)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ResultSet.class},(p,m,a)->switch(m.getName()){
                case "last"->{cursor=values.length-1;yield values.length>0;}
                case "getRow"->cursor+1;
                case "beforeFirst"->{cursor=-1;yield null;}
                case "getMetaData"->meta;
                case "next"->++cursor<values.length;
                case "getObject"->values[cursor];
                case "getStatement"->null;
                case "close"->{closed=true;yield null;}
                default->null;
            });
        }
    }
    public static void main(String[] args)throws Exception{
        Rows rows=new Rows(7,8);var rs=new ResultSetImpl(rows.result());
        check(rows.closed,"ResultSet closes even when getStatement returns null");
        check(rs.first()&&rs.getInt("value")==7,"first selects first of multiple rows");
        check(rs.first()&&rs.getInt("value")==7,"Repeated first resets instead of advancing");
        rs.gotoBeforeFirst();check(rs.gotoResult(1)&&rs.getInt("value")==8,"gotoResult validates requested index, not old cursor");
        for(int invalid:new int[]{-1,2,Integer.MAX_VALUE}){
            try{rs.gotoResult(invalid);throw new AssertionError("Accepted invalid cursor");}catch(Exception expected){checks++;}
        }
        rs.dispose();rs.dispose();check(true,"Dispose idempotent");
        var empty=new ResultSetImpl(new Rows().result());check(!empty.first()&&!empty.gotoLast(),"Empty cursor returns false");
        for(Number number:new Number[]{(byte)7,(short)7,7,7L,7f,7d,new BigDecimal("7")}){
            rs=new ResultSetImpl(new Rows(number).result());rs.first();
            check(rs.getInt(1)==7&&rs.getInt("value")==7,"Integer getters accept JDBC Number wrapper");
            check(rs.getLong(1)==7&&rs.getLong("value")==7,"Long getters accept JDBC Number wrapper");
            check(rs.getByte(1)==7&&rs.getShort(1)==7&&rs.getFloat(1)==7&&rs.getDouble(1)==7,"Numeric conversion parity");
        }
        rs=new ResultSetImpl(new Rows((Object)null).result());rs.first();check(rs.getInt(1)==0,"SQL NULL numeric zero");
        System.out.println("PASS: "+checks+" result adapter cursor, close, null and numeric wrapper regressions");
    }
}
