package org.jmaa.sdk.data;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jmaa.sdk.tools.StringUtils;

/**
 * 查询
 *
 * @author Eric Liang
 */
public class Query {
    Map<String, String> tables = new HashMap<>();
    List<String> whereClause = new ArrayList<>();
    List<Object> whereParams = new ArrayList<>();
    Map<String, JoinClause> joins = new LinkedHashMap<>();
    String order;
    Integer limit;
    Integer offset;
    Cursor cr;

    /**
     * 获取表信息：{别名:表名}
     *
     * @return
     */
    public Map<String, String> getTables() {
        return tables;
    }

    /**
     * 获取where条件
     *
     * @return
     */
    public List<String> getWhereClause() {
        return whereClause;
    }

    /**
     * 获取where参数
     *
     * @return
     */
    public List<Object> getWhereParams() {
        return whereParams;
    }

    /**
     * 获取关联信息
     *
     * @return
     */
    public Map<String, JoinClause> getJoins() {
        return joins;
    }

    /**
     * 获取排序语句
     *
     * @return
     */
    public String getOrder() {
        return order;
    }

    /**
     * 设置排序语句
     *
     * @param order
     * @return
     */
    public Query setOrder(String order) {
        this.order = order;
        return this;
    }

    /**
     * 获取限制数量
     *
     * @return
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * 设置限制数量
     *
     * @param limit
     * @return
     */
    public Query setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    /**
     * 获取偏移数量
     *
     * @return
     */
    public Integer getOffset() {
        return offset;
    }

    /**
     * 设置偏移数量
     *
     * @param offset
     * @return
     */
    public Query setOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    /**
     * 创建实例
     *
     * @param cr
     * @param alias
     */
    public Query(Cursor cr, String alias) {
        this(cr, alias, alias);
    }

    /**
     * 创建实例
     *
     * @param cr
     * @param alias
     * @param table
     */
    public Query(Cursor cr, String alias, String table) {
        this.cr = cr;
        if (StringUtils.isEmpty(table)) {
            table = alias;
        }
        tables.put(alias, table);
    }

    /**
     * 添加表
     *
     * @param alias
     */
    public void addTable(String alias) {
        addTable(alias, alias);
    }

    /**
     * 添加表
     *
     * @param alias
     * @param table
     */
    public void addTable(String alias, String table) {
        assert !tables.containsKey(alias) && !joins.containsKey(alias)
            : String.format("别名[%s]已经存在于[%s]", alias, this);
        tables.put(alias, table);
    }

    /**
     * 添加where条件
     *
     * @param whereClause
     */
    public void addWhere(String whereClause) {
        addWhere(whereClause, null);
    }

    /**
     * 添加where条件
     *
     * @param whereClause
     * @param whereParams
     */
    public void addWhere(String whereClause, List<Object> whereParams) {
        this.whereClause.add(whereClause);
        if (whereParams != null) {
            this.whereParams.addAll(whereParams);
        }
    }

    /**
     * 表连接
     *
     * @param lhsAlias
     * @param lhsColumn
     * @param rhsTable
     * @param rhsColumn
     * @param link
     * @return
     */
    public String join(String lhsAlias, String lhsColumn, String rhsTable, String rhsColumn, String link) {
        return join("JOIN", lhsAlias, lhsColumn, rhsTable, rhsColumn, link, null, null);
    }

    /**
     * 表连接
     *
     * @param lhsAlias
     * @param lhsColumn
     * @param rhsTable
     * @param rhsColumn
     * @param link
     * @param extra
     * @param extraParams
     * @return
     */
    public String join(String lhsAlias, String lhsColumn, String rhsTable, String rhsColumn, String link,
                       String extra, List<Object> extraParams) {
        return join("JOIN", lhsAlias, lhsColumn, rhsTable, rhsColumn, link, extra, extraParams);
    }

    /**
     * 左连接
     *
     * @param lhsAlias
     * @param lhsColumn
     * @param rhsTable
     * @param rhsColumn
     * @param link
     * @return
     */
    public String leftJoin(String lhsAlias, String lhsColumn, String rhsTable, String rhsColumn, String link) {

        return join("LEFT JOIN", lhsAlias, lhsColumn, rhsTable, rhsColumn, link, null, null);
    }

    /**
     * 左连接
     *
     * @param lhsAlias
     * @param lhsColumn
     * @param rhsTable
     * @param rhsColumn
     * @param link
     * @param extra
     * @param extraParams
     * @return
     */
    public String leftJoin(String lhsAlias, String lhsColumn, String rhsTable, String rhsColumn, String link,
                           String extra, List<Object> extraParams) {

        return join("LEFT JOIN", lhsAlias, lhsColumn, rhsTable, rhsColumn, link, extra, extraParams);
    }

    /**
     * 获取SqlClause
     *
     * @return
     */
    public SqlClause getSql() {
        List<String> fromList = new ArrayList<>();
        for (String alias : tables.keySet()) {
            fromList.add(formTable(tables.get(alias), alias));
        }
        String from = StringUtils.join(fromList, ",");
        List<String> joinList = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (String alias : joins.keySet()) {
            JoinClause join = joins.get(alias);
            joinList.add(String.format("%s %s ON (%s)", join.kind, formTable(join.getRhs(), alias), join.condition));
            params.addAll(join.getConditionParams());
        }
        if (joinList.size() > 0) {
            from += " " + StringUtils.join(joinList, " ");
        }
        String where = StringUtils.join(whereClause, " AND ");
        params.addAll(whereParams);
        return new SqlClause(from, where, params);
    }

