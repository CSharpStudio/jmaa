package org.jmaa.jdbc.h2;

import org.jmaa.sdk.data.*;
import org.jmaa.sdk.exceptions.DataException;
import org.jmaa.sdk.exceptions.SqlConstraintException;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.DateUtils;
import org.jmaa.sdk.tools.ObjectUtils;
import org.jmaa.sdk.tools.StringUtils;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Eric Liang
 */
public class H2Dialect implements SqlDialect {
    @Override
    public String getName() {
        return "h2";
    }

    @Override
    public String getNowUtc() {
        return "CURRENT_TIMESTAMP()";
    }

    @Override
    public void createDatabase(Cursor cr, String name) {
        String sql = "CREATE SCHEMA IF NOT EXISTS " + name + " AUTHORIZATION sa";
        cr.execute(sql);
    }

    @Override
    public boolean databaseExists(Cursor cr, String name) {
        String sql = "SELECT count(1) FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = %s";
        cr.execute(sql, Arrays.asList(name));
        long count = ObjectUtils.toLong(cr.fetchOne()[0]);
        return count > 0;
    }

    @Override
    public void createColumn(Cursor cr, String table, String name, String columnType, String comment, boolean allowNull) {
        String sql = String.format("ALTER TABLE %s ADD COLUMN %s %s %s", quote(table), quote(name), columnType, allowNull ? "NULL" : "NOT NULL");
        if (!allowNull) {
            // 解决 1067 invalid default value for timestamp
            if ("timestamp".equals(columnType)) {
                sql += " DEFAULT '1970-01-01 12:00:00'";
            } else if ("date".equals(columnType)) {
                sql += " DEFAULT '1970-01-01'";
            }
        }
//        if (StringUtils.isNotEmpty(comment)) {
//            sql += String.format(" COMMENT '%s'", comment.replace("'", "''"));
//        }
        cr.execute(sql);
        schema.debug("Table {} added column {} of type {}", table, name, columnType);
    }

    @Override
    public String getColumnType(ColumnType type) {
        switch (type) {
            case Boolean:
                return "bool";
            case VarChar:
                return "varchar";
            case Text:
                return "longvarchar";
            case Binary:
                return "blob";
            case Integer:
                return "int";
            case Long:
                return "bigint";
            case Float:
                return "double";
            case Date:
                return "date";
            case DateTime:
                return "timestamp";
            default:
                return null;
        }
    }

    @Override
    public String getColumnType(ColumnType type, Integer length, Integer precision) {
        String result = getColumnType(type);
        if (length != null && length > 0) {
            result += "(" + length + ")";
        }
        return result;
    }

    @Override
    public boolean tableExists(Cursor cr, String table) {
        return existingTables(cr, Arrays.asList(table)).size() == 1;
    }

    @Override
    public List<String> existingTables(Cursor cr, List<String> tableNames) {
        String sql = "select TABLE_NAME, TABLE_TYPE from information_schema.tables"
            + " where TABLE_TYPE='TABLE' AND TABLE_SCHEMA=CURRENT_SCHEMA() AND TABLE_NAME in %s";
        cr.execute(sql, Arrays.asList(tableNames));
        List<String> result = new ArrayList<>();
        for (Object[] row : cr.fetchAll()) {
            result.add(((String) row[0]).toLowerCase());
        }
        return result;
    }

    @Override
    public void createModelTable(Cursor cr, String table, String comment) {
        String sql = String.format("CREATE TABLE %s (id VARCHAR(13) NOT NULL, PRIMARY KEY(id))", quote(table));
//        if (StringUtils.isNotEmpty(comment)) {
//            sql += String.format(" COMMENT '%s'", comment.replace("'", "''"));
//        }
        cr.execute(sql);
        schema.debug("Table {}: created", table);
    }

    @Override
    public Map<String, DbColumn> tableColumns(Cursor cr, String table) {
        String sql = "SELECT column_name,type_name,character_maximum_length,is_nullable " +
            "FROM Information_schema.columns where TABLE_SCHEMA=CURRENT_SCHEMA() and TABLE_NAME = %s";
        cr.execute(sql, Arrays.asList(table));
        List<Object[]> all = cr.fetchAll();
        Map<String, DbColumn> result = new HashMap<>(all.size());
        for (Object[] row : all) {
            String name = ((String) row[0]).toLowerCase();
            result.put(name, new DbColumn(name, (String) row[1], Integer.valueOf(row[2].toString()), "YES".equals((String) row[3])));
        }
        return result;
    }

    static Set<String> keywords = new HashSet<String>() {{
        add("table");
        add("order");
    }};

