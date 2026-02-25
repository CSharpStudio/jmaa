package jmaa.modules.inventory.models.report;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.data.Expression;
import org.jmaa.sdk.data.SqlFormat;
import org.jmaa.sdk.data.xml.SqlTemplate;
import org.jmaa.sdk.util.KvMap;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 库存明细报表
 */
@Model.Meta(name = "wms.onhand_report", label = "库存明细查询", inherit = "stock.onhand_report_base")
@Model.Service(remove = "@edit")
public class OnhandReport extends ReportBase {
    static Field sn = Field.Char().label("序列号");
    static Field location = Field.Char().label("库位");
    static Field lot_num = Field.Char().label("物料批次");
    static Field product_lot = Field.Char().label("生产批次");
    static Field product_date = Field.Date().label("生产日期");
    static Field quality_status = Field.Selection(Selection.related("lbl.material_label", "quality_status")).label("质量状态");
    static Field status = Field.Selection(Selection.related("stock.onhand", "status")).label("状态");

    @Override
    public long count(Records rec, Criteria criteria) {
        if (Utils.isEmpty(criteria)) {
            Criteria.in("a.company_id", rec.getEnv().getCompanies().getIds());
        }
        Cursor cursor = rec.getEnv().getCursor();
        SqlFormat where = Expression.toWhereSql(criteria);
        String condition = where.getSql();
        SqlTemplate template = rec.getEnv().getSql("onhand_report_count");
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
        SqlTemplate template = env.getSql("onhand_report");
        SqlFormat sql = template.process(new KvMap().set("WHERE", "WHERE " + condition));
        String querySql = cursor.getSqlDialect().getPaging(sql.getSql(), limit, offset);
        cursor.execute(querySql, where.getParmas());
        List<Map<String, Object>> onhandList = cursor.fetchMapAll();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        onhandList.stream().forEach(row -> {
            updateData(row);
            Date dt = (Date) row.get("product_date");
            String productionDate = dt == null ? "" : format.format(dt);
            row.put("product_date", productionDate);
            row.put("warehouse", getCodeName((String) row.get("warehouse_code"), (String) row.get("warehouse_name")));
        });
        return onhandList;
    }

    @ServiceMethod(label = "查找库存物料", auth = "read")
    public Map<String, Object> findMaterialByCode(Records records, @Doc("物料条码") String code) {
        return (Map<String, Object>) records.getEnv().get("wms.location_move").call("findMaterialByCode", code);
    }

    @ServiceMethod(label = "库位移动")
    public Object move(Records records, @Doc("物料条码") String code, @Doc("仓库id") String warehouseId, @Doc("源库位") String locationCodeSource, @Doc("库位条码") String locationCodeTarget, @Doc("数量") double qty) {
        return records.getEnv().get("wms.location_move").call("move", code, warehouseId, locationCodeSource, locationCodeTarget, qty);
    }
}
