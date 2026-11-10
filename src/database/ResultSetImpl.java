package database;

import java.sql.Timestamp;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.sql.ResultSet;
import java.util.Map;

public class ResultSetImpl implements AlyraResultSet
{
    private Map<String, Object>[] data;
    private Object[][] values;
    private int indexData;
    
    public ResultSetImpl(final ResultSet rs) throws Exception {
        this.indexData = -1;
        try {
            rs.last();
            final int nRow = rs.getRow();
            rs.beforeFirst();
            final ResultSetMetaData rsmd = rs.getMetaData();
            final int nColumn = rsmd.getColumnCount();
            this.data = new HashMap[nRow];
            for (int i = 0; i < this.data.length; ++i) {
                this.data[i] = new HashMap<>();
            }
            this.values = new Object[nRow][nColumn];
            int index = 0;
            while (rs.next()) {
                for (int j = 1; j <= nColumn; ++j) {
                    final String tableName = rsmd.getTableName(j);
                    final String columnName = rsmd.getColumnName(j);
                    final Object columnValue = rs.getObject(j);
                    this.data[index].put(columnName.toLowerCase(), columnValue);
                    this.data[index].put(tableName.toLowerCase() + "." + columnName.toLowerCase(), columnValue);
                    this.values[index][j - 1] = columnValue;
                }
                ++index;
            }
        }
        catch (final Exception e) {
            throw e;
        }
        finally {
            if (rs != null) {
                java.sql.Statement statement = null;
                try { statement = rs.getStatement(); } catch (Exception ignored) {}
                try { rs.close(); } catch (Exception ignored) {}
                try {
                    if (statement != null) statement.close();
                }
                catch (final Exception ex) {}
            }
        }
    }
    
    @Override
    public void dispose() {
        if (this.data == null && this.values == null) return;
        for (Map map : this.data) {
            map.clear();
            map = null;
        }
        this.data = null;
        for (final Object[] array : this.values) {
            Object[] obj = array;
            for (Object o : array) {
                o = null;
            }
            obj = null;
        }
        this.values = null;
    }
    
    @Override
    public boolean next() throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        ++this.indexData;
        return this.indexData < this.data.length;
    }
    
    @Override
    public boolean first() throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        this.indexData = this.data.length == 0 ? -1 : 0;
        return this.data.length > 0;
    }
    
    @Override
    public boolean gotoResult(final int index) throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        if (index < 0 || index >= this.data.length) {
            throw new Exception("Index out of bound");
        }
        this.indexData = index;
        return true;
    }
    
    @Override
    public boolean gotoFirst() throws Exception {
        if (this.data == null || this.data.length == 0) {
            throw new Exception("No data available");
        }
        this.indexData = 0;
        return true;
    }
    
    @Override
    public void gotoBeforeFirst() {
        this.indexData = -1;
    }
    
    @Override
    public boolean gotoLast() throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        this.indexData = this.data.length - 1;
        return this.data.length > 0;
    }
    
    @Override
    public int getRows() throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        return this.data.length;
    }
    
    private static Number number(Object value) {
        return value == null ? 0 : (Number)value;
    }

    @Override
    public byte getByte(final int column) throws Exception {
        if (this.values == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return number(this.values[this.indexData][column - 1]).byteValue();
    }
    
    @Override
    public byte getByte(final String column) throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return number(this.data[this.indexData].get(column.toLowerCase())).byteValue();
    }
    
    @Override
    public int getInt(final int column) throws Exception {
        if (this.values == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return number(this.values[this.indexData][column - 1]).intValue();
    }
    
    @Override
    public int getInt(final String column) throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return number(this.data[this.indexData].get(column.toLowerCase())).intValue();
    }
    
    @Override
    public float getFloat(final int column) throws Exception {
        if (this.values == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return number(this.values[this.indexData][column - 1]).floatValue();
    }
    
    @Override
    public float getFloat(final String column) throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return number(this.data[this.indexData].get(column.toLowerCase())).floatValue();
    }
    
    @Override
    public double getDouble(final int column) throws Exception {
        if (this.values == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return number(this.values[this.indexData][column - 1]).doubleValue();
    }
    
    @Override
    public double getDouble(final String column) throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return number(this.data[this.indexData].get(column.toLowerCase())).doubleValue();
    }
    
    @Override
    public long getLong(final int column) throws Exception {
        if (this.values == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return number(this.values[this.indexData][column - 1]).longValue();
    }
    
    @Override
    public long getLong(final String column) throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return number(this.data[this.indexData].get(column.toLowerCase())).longValue();
    }
    
    @Override
    public String getString(final int column) throws Exception {
        if (this.values == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return String.valueOf(this.values[this.indexData][column - 1]);
    }
    
    @Override
    public String getString(final String column) throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return String.valueOf(this.data[this.indexData].get(column.toLowerCase()));
    }
    
    @Override
    public Object getObject(final int column) throws Exception {
        if (this.values == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return this.values[this.indexData][column - 1];
    }
    
    @Override
    public Object getObject(final String column) throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return this.data[this.indexData].get(column.toLowerCase());
    }
    
    @Override
    public boolean getBoolean(final int column) throws Exception {
        if (this.values == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        Object value = this.values[this.indexData][column - 1];
        return value instanceof Number n ? n.intValue() == 1 : Boolean.TRUE.equals(value);
    }
    
    @Override
    public boolean getBoolean(final String column) throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        Object value = this.data[this.indexData].get(column.toLowerCase());
        return value instanceof Number n ? n.intValue() == 1 : Boolean.TRUE.equals(value);
    }
    
    @Override
    public Timestamp getTimestamp(final int column) throws Exception {
        if (this.values == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return (Timestamp)this.values[this.indexData][column - 1];
    }
    
    @Override
    public Timestamp getTimestamp(final String column) throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return (Timestamp) this.data[this.indexData].get(column.toLowerCase());
    }
    
    @Override
    public short getShort(final int column) throws Exception {
        if (this.values == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return number(this.values[this.indexData][column - 1]).shortValue();
    }
    
    @Override
    public short getShort(final String column) throws Exception {
        if (this.data == null) {
            throw new Exception("No data available");
        }
        if (this.indexData == -1) {
            throw new Exception("Results need to be prepared in advance");
        }
        return number(this.data[this.indexData].get(column.toLowerCase())).shortValue();
    }
}
