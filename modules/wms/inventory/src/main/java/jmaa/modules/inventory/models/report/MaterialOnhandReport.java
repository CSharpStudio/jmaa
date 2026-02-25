package jmaa.modules.inventory.models.report;

import org.jmaa.sdk.*;
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
 * @Date: 2023-03-11  13:57
 */
@Model.Meta(name = "wms.material_onhand_report", label = "物料库存查询", inherit = "stock.onhand_report_base")
@Model.Service(remove = "@edit")
public class MaterialOnhandReport extends ReportBase {

    static Field stock_rule = Field.Selection().label("库存规则").related("material_id.stock_rule");

    @Override
    public long count(Records rec, Criteria criteria) {
        if (Utils.isEmpty(criteria)) {
            Criteria.in("a.company_id", rec.getEnv().getCompanies().getIds());
        }
        Cursor cursor = rec.getEnv().getCursor();
        SqlFormat where = Expression.toWhereSql(criteria);
        String condition = where.getSql();
        SqlTemplate template = rec.getEnv().getSql("material_onhand_report_count");
        SqlFormat sql = template.process(new KvMap().set("WHERE", "WHERE " + condition));
        cursor.execute(sql.getSql(), where.getParmas());
        return Utils.toLong(cursor.fetchOne()[0]);
    }

    @Override
    public List<Map<String, Object>> search(Records rec, Collection<String> fields, Criteria criteria, Integer offset,
                                            Integer limit, String order) {
        if (Utils.isEmpty(criteria)) {
            Criteria.in("a.company_id", rec.getEnv().getCompanies().getIds());
        }
        Environment env = rec.getEnv();
        Cursor cursor = env.getCursor();
        SqlFormat where = Expression.toWhereSql(criteria);
        String condition = where.getSql();
        SqlTemplate template = env.getSql("material_onhand_report");
        SqlFormat sql = template.process(new KvMap().set("WHERE", "WHERE " + condition));
        String querySql = cursor.getSqlDialect().getPaging(sql.getSql(), limit, offset);
        cursor.execute(querySql, where.getParmas());
        List<Map<String, Object>> onhandList = cursor.fetchMapAll();
        onhandList.stream().forEach(row -> {
            updateData(row);
        });
        return onhandList;
    }
}
