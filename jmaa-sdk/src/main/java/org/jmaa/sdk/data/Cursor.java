package org.jmaa.sdk.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jmaa.sdk.Utils;
import org.jmaa.sdk.tools.*;
import org.jmaa.sdk.util.Callbacks;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.exceptions.DataException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据访问游标
 *
 * @author Eric Liang
 */
public class Cursor implements AutoCloseable {
    private final static Logger logger = LoggerFactory.getLogger(Cursor.class);
    /**
     * decent limit on size of IN queries - guideline = Oracle limit
     */
    static int IN_MAX = 1000;
    Connection connection;
    PreparedStatement statement;
    ResultSet resultSet;
    SqlDialect sqlDialect;
    CursorState state;
    Callbacks preCommit = new Callbacks();

    /**
     * 游标状态
     */
    public enum CursorState {
        /**
         * 未执行execute，此时调用fetch*()将抛异常
         */
        Unexecuted,
        /**
         * 已执行execute但没有返回数据
         */
        ExecuteNonQuery,
        /**
         * 数据可读，此时可执行fetch*()
         */
        Fetchable,
        /**
         * 数据已读完
         */
        EndOfFetch,
    }

    int rowCount;

    /**
     * 使用方言创建实例
     *
     * @param dialect 数据库方言
     */
    protected Cursor(SqlDialect dialect) {
        sqlDialect = dialect;
    }

    /**
     * 创建实例，默认关闭自动提交
     *
     * @param conn    数据库链接
     * @param dialect 数据库方言
     */
    public Cursor(Connection conn, SqlDialect dialect) {
        connection = conn;
        dialect.setTransactionIsolation(conn, Connection.TRANSACTION_READ_COMMITTED);
        sqlDialect = dialect;
        setAutoCommit(false);
    }

    /**
     * 获取数据库方言
     *
     * @return 数据库方言
     */
    public SqlDialect getSqlDialect() {
        return sqlDialect;
    }

    /**
     * 获取数据库链接
     *
     * @return 数据库链接
     */
    public Connection getConnection() {
        return connection;
    }

    public Callbacks getPreCommit() {
        return preCommit;
    }

    /**
     * 设置数据库自动提交模式为指定状态
     *
     * @param autoCommit 是否自动提交
     */
    public void setAutoCommit(boolean autoCommit) {
        getSqlDialect().setAutoCommit(connection, autoCommit);
    }

    /**
     * 提交当前事务的所有变更，释放数据库锁
     */
    public void commit() {
        try {
            preCommit.run();
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new DataException("事务提交失败", e);
        }
    }

