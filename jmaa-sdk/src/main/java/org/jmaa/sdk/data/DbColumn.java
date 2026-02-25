package org.jmaa.sdk.data;

/**
 * 数据库字段信息
 *
 * @author Eric Liang
 */
public class DbColumn {
    String column;
    String type;
    Integer length;
    boolean nullable;

    public DbColumn(String column, String type, Integer length, boolean nullable) {
        this.column = column;
        this.type = type;
        this.length = length;
        this.nullable = nullable;
    }

    public String getColumn() {
        return column;
    }

    public String getType() {
        return type;
    }

    public String getDbType() {
        if (length != null && length > 0) {
            return type + "(" + length + ")";
        }
        return type;
    }

    public Integer getLength() {
        return length;
    }

    public boolean isNullable() {
        return nullable;
    }
}