    @Override
    public String quote(String identify) {
        if (keywords.contains(identify)) {
            return "\"" + identify + "\"";
        }
        return identify;
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
        try {
            String definition = String.format("unique(%s)",
                Arrays.stream(fields).map(f -> quote(f)).collect(Collectors.joining(",")));
            String oldDefinition = getConstraintDefinition(cr, constraint);
            if (oldDefinition != null) {
                if (!definition.equals(oldDefinition)) {
                    dropConstraint(cr, table, constraint);
                } else {
                    return null;
                }
            }
            String sql = String.format("ALTER TABLE %s ADD CONSTRAINT %s %s", quote(table), quote(constraint),
                definition);
            cr.execute(sql);
            schema.debug("Table {} add unique constaint {} as {}", table, constraint, definition);
            return definition;
        } catch (Exception exc) {
            schema.warn("表{}添加唯一约束{}失败:{}", table, constraint, exc.getMessage());
            return null;
        }
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
        cr.execute("SELECT a.index_name, a.TABLE_NAME\n" +
            "FROM information_schema.INDEXES a\n" +
            "where a.table_schema=CURRENT_SCHEMA() and a.index_name IN %s", Arrays.asList(idxNames));
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
            String sql = String.format("ALTER TABLE %s ADD INDEX %s (%s)", cr.quote(table), cr.quote(idxName), args);
            cr.execute(sql);
            schema.debug("Table {}: add index {}({})", table, idxName, args);
        } catch (Exception e) {
            schema.warn("Table {}: unable to add index {}({})", table, idxName, args);
        }
    }

    @Override
    public void setNotNull(Cursor cr, String table, String column, String columnType) {
        try {
            String sql = String.format("ALTER TABLE %s MODIFY %s %s NOT NULL", quote(table), quote(column), columnType);
            // TODO savepoint
            cr.execute(sql);
            schema.debug("Table {}: column {}: added constraint NOT NULL", table, column);
        } catch (Exception e) {
            schema.warn("Table {}: unable to set column {} null", table, table, column);
        }
    }

    @Override
    public void dropNotNull(Cursor cr, String table, String column, String columnType) {
        try {
            String sql = String.format("ALTER TABLE %s MODIFY %s %s NULL", quote(table), quote(column), columnType);
            // TODO savepoint
            cr.execute(sql);
            schema.debug("Table {}: column {}: dropped constraint NOT NULL", table, column);
        } catch (Exception e) {
            schema.warn("Table {}: unable to drop column {} null", table, table, column);
        }
    }

    @Override
    public void createMany2ManyTable(Cursor cr, String table, String column1, String column2, String comment) {
        table = quote(table);
        column1 = quote(column1);
        column2 = quote(column2);
        String sql = "CREATE TABLE " + table + " (id VARCHAR(13)," + column1 + " VARCHAR(13) NOT NULL, " + column2
            + " VARCHAR(13) NOT NULL, PRIMARY KEY(" + column1 + "," + column2 + "))";

//        if (StringUtils.isNotEmpty(comment)) {
//            sql += String.format(" COMMENT = '%s'", comment.replace("'", "''"));
//        }
        cr.execute(sql);
        schema.debug("Create table %s: %s", table, comment);
    }

    @Override
    public List<Object[]> getForeignKeys(Cursor cr, Collection<String> tables) {
        String sql = "SELECT\n" +
            "  R.UNIQUE_CONSTRAINT_NAME," +
            "  UF.TABLE_NAME," +
            "  UF.COLUMN_NAME," +
            "  UU.TABLE_NAME AS R_TABLE_NAME," +
            "  UU.COLUMN_NAME AS R_COLUMN_NAME" +
            " FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS R" +
            " JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE UF" +
            "  ON (R.CONSTRAINT_CATALOG, R.CONSTRAINT_SCHEMA, R.CONSTRAINT_NAME)" +
            "    = (UF.CONSTRAINT_CATALOG, UF.CONSTRAINT_SCHEMA, UF.CONSTRAINT_NAME)" +
            " JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE UU" +
            "  ON (R.UNIQUE_CONSTRAINT_CATALOG, R.UNIQUE_CONSTRAINT_SCHEMA, R.UNIQUE_CONSTRAINT_NAME)" +
            "    = (UU.CONSTRAINT_CATALOG, UU.CONSTRAINT_SCHEMA, UU.CONSTRAINT_NAME)" +
            "  AND UU.ORDINAL_POSITION = UF.POSITION_IN_UNIQUE_CONSTRAINT WHERE UF.TABLE_NAME in %s";
        cr.execute(sql, Arrays.asList(tables));
        return cr.fetchAll();
    }

    @Override
    public String addForeignKey(Cursor cr, String table1, String column1, String table2, String column2,
                                String ondelete) {
        String fk = limitIdentity(String.format("fk_%s_%s", table1, column1));
        String sql = String.format("ALTER TABLE %s"
                + " ADD CONSTRAINT %s"
                + " FOREIGN KEY (%s)"
                + " REFERENCES %s (%s)"
                + " ON DELETE %s",
            cr.quote(table1), cr.quote(fk), cr.quote(column1), cr.quote(table2), cr.quote(column2), ondelete);
        try {
            cr.execute(sql);
            schema.debug("Table {}: added foreign key {} references {}({}) ON DELETE {}", table1, column1, table2,
                column2, ondelete);
        } catch (Exception exc) {
            schema.warn("表 {} 添加外键 {} 引用 {}({}) ON DELETE {}失败:{}", table1, column1, table2, column2, ondelete,
                exc.getMessage());
        }
        return fk;
    }

    @Override
    public String getColumnLabel(String column) {
        return column.toLowerCase();
    }

    @Override
    public Object getObject(Object obj) {
        if (obj instanceof org.h2.jdbc.JdbcBlob) {
            try {
                org.h2.jdbc.JdbcBlob blob = (org.h2.jdbc.JdbcBlob) obj;
                return blob.getBytes(1, (int)blob.length());
            } catch (SQLException e) {
                throw new DataException("读取CLOB字符值失败", e);
            }
        }
        return obj;
    }
}
