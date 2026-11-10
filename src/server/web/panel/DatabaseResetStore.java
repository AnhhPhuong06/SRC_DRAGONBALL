package server.web.panel;

import database.AlyraManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import static server.web.panel.PanelSupport.*;

/** Exact account-database scope, transactional DELETE; no DDL or disabled FK checks. */
final class DatabaseResetStore {
    static final List<String> SCOPE=List.of("player_gift_data","player","clan","account");
    record Table(String name,long rows,String engine) {}
    record Snapshot(String database,List<Table> tables,List<String> deleteOrder,List<String> blockers) {
        Snapshot {tables=List.copyOf(tables);deleteOrder=List.copyOf(deleteOrder);blockers=List.copyOf(blockers);}
    }
    interface Store {Snapshot preview() throws Exception;Map<String,Long> erase(Snapshot expected) throws Exception;}
    static final class UncertainCommit extends SQLException {UncertainCommit(Exception cause){super("Không xác định được trạng thái commit/rollback. Đã khóa ghi dữ liệu; hãy kiểm tra database và khởi động lại server.",cause);}}
    static final Store LIVE=new Store(){
        public Snapshot preview()throws Exception{try(Connection c=AlyraManager.getConnection()){c.setReadOnly(true);c.setAutoCommit(false);try{return inspect(c);}finally{c.rollback();}}}
        public Map<String,Long> erase(Snapshot expected)throws Exception{
            boolean committed=false;
            try(Connection c=AlyraManager.getConnectionForReset()){var result=delete(c,expected);committed=true;return result;}
            catch(Exception e){if(committed)throw new UncertainCommit(e);throw e;}
        }
    };
    private DatabaseResetStore() {}
    static String revision(Snapshot s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(GSON.toJson(s).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    static Snapshot inspect(Connection c)throws SQLException {
        String database=c.getCatalog();if(database==null||database.isBlank())throw new SQLException("Chưa xác định database tài khoản.");
        var engines=new HashMap<String,String>();
        try(var ps=c.prepareStatement("SELECT TABLE_NAME,ENGINE FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('account','player','clan','player_gift_data')");var rs=ps.executeQuery()){
            while(rs.next())engines.put(rs.getString(1),Objects.toString(rs.getString(2),""));
        }
        var tables=new ArrayList<Table>();var blockers=new ArrayList<String>();
        for(String name:SCOPE) {
            if(!engines.containsKey(name)){if(!name.equals("player_gift_data"))blockers.add("Thiếu bảng "+name+".");continue;}
            String engine=engines.get(name);if(!engine.equalsIgnoreCase("InnoDB"))blockers.add("Bảng "+name+" cần dùng InnoDB để reset trong một giao dịch.");
            tables.add(new Table(name,count(c,name),engine));
        }
        var names=new LinkedHashSet<>(tables.stream().map(Table::name).toList());var edges=new ArrayList<String[]>();
        try(var ps=c.prepareStatement("SELECT TABLE_SCHEMA,TABLE_NAME,REFERENCED_TABLE_NAME FROM information_schema.key_column_usage WHERE REFERENCED_TABLE_SCHEMA=DATABASE() AND REFERENCED_TABLE_NAME IN ('account','player','clan','player_gift_data')");var rs=ps.executeQuery()){
            while(rs.next()) {
                String schema=rs.getString(1),child=rs.getString(2),parent=rs.getString(3);
                if(!database.equals(schema)||!names.contains(child))blockers.add("Bảng ngoài phạm vi "+schema+"."+child+" đang tham chiếu "+parent+"; chưa thể reset.");
                else if(!child.equals(parent))edges.add(new String[]{child,parent});
            }
        }
        try(var ps=c.prepareStatement("SELECT TRIGGER_NAME,EVENT_OBJECT_TABLE FROM information_schema.triggers WHERE TRIGGER_SCHEMA=DATABASE() AND EVENT_MANIPULATION='DELETE' AND EVENT_OBJECT_TABLE IN ('account','player','clan','player_gift_data')");var rs=ps.executeQuery()){
            while(rs.next())blockers.add("Bảng "+rs.getString(2)+" có trigger DELETE "+rs.getString(1)+"; cần kiểm tra phạm vi trước khi reset.");
        }
        var remaining=new LinkedHashSet<>(names);var order=new ArrayList<String>();
        while(!remaining.isEmpty()) {
            String ready=remaining.stream().filter(name->edges.stream().noneMatch(edge->edge[1].equals(name)&&remaining.contains(edge[0]))).findFirst().orElse(null);
            if(ready==null){blockers.add("Quan hệ khóa ngoại tạo vòng lặp giữa các bảng reset.");break;}
            order.add(ready);remaining.remove(ready);
        }
        return new Snapshot(database,tables,order,blockers);
    }
    private static long count(Connection c,String name)throws SQLException {
        if(!SCOPE.contains(name))throw new SQLException("Bảng ngoài phạm vi reset.");
        try(var ps=c.prepareStatement("SELECT COUNT(*) FROM `"+name+"`");var rs=ps.executeQuery()){if(!rs.next())throw new SQLException("Không đọc được số dòng.");return rs.getLong(1);}
    }
    static Map<String,Long> delete(Connection c,Snapshot expected)throws Exception {
        boolean committing=false;c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
        try {
            Snapshot current=inspect(c);
            if(!current.blockers().isEmpty())throw error(409,String.join(" ",current.blockers()));
            if(!revision(current).equals(revision(expected)))throw error(409,"Database hoặc số dòng đã thay đổi. Tải lại để xác nhận phạm vi mới.");
            var deleted=new LinkedHashMap<String,Long>();
            for(String name:current.deleteOrder()) {
                if(!SCOPE.contains(name))throw new SQLException("Bảng ngoài phạm vi reset.");
                try(var ps=c.prepareStatement("DELETE FROM `"+name+"`")){ps.setQueryTimeout(60);deleted.put(name,(long)ps.executeUpdate());}
            }
            for(var table:current.tables())if(count(c,table.name())!=0)throw new SQLException("Bảng "+table.name()+" chưa rỗng; hủy reset.");
            committing=true;c.commit();return deleted;
        }catch(Exception failure){
            try{c.rollback();}catch(Exception rollback){failure.addSuppressed(rollback);throw new UncertainCommit(failure);}
            if(committing)throw new UncertainCommit(failure);
            throw failure;
        }
    }
}