    /**
     * 指定select列
     *
     * @param selects
     * @return
     */
    public SelectClause select(String... selects) {
        return doSelect(selects.length > 0 ? StringUtils.join(selects, ",")
            : cr.quote(tables.keySet().stream().findFirst().get()) + ".id");
    }

    /**
     * 指定select列
     *
     * @param selects
     * @return
     */
    public SelectClause select(Collection<String> selects) {
        return doSelect(selects != null && selects.size() > 0 ? StringUtils.join(selects, ",")
            : cr.quote(tables.keySet().stream().findFirst().get()) + ".id");
    }

    /**
     * 指定子查询列
     *
     * @param selects
     * @return
     */
    public SelectClause subSelect(String... selects) {
        return doSubSelect(selects.length > 0 ? StringUtils.join(selects, ",")
            : cr.quote(tables.keySet().stream().findFirst().get()) + ".id");
    }

    /**
     * 指定子查询列
     *
     * @param selects
     * @return
     */
    public SelectClause subSelect(Collection<String> selects) {
        return doSubSelect(selects != null && selects.size() > 0 ? StringUtils.join(selects, ",")
            : cr.quote(tables.keySet().stream().findFirst().get()) + ".id");
    }

    SelectClause doSelect(String selects) {
        SqlClause sql = getSql();
        String queryStr = String.format("SELECT %s FROM %s", selects, sql.getFrom());
        if (StringUtils.isNotEmpty(sql.getWhere())) {
            queryStr += " WHERE " + sql.getWhere();
        }
        if (StringUtils.isNotEmpty(order)) {
            queryStr += " ORDER BY " + order;
        }
        queryStr = cr.getSqlDialect().getPaging(queryStr, limit, offset);
        return new SelectClause(queryStr, sql.getParams());
    }

    SelectClause doSubSelect(String selects) {
        // 有分页时需要排序
        boolean isPaging = (limit != null && limit > 0) || (offset != null && offset > 0);
        if (isPaging) {
            return doSelect(selects);
        }
        // 不需要排序
        SqlClause sql = getSql();
        String queryStr = String.format("SELECT %s FROM %s", selects, sql.getFrom());
        if (StringUtils.isNotEmpty(sql.getWhere())) {
            queryStr += " WHERE " + sql.getWhere();
        }
        queryStr = cr.getSqlDialect().getPaging(queryStr, limit, offset);
        return new SelectClause(queryStr, sql.getParams());
    }

    static Pattern table_name_patten = Pattern.compile("^[a-z_][a-z0-9_$]*$", Pattern.CASE_INSENSITIVE);

    String formTable(String table, String alias) {
        if (table != null && table.equals(alias)) {
            return cr.quote(alias);
        }
        Matcher matcher = table_name_patten.matcher(table);
        if (matcher.matches()) {
            return String.format("%s %s", cr.quote(table), cr.quote(alias));
        }
        return String.format("(%s) %s", table, cr.quote(alias));

    }

    String join(String kind, String lhsAlias, String lhsColumn, String rhsTable, String rhsColumn, String link,
                String extra, List<Object> extraParams) {
        String rhsAlias = cr.getSqlDialect().generateTableAlias(lhsAlias, link);
        if (!joins.containsKey(rhsAlias)) {
            String condition = String.format("%s.%s = %s.%s", cr.quote(lhsAlias), cr.quote(lhsColumn),
                cr.quote(rhsAlias), cr.quote(rhsColumn));
            List<Object> conditionParams = new ArrayList<>();
            if (StringUtils.isNotEmpty(extra)) {
                condition += " AND "
                    + extra.replace("{lhs}", cr.quote(lhsAlias)).replace("{rhs}", cr.quote(rhsAlias));
                if (extraParams != null) {
                    conditionParams.addAll(extraParams);
                }
            }
            if (StringUtils.isNotEmpty(kind)) {
                joins.put(rhsAlias, new JoinClause(kind, rhsTable, condition, conditionParams));
            } else {
                tables.put(rhsAlias, rhsTable);
                addWhere(condition, conditionParams);
            }
        }
        return rhsAlias;
    }

    /**
     * Select语句
     */
    public class SelectClause {
        String query;
        List<Object> params;

        public SelectClause(String query, List<Object> params) {
            this.query = query;
            this.params = params;
        }

        public String getQuery() {
            return query;
        }

        public List<Object> getParams() {
            return params;
        }
    }

    /**
     * SQL语句
     */
    public class SqlClause {
        String from;
        String where;
        List<Object> params;

        public SqlClause(String from, String where, List<Object> params) {
            this.from = from;
            this.where = where;
            this.params = params;
        }

        public String getFrom() {
            return from;
        }

        public String getWhere() {
            return where;
        }

        public List<Object> getParams() {
            return params;
        }
    }

    /**
     * 关联分句
     */
    public class JoinClause {
        String kind;
        String rhs;
        String condition;
        List<Object> conditionParams;

        public JoinClause(String kind, String rhsTable, String condition, List<Object> conditionParams) {
            this.kind = kind;
            this.rhs = rhsTable;
            this.condition = condition;
            this.conditionParams = conditionParams;
        }

        public String getKind() {
            return kind;
        }

        public String getRhs() {
            return rhs;
        }

        public String getCondition() {
            return condition;
        }

        public List<Object> getConditionParams() {
            return conditionParams;
        }
    }
}
