package jmaa.modules.inventory.models.report;

import org.jmaa.sdk.Criteria;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.Utils;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.data.Expression;
import org.jmaa.sdk.data.SqlFormat;
import org.jmaa.sdk.data.xml.SqlTemplate;
import org.jmaa.sdk.util.KvMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @Author: ljx
 * @Date: 2023-03-11  15:27
 */
@Model.Meta(name = "wms.warehouse_onhand_report", label = "仓库库存查询", inherit = "stock.onhand_report_base")
@Model.Service(remove = "@edit")
public class WarehouseOnhandReport extends ReportBase {
    @Override
    public long count(Records rec, Criteria criteria) {
        if (Utils.isEmpty(criteria)) {
            Criteria.in("a.company_id", rec.getEnv().getCompanies().getIds());
        }
        Cursor cursor = rec.getEnv().getCursor();
        SqlFormat where = Expression.toWhereSql(criteria);
        String condition = where.getSql();
        SqlTemplate template = rec.getEnv().getSql("warehouse_onhand_report_count");
        SqlFormat sql = template.process(new KvMap().set("WHERE", "WHERE " + condition));
        cursor.execute(sql.getSql(), where.getParmas());
        return Utils.toLong(cursor.fetchOne()[0]);
    }

    @Override
    public List<Map<String, Object>> search(Records rec, Collection<String> fields, Criteria criteria, Integer offset, Integer limit, String order) {
        if (Utils.isEmpty(criteria)) {
            Criteria.in("a.company_id", rec.getEnv().getCompanies().getIds());
        }
        Environment env = rec.getEnv();
        Cursor cursor = rec.getEnv().getCursor();
        SqlFormat where = Expression.toWhereSql(criteria);
        String condition = where.getSql();
        SqlTemplate template = env.getSql("warehouse_onhand_report");
        SqlFormat sql = template.process(new KvMap().set("WHERE", "WHERE " + condition));
        String querySql = cursor.getSqlDialect().getPaging(sql.getSql(), limit, offset);
        cursor.execute(querySql, where.getParmas());
        List<Map<String, Object>> onhandList = cursor.fetchMapAll();
        onhandList.stream().forEach(row -> {
            updateData(row);
            row.put("warehouse", getCodeName((String) row.get("warehouse_code"), (String) row.get("warehouse_name")));
        });
        return onhandList;
    }

}
