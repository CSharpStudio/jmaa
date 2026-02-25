package org.jmaa.jdbc.clickhouse;

import org.jmaa.sdk.data.*;
import org.jmaa.sdk.exceptions.DataException;
import org.jmaa.sdk.exceptions.SqlConstraintException;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.ObjectUtils;
import org.jmaa.sdk.tools.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Eric Liang
 */
public class ClickhouseDialect implements SqlDialect {
    @Override
    public void setTransactionIsolation(Connection conn, int level) {
    }

    @Override
    public void setAutoCommit(Connection conn, boolean autoCommit) {
    }

    /**
     * 准备参数
     */
    @Override
    public Object prepareObject(Object obj) {
        if (obj instanceof byte[]) {
            return Base64.getEncoder().encodeToString((byte[]) obj);
        }
        if (obj instanceof Date) {
            return new java.sql.Timestamp(((Date) obj).getTime());
        }
        return obj;
    }

    @Override
    public String getName() {
        return "clickhouse";
    }

    @Override
    public String getNowUtc() {
        return "now('UTC')";
    }

    @Override
    public void createDatabase(Cursor cr, String name) {
        String sql = "CREATE DATABASE IF NOT EXISTS " + name + " ENGINE = Atomic";
        cr.execute(sql);
    }

    @Override
    public boolean databaseExists(Cursor cr, String name) {
        String sql = "SELECT count(1) FROM system.databases WHERE = %s";
        cr.execute(sql, Arrays.asList(name));
        long count = ObjectUtils.toLong(cr.fetchOne()[0]);
        return count > 0;
    }

    @Override
    public void createColumn(Cursor cr, String table, String name, String columnType, String comment, boolean allowNull) {
        if (allowNull) {
            columnType = "Nullable(" + columnType + ")";
        }
        String sql = String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS %s %s", quote(table), quote(name), columnType);
        if (StringUtils.isNotEmpty(comment)) {
            sql += String.format(" COMMENT '%s'", comment.replace("'", "''"));
        }
        cr.execute(sql);
        schema.debug("Table {} added column {} of type {}", table, name, columnType);
    }

    @Override
    public String getColumnType(ColumnType type) {
        switch (type) {
            case Boolean:
                return "Int8";
            case VarChar:
                return "String";
            case Text:
                return "String";
            case Binary:
                return "String";
            case Integer:
                return "Int32";
            case Long:
                return "Int64";
            case Float:
                return "Float64";
            case Date:
                return "Date";
            case DateTime:
                return "DateTime64(3)";
            default:
                return null;
        }
    }

    @Override
    public String getColumnType(ColumnType type, Integer length, Integer precision) {
        return getColumnType(type);
    }

    @Override
    public boolean tableExists(Cursor cr, String table) {
        return existingTables(cr, Arrays.asList(table)).size() == 1;
    }

    @Override
    public List<String> existingTables(Cursor cr, List<String> tableNames) {
        String sql = "select name FROM system.tables where engine != 'View' AND database = currentDatabase() AND name IN (%s)";
        cr.execute(sql, Arrays.asList(tableNames));
        List<String> result = new ArrayList<>();
        for (Object[] row : cr.fetchAll()) {
            result.add((String) row[0]);
        }
        return result;
    }

    @Override
    public void createModelTable(Cursor cr, String table, String comment) {
        String sql = String.format("CREATE TABLE IF NOT EXISTS %s (id String NOT NULL) ENGINE = MergeTree() ORDER BY id SETTINGS enable_block_number_column = 1, enable_block_offset_column = 1", quote(table));
        if (StringUtils.isNotEmpty(comment)) {
            sql += String.format(" COMMENT '%s'", comment.replace("'", "''"));
        }
        cr.execute(sql);
        schema.debug("Table {}: created", table);
    }

    @Override
    public Map<String, DbColumn> tableColumns(Cursor cr, String table) {
        String sql = "SELECT name,type FROM system.columns  WHERE database = currentDatabase() AND table = %s";
        cr.execute(sql, Arrays.asList(table));
        List<Object[]> all = cr.fetchAll();
        Map<String, DbColumn> result = new HashMap<>(all.size());
        for (Object[] row : all) {
            result.put((String) row[0], new DbColumn((String) row[0], (String) row[1], -1, false));
        }
        return result;
    }

