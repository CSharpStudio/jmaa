package org.jmaa.sdk.data;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据库方言
 *
 * @author Eric Liang
 */
public interface SqlDialect {
    Logger schema = LoggerFactory.getLogger("sql.schema");

    String getName();

    /**
     * 数据库标识符最大长度
     */
    default int getIdentityMaxLength() {
        return 63;
    }

    default void setTransactionIsolation(Connection conn, int level) {
        try {
            conn.setTransactionIsolation(level);
        } catch (Exception e) {
        }
    }

    default void setAutoCommit(Connection conn, boolean autoCommit) {
        try {
            conn.setAutoCommit(autoCommit);
        } catch (SQLException e) {
        }
    }

    /**
     * 获取数据库时间
     */
    String getNowUtc();

    /**
     * 生成别名
     */
    default String generateTableAlias(String srcTableAlias, String link) {
        return limitIdentity(srcTableAlias + "__" + link);
    }

    /**
     * 处理列名
     */
    default String getColumnLabel(String column) {
        return column;
    }

    /**
     * 处理数据值
     */
    default Object getObject(Object obj) {
        return obj;
    }

    /**
     * 转为数据库时间
     */
    default Timestamp toDbTimestamp(Timestamp ts) {
        return ts;
    }

    /**
     * 准备参数值
     */
    default Object prepareObject(Object obj) {
        return obj;
    }

    /**
     * 标识符长度限制
     */
    default String limitIdentity(String identity) {
        if (identity.length() > getIdentityMaxLength()) {
            CRC32 crc = new CRC32();
            crc.update(identity.getBytes(StandardCharsets.UTF_8));
            identity = identity.substring(0, getIdentityMaxLength() - 10) + "_" + Long.toString(crc.getValue(), 32);
        }
        return identity;
    }

    /**
     * 创建数据库
     */
    void createDatabase(Cursor cr, String name);

    /**
     * 数据库是否存在
     */
    boolean databaseExists(Cursor cr, String name);

    @Deprecated
    default void createColumn(Cursor cr, String table, String name, String columnType, String comment) {
        createColumn(cr, table, name, columnType, comment, true);
    }

    /**
     * 创建字段
     */
    void createColumn(Cursor cr, String table, String name, String columnType, String comment, boolean allowNull);

    /**
     * 根据{@link ColumnType}转换数据库字段类型
     */
    String getColumnType(ColumnType type);

    /**
     * 根据{@link ColumnType}转换数据库字段类型
     */
    String getColumnType(ColumnType type, Integer length, Integer precision);

    /**
     * 判断指定的表名是否存在
     */
    boolean tableExists(Cursor cr, String table);

    /**
     * 判断指定的表名是否存在
     *
     * @return 存在的表名
     */
    List<String> existingTables(Cursor cr, List<String> tableNames);

    /**
     * 创建模型的表
     */
    void createModelTable(Cursor cr, String table, String comment);

    /**
     * 创建多对多中间表
     */
    void createMany2ManyTable(Cursor cr, String table, String column1, String column2, String comment);

    /**
     * 获取指定表的字段
     */
    Map<String, DbColumn> tableColumns(Cursor cr, String table);

    /**
     * 为数据库标识符添加分隔符：如oracle:"identify", SqlServer:[identify], mysql: `identify`
     */
    String quote(String identify);

    /**
     * 生成分页sql
     */
    String getPaging(String sql, Integer limit, Integer offset);

    /**
     * 数据类型转换
     */
    String cast(String column, ColumnType type);

    /**
     * 添加唯一约束
     */
    String addUniqueConstraint(Cursor cr, String table, String constraint, String[] fields);

    /**
     * 设置非空
     */
    void setNotNull(Cursor cr, String table, String column, String columnType);

    /**
     * 设置可空
     */
    void dropNotNull(Cursor cr, String table, String column, String columnType);

    /**
     * 获取指定表的外键信息
     */
    List<Object[]> getForeignKeys(Cursor cr, Collection<String> tables);

    /**
     * 添加外键
     */
    String addForeignKey(Cursor cr, String table1, String column1, String table2, String column2, String onDelete);

    /**
     * 删除约束
     */
    void dropConstraint(Cursor cr, String table, String name);

    /**
     * 处理异常
     */
    RuntimeException getError(SQLException err, SqlFormat sql);

    /**
     * 获取指定名称的索引
     */
    Map<String, String> getIndexes(Cursor cr, Collection<String> idxNames);

    /**
     * 创建索引
     */
    void createIndex(Cursor cr, String idxName, String table, Collection<String> columns);
}
