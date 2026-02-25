package jmaa.modules.report.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.data.SqlDialect;
import org.jmaa.sdk.data.SqlFormat;
import org.jmaa.sdk.data.xml.SqlTemplate;
import org.jmaa.sdk.data.xml.SqlTemplateBuilder;
import org.jmaa.sdk.exceptions.DataException;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.ThrowableUtils;
import org.jmaa.sdk.util.KvMap;

import java.sql.*;
import java.util.*;

@Model.Meta(name = "rpt.report", label = "查询报表", inherit = "code.auto_code")
public class Report extends Model {
    static Field code = Field.Char().label("编码").unique();
    static Field name = Field.Char().label("名称");
    static Field content = Field.Text().label("内容");
    static Field status = Field.Selection(new Options() {{
        put("0", "未发布");
        put("1", "已发布");
    }}).label("状态").defaultValue("0");
    static Field menu_id = Field.Many2one("ir.ui.menu").label("菜单").ondelete(DeleteMode.SetNull);
    static Field dataset_ids = Field.One2many("rpt.dataset", "report_id").label("数据源");

    @ServiceMethod(label = "更新状态")
    public Object updateStatus(Records records, String status) {
        records.set("status", status);
        return Action.success();
    }

    @OnSaved("status")
    public void onStatusSave(Records records) {
        for (Records record : records) {
            String status = record.getString("status");
            Records menu = record.getRec("menu_id");
            if ("1".equals(status)) {
                if (menu.any()) {
                    menu.set("active", true);
                    menu.set("url", "/" + record.getEnv().getRegistry().getTenant().getKey() + "/report#" + record.getString("code"));
                } else {
                    menu = menu.create(new KvMap()
                        .set("name", record.get("name"))
                        .set("parent_id", record.getEnv().getRef("report.report_menus").getId())
                        .set("url", "/" + record.getEnv().getRegistry().getTenant().getKey() + "/report#" + record.getString("code"))
                        .set("icon", "/web/jmaa/modules/report/statics/menu.png"));
                    record.set("menu_id", menu.getId());
                }
            } else {
                menu.set("active", false);
            }
        }
    }

    @OnDelete
    public void onDelete(Records records) {
        for (Records row : records) {
            if ("1".equals(row.getString("status"))) {
                throw new ValidationException(row.l10n("报表[%s]状态为已发布，不能删除", row.get("present")));
            }
            Records menu = row.getRec("menu_id");
            menu.delete();
        }
    }

    @ServiceMethod(auth = Constants.ANONYMOUS)
    public Object loadReport(Records records, String code) {
        Records report = records.find(Criteria.equal("code", code));
        return report.read(Utils.asList("name", "content")).get(0);
    }

    public boolean hasPermission(Records record) {
        Records menu = record.getRec("menu_id");
        Cursor cr = record.getEnv().getCursor();
        String sql = "SELECT count(1) FROM rbac_role_menu m"
            + " JOIN rbac_role_user u on m.role_id=u.role_id"
            + " JOIN rbac_role r on m.role_id=r.id"
            + " WHERE u.user_id=%s AND r.active=%s AND m.menu_id=%s";
        cr.execute(sql, Arrays.asList(record.getEnv().getUserId(), true, menu.getId()));
        return Utils.toLong(cr.fetchOne()[0]) > 0;
    }

    public Object searchData(Records record, String dataset, Map<String, Object> params, Integer limit, Integer offset) {
        Records ds = record.getEnv().get("rpt.dataset").find(Criteria.equal("report_id", record.getId())
            .and("code", "=", dataset));
        if (!ds.any()) {
            throw new ValidationException(record.l10n("报表[%s]不存在数据源[%s]", record.get("present"), dataset));
        }
        String content = ds.getString("content");
        SqlTemplate template = SqlTemplateBuilder.getBuilder().createSqlTemplate(content);
        SqlFormat sql = template.process(params);
        Cursor cr = record.getEnv().getCursor();
        if (limit == null || limit < 1) {
            limit = 10;
        }
        int size = limit + 1;
        String querySql = cr.getSqlDialect().getPaging(sql.getSql(), size, offset);
        DataSet.validateSql(querySql);
        cr.execute(querySql, sql.getParmas());
        List<Map<String, Object>> list = cr.fetchMapAll();
        KvMap result = new KvMap();
        result.put("hasNext", list.size() > limit);
        if (list.size() > limit) {
            list.remove(list.size() - 1);
        }
        result.put("values", list);
        return result;
    }

    @ServiceMethod(auth = "update", label = "读取数据集字段", ids = false)
    public Object readDataSetColumns(Records record, String dataSetId) {
        Records dataSet = record.getEnv().get("rpt.dataset", dataSetId);
        return dataSet.call("readDataSetColumns");
    }


    @ServiceMethod(auth = "update", label = "读取数据集信息")
    public Object readDataSet(Records record, String ds) {
        Map<String, Object> result = new HashMap<>();
        Records dataSet = record.getEnv().get("rpt.dataset").find(Criteria.equal("report_id", record.getId()).and("code", "=", ds));
        if (dataSet.any()) {
            result.put("code", ds);
            result.put("name", dataSet.get("name"));
            result.put("columns", dataSet.call("readDataSetColumns"));
            String content = dataSet.getString("content");
            Collection<String> params = DataSet.extractParams(content);
            result.put("params", params);
        }
        return result;
    }

    @ServiceMethod(auth = "update", label = "执行SQL", ids = false)
    public Object executeSql(Records records, String code, Map<String, Object> params) {
        Cursor cr = records.getEnv().getCursor();
        SqlDialect sqlDialect = cr.getSqlDialect();
        SqlTemplate template = SqlTemplateBuilder.getBuilder().createSqlTemplate(code);
        SqlFormat sql = template.process(params);
        String querySql = sqlDialect.getPaging(sql.getSql(), 5, 0);
        DataSet.validateSql(querySql);
        Map<String, Object> result = new HashMap<>();
        Connection connection = null;
        try {
            connection = records.getEnv().getDatabase().getConnection();
            SqlFormat format = cr.getSqlFormat(querySql, sql.getParmas());
            PreparedStatement statement = connection.prepareStatement(format.getSql());
            int parameterIndex = 1;
            for (Object p : format.getParmas()) {
                statement.setObject(parameterIndex++, sqlDialect.prepareObject(p));
            }
            statement.execute();
            ResultSet resultSet = statement.getResultSet();
            result.put("columns", DataSet.getColumnMeta(resultSet, sqlDialect));
            result.put("values", fetchMapAll(resultSet, sqlDialect));
        } catch (Exception e) {
            result.put("error", ThrowableUtils.getCause(e).getMessage());
            result.put("debug", ThrowableUtils.getDebug(e));
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                }
            }
        }
        return result;
    }

    private static List<Map<String, Object>> fetchMapAll(ResultSet resultSet, SqlDialect sqlDialect) {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            ResultSetMetaData meta = resultSet.getMetaData();
            while (resultSet.next()) {
                Object[] values = new Object[meta.getColumnCount()];
                KvMap map = new KvMap(values.length);
                for (int i = 1; i <= values.length; i++) {
                    map.put(sqlDialect.getColumnLabel(meta.getColumnLabel(i)), resultSet.getObject(i));
                }
                list.add(map);
            }
        } catch (SQLException e) {
            throw new DataException("读取行失败", e);
        }
        return list;
    }
}