    @Override
    public String quote(String identify) {
        return String.format("`%s`", identify);
    }

    @Override
    public String getPaging(String sql, Integer limit, Integer offset) {
        if (limit != null && limit > 0) {
            sql += " LIMIT " + limit;
        }
        if (offset != null && offset > 0) {
            sql += " OFFSET " + offset;
        }
        return sql;
    }

    @Override
    public String cast(String column, ColumnType type) {
        return column;
    }

    @Override
    public String addUniqueConstraint(Cursor cr, String table, String constraint, String[] fields) {
        return String.format("unique(%s)", Arrays.stream(fields).map(f -> quote(f)).collect(Collectors.joining(",")));
    }

    @Override
    public void dropConstraint(Cursor cr, String table, String constraint) {
        try {
            // TODO savepoint
            cr.execute(String.format("ALTER TABLE %s DROP CONSTRAINT %s", quote(table), quote(constraint)));
            schema.debug("Table {}: dropped constraint {}", table, constraint);
        } catch (Exception e) {
            schema.warn("Table {}: unable to drop constraint {}", table, constraint);
        }
    }

    String getConstraintDefinition(Cursor cr, String constraint) {
        String sql = "SELECT definition FROM ir_model_constraint where name=%s";
        cr.execute(sql, Arrays.asList(constraint));
        Object[] row = cr.fetchOne();
        return row.length > 0 ? (String) row[0] : null;
    }

    Pattern constraintPattern = Pattern.compile("CONSTRAINT `(?<name>\\S+)` FOREIGN KEY");

    @Override
    public RuntimeException getError(SQLException err, SqlFormat sql) {
        if (err.getErrorCode() == 1451) {
            String msg = err.getMessage();
            Matcher m = constraintPattern.matcher(msg);
            if (m.find()) {
                String constraint = m.group("name");
                throw new SqlConstraintException(constraint);
            }
            throw new ValidationException("数据被引用,不能删除", err);
        }
        return new DataException(String.format("执行SQL[%s]失败", sql.getSql()), err);
    }

    @Override
    public Map<String, String> getIndexes(Cursor cr, Collection<String> idxNames) {
        cr.execute("SELECT distinct a.name AS index_name, a.table AS TABLE_NAME " +
            "FROM system.data_skipping_indices a " +
            "WHERE a.database = currentDatabase() AND a.name IN %s", Arrays.asList(idxNames));
        Map<String, String> result = new HashMap<>();
        for (Object[] row : cr.fetchAll()) {
            result.put((String) row[0], (String) row[1]);
        }
        return result;
    }

    @Override
    public void createIndex(Cursor cr, String idxName, String table, Collection<String> columns) {
        String args = columns.stream().collect(Collectors.joining(","));
        try {
            // TODO savepoint
            String sql = String.format("ALTER TABLE %s ADD INDEX IF NOT EXISTS %s (%s) TYPE minmax", cr.quote(table), cr.quote(idxName), args);
            cr.execute(sql);
            schema.debug("Table {}: add index {}({})", table, idxName, args);
        } catch (Exception e) {
            schema.warn("Table {}: unable to add index {}({})", table, idxName, args);
        }
    }

    @Override
    public void setNotNull(Cursor cr, String table, String column, String columnType) {
        //
    }

    @Override
    public void dropNotNull(Cursor cr, String table, String column, String columnType) {
        //
    }

    @Override
    public void createMany2ManyTable(Cursor cr, String table, String column1, String column2, String comment) {
        table = quote(table);
        column1 = quote(column1);
        column2 = quote(column2);
        String sql = String.format("CREATE TABLE IF NOT EXISTS %s (%s String, %s String) ENGINE = MergeTree() ORDER BY (%s, %s)",
            table, column1, column2, column1, column2);

        if (StringUtils.isNotEmpty(comment)) {
            sql += String.format(" COMMENT '%s'", comment.replace("'", "''"));
        }
        cr.execute(sql);
        schema.debug("Create table %s: %s", table, comment);
    }

    @Override
    public List<Object[]> getForeignKeys(Cursor cr, Collection<String> tables) {
        return Collections.emptyList();
    }

    @Override
    public String addForeignKey(Cursor cr, String table1, String column1, String table2, String column2, String ondelete) {
        return limitIdentity(String.format("fk_%s_%s", table1, column1));
    }
}