    /**
     * 回滚当前事务的所有变更，释放数据库锁
     */
    public void rollback() {
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
            }
        } catch (SQLException e) {
            throw new DataException("事务回滚失败", e);
        }
    }

    /**
     * 获取格式化的sql，处理字符串中的%s占位符，生成参数化的sql
     *
     * @param sql    SQL语句
     * @param params SQL参数
     * @return 格式化的SQL
     */
    public SqlFormat getSqlFormat(String sql, Collection<?> params) {
        List<Object> args = new ArrayList<>();
        for (Object param : params) {
            Object p = param;
            if (p instanceof Object[]) {
                p = Arrays.asList((Object[]) p);
            }
            if (p instanceof Collection<?>) {
                Collection<?> col = (Collection<?>) p;
                if (col.isEmpty()) {
                    sql = sql.replaceFirst("%s", "(null)");
                } else {
                    String v = col.stream().map(c -> "?").collect(Collectors.joining(","));
                    sql = sql.replaceFirst("%s", "(" + v + ")");
                    args.addAll(col);
                }
            } else {
                sql = sql.replaceFirst("%s", "?");
                args.add(p);
            }
        }
        return new SqlFormat(sql, args);
    }

    /**
     * 关闭链接，回滚所有未提交的变更
     */
    @Override
    public void close() {
        reset();
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
            }
        } catch (Exception e) {
            logger.warn("数据库事务回滚失败", e);
        }
        try {
            connection.close();
        } catch (SQLException e) {
            logger.warn("数据库连接关闭失败", e);
        }
    }

    /**
     * 执行sql
     *
     * @param sql SQL语句
     */
    public void execute(String sql) {
        execute(sql, Collections.emptyList(), true);
    }

    /**
     * 执行sql
     *
     * @param sql    SQL语句
     * @param params SQL参数
     */
    public void execute(String sql, Collection<?> params) {
        execute(sql, params, true);
    }

    static Boolean printSql;

    boolean isPrintSql() {
        if (printSql == null) {
            printSql = ObjectUtils.toBoolean(SpringUtils.getProperty("printSql"));
        }
        return printSql;
    }

    /**
     * 设置是否打印sql到控制台
     *
     * @param isPrint 是否打印SQL
     */
    public static void setPrintSql(boolean isPrint) {
        printSql = isPrint;
    }

    /**
     * 执行sql
     *
     * @param sql           SQL语句
     * @param params        SQL参数
     * @param logExceptions 是否记录异常
     * @return true if the first result is a ResultSet object; false if the first result is an update count or there is no result
     */
    public boolean execute(String sql, Collection<?> params, boolean logExceptions) {
        reset();
        SqlFormat format = getSqlFormat(sql, params);
        try (AutoCloseable m = Profiler.monitor(() -> Utils.format("sql:%s", format.toString(getSqlDialect())))) {
            statement = connection.prepareStatement(format.getSql());
            int parameterIndex = 1;
            for (Object p : format.getParmas()) {
                statement.setObject(parameterIndex++, sqlDialect.prepareObject(p));
            }
            if (isPrintSql()) {
                System.out.println(format.toString(getSqlDialect()));
            }
            long elapsed = System.currentTimeMillis();
            boolean res = statement.execute();
            elapsed = System.currentTimeMillis() - elapsed;
            if (elapsed > 500) {
                logger.warn("sql slow:{}", format.toString(getSqlDialect()));
            }
            if (res) {
                resultSet = statement.getResultSet();
                //rowCount = resultSet.getRow();
                scroll();
            } else {
                int count = statement.getUpdateCount();
                reset();
                rowCount = count;
                state = CursorState.ExecuteNonQuery;
            }
            return res;
        } catch (SQLException e) {
            if (logExceptions) {
                logger.error("sql error:{}\n{}", ThrowableUtils.getCause(e).getMessage(), format.toString(getSqlDialect()));
            }
            throw getSqlDialect().getError(e, format);
        } catch (Exception e) {
            throw new DataException(e);
        }
    }

    /**
     * 获取受影响行数
     *
     * @return 获取受影响行数
     */
    public int getRowCount() {
        return rowCount;
    }

    /**
     * 读取一行数据
     *
     * @return 数据
     */
    public Object[] fetchOne() {
        ensureExecuted();
        if (state == CursorState.Fetchable) {
            Object[] row = readRow();
            scroll();
            return row;
        }
        return ArrayUtils.EMPTY_OBJECT_ARRAY;
    }

    /**
     * 读取指定数量的数据
     *
     * @param size 读取不超过指定大小的数据量
     * @return 数据
     */
    public List<Object[]> fetchMany(int size) {
        ensureExecuted();
        List<Object[]> list = new ArrayList<>();
        int i = 0;
        while (state == CursorState.Fetchable && i++ < size) {
            list.add(readRow());
            scroll();
        }
        return list;
    }

    /**
     * 读取所有数据
     *
     * @return 数据
     */
    public List<Object[]> fetchAll() {
        ensureExecuted();
        List<Object[]> list = new ArrayList<>();
        while (state == CursorState.Fetchable) {
            list.add(readRow());
            scroll();
        }
        return list;
    }

    /**
     * 读取一行数据
     *
     * @return 数据
     */
    public Map<String, Object> fetchMapOne() {
        ensureExecuted();
        if (state == CursorState.Fetchable) {
            Map<String, Object> map = readMap();
            scroll();
            return map;
        }
        return KvMap.empty();
    }

    /**
     * 读取指定数量的数据
     *
     * @param size 读取不超过指定大小的数据量
     * @return 数据
     */
    public List<Map<String, Object>> fetchMapMany(int size) {
        ensureExecuted();
        List<Map<String, Object>> list = new ArrayList<>();
        int i = 0;
        while (state == CursorState.Fetchable && i++ < size) {
            list.add(readMap());
            scroll();
        }
        return list;
    }

    /**
     * 读取所有数据
     *
     * @return 数据
     */
    public List<Map<String, Object>> fetchMapAll() {
        ensureExecuted();
        List<Map<String, Object>> list = new ArrayList<>();
        while (state == CursorState.Fetchable) {
            list.add(readMap());
            scroll();
        }
        return list;
    }

    Map<String, Object> readMap() {
        try {
            ResultSetMetaData meta = resultSet.getMetaData();
            Object[] values = new Object[meta.getColumnCount()];
            KvMap map = new KvMap(values.length);
            for (int i = 1; i <= values.length; i++) {
                map.put(sqlDialect.getColumnLabel(meta.getColumnLabel(i)), resultSet.getObject(i));
            }
            return map;
        } catch (SQLException e) {
            throw new DataException("读取行失败", e);
        }
    }

    Object[] readRow() {
        try {
            ResultSetMetaData meta = resultSet.getMetaData();
            Object[] values = new Object[meta.getColumnCount()];
            for (int i = 0; i < values.length; i++) {
                values[i] = sqlDialect.getObject(resultSet.getObject(i + 1));
            }
            return values;
        } catch (SQLException e) {
            throw new DataException("读取行失败", e);
        }
    }

    void ensureExecuted() {
        if (state == CursorState.Unexecuted) {
            throw new DataException("没有执行SQL");
        }
    }

    private void scroll() {
        try {
            if (resultSet.next()) {
                rowCount++;
                state = CursorState.Fetchable;
            } else {
                reset();
                state = CursorState.EndOfFetch;
            }
        } catch (Exception e) {
            throw new DataException("记录集滚动失败", e);
        }
    }

    private void reset() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            logger.warn("记录集关闭失败", e);
        }
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            logger.warn("SQL关闭失败", e);
        }
        resultSet = null;
        statement = null;
        rowCount = 0;
        state = CursorState.Unexecuted;
    }

    /**
     * 给标识符添加引号，不同数据库使用的引号不同
     *
     * @param identify 标识符
     * @return 添加引号的标识符
     */
    public String quote(String identify) {
        return sqlDialect.quote(identify);
    }

    /**
     * 根据in条件参数个数限制，对参数进行分批
     *
     * @param ids id数组
     * @return 分组的id
     */
    public List<Object[]> splitForInConditions(Object[] ids) {
        List<Object[]> result = new ArrayList<>();
        if (ids.length < IN_MAX) {
            result.add(ids);
        } else {
            int from = 0;
            while (from < ids.length) {
                int to = from + IN_MAX;
                if (to > ids.length) {
                    to = ids.length;
                }
                result.add(Arrays.copyOfRange(ids, from, to));
                from += IN_MAX;
            }
        }
        return result;
    }
}
