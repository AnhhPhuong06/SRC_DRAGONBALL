package server.web.panel;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import javax.sql.rowset.*;
import models.Template.*;
import server.Manager;
import services.top.*;
import sun.misc.Unsafe;

/** Executes the real JDBC save path using a private transactional JDBC fixture. */
public final class TopJdbcStoreTest {
    static int checks;
    public static void main(String[] args)throws Exception {
        FakeDb db=new FakeDb();var store=new TopPanelData.JdbcStore(db::connection,db::connection);
        var before=db.row;String version=TopPanelData.version(before);
        var body=TopPanelDataTest.payload(before);body.addProperty("name","Changed top");
        boolean runtime=store.edit(0,version,row->TopPanelData.change(row,body,"save",TopPanelDataTest.CATALOG,TopPanelDataTest.NOW));
        check(!runtime&&db.commits==1&&db.row.name().equals("Changed top"),"Actual JDBC edit commits configuration once");
        check(db.row.query().equals(before.query())&&db.row.users().equals(before.users()),"SQL preserves query and recipient progress");
        check(db.writes.get(0).contains("name=?")&&!db.writes.get(0).contains("users=")&&!db.writes.get(0).contains("query="),"Only whitelisted columns use placeholders");
        try{store.edit(0,version,row->row);throw new AssertionError("Expected stale rejection");}catch(PanelSupport.PanelException e){check(e.status==409,"Stale row rejected under SELECT FOR UPDATE");}
        check(db.writes.size()==1,"Stale edit makes no update");
        var original=db.row;db.failWrite=true;
        try{store.edit(0,TopPanelData.version(original),row->row);throw new AssertionError("Expected failed write");}catch(SQLException expected){checks++;}
        check(db.row.equals(original)&&db.rollbacks>=2,"Write failure rolls back without publishing state");db.failWrite=false;

        // A real TOPAUTO constructor must accept the exact JSON written by the panel.
        while(Manager.ITEM_TEMPLATES.size()<=674)Manager.ITEM_TEMPLATES.add(null);
        for(int id:new int[]{568,674}){var t=new ItemTemplate();t.id=(short)id;t.type=27;t.name="Fixture item "+id;Manager.ITEM_TEMPLATES.set(id,t);}
        for(int id=0;id<252;id++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"Fixture #",0));
        Field uf=Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);Unsafe unsafe=(Unsafe)uf.get(null);
        TOPAUTO top=(TOPAUTO)unsafe.allocateInstance(TOPAUTO.class);top.template=new TopManager.TopTemplate(0,original.name(),original.query());
        top.date=Timestamp.valueOf("2029-12-28 23:59:59");top.items=new HashMap<>();top.users=new LinkedHashMap<>();top.limit=2;
        TopManager.AUTO_GIFT_TOPS.add(top);TopManager.TOP_TEMPLATE.add(top.template);
        Field tf=TopAutoService.class.getDeclaredField("timers");tf.setAccessible(true);
        try {
            db.failCommit=true;
            try{store.edit(0,TopPanelData.version(db.row),row->row);throw new AssertionError("Expected commit failure");}catch(SQLException expected){checks++;}
            check(db.row.equals(original)&&top.items.isEmpty(),"Commit failure publishes neither DB nor staged runtime rewards");db.failCommit=false;
            body.addProperty("name","Live top");body.addProperty("version",TopPanelData.version(db.row));
            check(store.edit(0,TopPanelData.version(db.row),row->TopPanelData.change(row,body,"save",TopPanelDataTest.CATALOG,TopPanelDataTest.NOW)),"Active campaign applies without restarting");
            check(top.template.name.equals("Live top")&&top.items.get(1).get(0).template.id==568&&top.items.get(1).get(0).quantity==10
                    &&top.items.get(1).get(0).getOptionParam(50)==20,"Actual TOPAUTO receives exact configured item, count and options");
            check(top.users.isEmpty()&&TopManager.SELECT[0].contains("Live top"),"Progress preserved and NPC top menu refreshed");
            check(((List<?>)tf.get(TopAutoService.gI())).size()==1,"Edited campaign has one rescheduled timer");
        } finally {for(var timer:(List<java.util.Timer>)tf.get(TopAutoService.gI()))timer.cancel();TopManager.AUTO_GIFT_TOPS.remove(top);TopManager.TOP_TEMPLATE.remove(top.template);}
        System.out.println("PASS: "+checks+" real JDBC save, optimistic lock, commit/rollback, TOPAUTO compatibility and live configuration checks with private fixtures");
    }
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static final class FakeDb {
        TopPanelData.Row row=TopPanelDataTest.row(0,false,true,"[]"),pending;int commits,rollbacks;boolean failWrite,failCommit;List<String>writes=new ArrayList<>();
        Connection connection(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->switch(m.getName()){
            case "prepareStatement"->statement((String)a[0]);case "commit"->{if(failCommit)throw new SQLException("Fixture commit failure");if(pending!=null)row=pending;pending=null;commits++;yield null;}
            case "rollback"->{pending=null;rollbacks++;yield null;}case "close","setAutoCommit"->null;case "isClosed"->false;case "getAutoCommit"->false;default->throw new AssertionError("Unexpected JDBC call "+m.getName());});}
        PreparedStatement statement(String sql){Map<Integer,Object> values=new HashMap<>();return (PreparedStatement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(p,m,a)->{
            if(Set.of("setInt","setString","setTimestamp").contains(m.getName())){values.put((Integer)a[0],a[1]);return null;}
            return switch(m.getName()){
                case "executeQuery"->result(sql.contains("FOR UPDATE")||pending==null?row:pending);
                case "executeUpdate"->{writes.add(sql);if(failWrite)throw new SQLException("Fixture write failure");pending=new TopPanelData.Row(row.id(),(String)values.get(1),row.query(),((Timestamp)values.get(2)).toLocalDateTime().format(TopPanelData.DATE),(String)values.get(3),row.users(),Boolean.parseBoolean((String)values.get(4)),Boolean.parseBoolean((String)values.get(5)),(Integer)values.get(6));yield 1;}
                case "close"->null;default->throw new AssertionError("Unexpected SQL call "+m.getName());};});}
        ResultSet result(TopPanelData.Row row)throws Exception{
            var result=RowSetProvider.newFactory().createCachedRowSet();var meta=new RowSetMetaDataImpl();meta.setColumnCount(9);
            String[] names={"id","name","query","date","items","users","isDone","isAuto","limit"};
            for(int i=0;i<9;i++){meta.setColumnName(i+1,names[i]);meta.setColumnType(i+1,i==0||i==8?Types.INTEGER:i==3?Types.TIMESTAMP:i==6||i==7?Types.BOOLEAN:Types.VARCHAR);}
            result.setMetaData(meta);result.moveToInsertRow();result.updateInt(1,row.id());result.updateString(2,row.name());result.updateString(3,row.query());
            result.updateTimestamp(4,Timestamp.valueOf(TopPanelData.date(row.date())));result.updateString(5,row.items());result.updateString(6,row.users());
            result.updateBoolean(7,row.done());result.updateBoolean(8,row.auto());result.updateInt(9,row.limit());result.insertRow();result.moveToCurrentRow();result.beforeFirst();return result;
        }
    }
}
