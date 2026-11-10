package database;

import java.lang.reflect.*;
import java.sql.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Account connections and complete logins drain before reset; restart releases the completed latch. */
public final class DatabaseResetGuard {
    public static final DatabaseResetGuard GLOBAL = new DatabaseResetGuard();
    public static final String RESTART_MESSAGE = "Dữ liệu đã được reset. Cần khởi động lại server trước khi đăng nhập hoặc ghi dữ liệu.";
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private volatile boolean completed;

    public boolean completed(){return completed;}
    public Lease read() throws SQLException {
        lock.readLock().lock();
        if(completed){lock.readLock().unlock();throw new SQLException(RESTART_MESSAGE,"55000");}
        return new Lease(false);
    }
    public Lease exclusive(long timeoutMillis) throws SQLException {
        if(lock.getReadHoldCount()>0)throw new SQLException("Không reset từ bên trong giao dịch đang mở.");
        try{if(!lock.writeLock().tryLock(timeoutMillis,TimeUnit.MILLISECONDS))throw new SQLException("Đang có đăng nhập/giao dịch chưa hoàn tất. Chờ rồi tải lại trang reset.","40001");}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new SQLException("Đã dừng chờ reset.",e);}
        if(completed){lock.writeLock().unlock();throw new SQLException(RESTART_MESSAGE,"55000");}
        return new Lease(true);
    }
    public void requireExclusive() throws SQLException {
        if(!lock.isWriteLockedByCurrentThread()||completed)throw new SQLException("Chưa khóa dữ liệu để reset.");
    }
    @FunctionalInterface public interface ConnectionFactory {Connection open() throws SQLException;}
    public Connection connection(ConnectionFactory factory) throws SQLException {
        Lease lease=read();
        try {
            Connection real=factory.open();
            return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(proxy,method,args)->{
                boolean closing=method.getName().equals("close")||method.getName().equals("abort");
                try{return method.invoke(real,args);}
                catch(InvocationTargetException e){throw e.getCause();}
                finally{if(closing)lease.close();}
            });
        }catch(SQLException|RuntimeException|Error e){lease.close();throw e;}
    }
    public final class Lease implements AutoCloseable {
        private final boolean exclusive;private final AtomicBoolean closed=new AtomicBoolean();
        private Lease(boolean exclusive){this.exclusive=exclusive;}
        public void complete() throws SQLException {
            if(!exclusive||closed.get())throw new SQLException("Không có phiên reset hợp lệ.");
            requireExclusive();completed=true;
        }
        @Override public void close(){if(closed.compareAndSet(false,true)){if(exclusive)lock.writeLock().unlock();else lock.readLock().unlock();}}
    }
}
