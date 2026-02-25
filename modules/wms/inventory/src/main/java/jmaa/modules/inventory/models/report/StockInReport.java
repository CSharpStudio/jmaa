package jmaa.modules.inventory.models.report;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.BinaryOp;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.data.Expression;
import org.jmaa.sdk.data.SqlFormat;
import org.jmaa.sdk.data.xml.SqlTemplate;
import org.jmaa.sdk.fields.SelectionField;
import org.jmaa.sdk.util.KvMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Model.Meta(name = "stock.in_report", label = "待入库数据报表")
public class StockInReport extends ReportBase {
    static Field material_id = Field.Many2one("md.material").label("物料编码");
    static Field material_code = Field.Char().label("物料编码");
    static Field name_spec = Field.Char().label("名称规格");
    static Field iqc_code = Field.Char().label("来料检验单");
    static Field stock_in_code = Field.Char().label("入库单号");
    static Field product_stock_in_code = Field.Char().label("成品入库");
    static Field material_return_code = Field.Char().label("退料单");
    static Field receipt_code = Field.Char().label("收料单");
    static Field return_code = Field.Char().label("销售退货");
    static Field code = Field.Char().label("入库单号");
    static Field type = Field.Char().label("类型");
    static Field qty = Field.Float().label("数量");
    static Field return_qty = Field.Float().label("退货数量");
    static Field should_stock_qty = Field.Float().label("应入库数量");
    static Field stock_qty = Field.Float().label("已入库数量");
    static Field wait_stock_qty = Field.Float().label("待入库数量");
    static Field details_ids = Field.One2many("stock.in_details_report", "stock_in_report_id");

    @Override
    public List<Map<String, Object>> search(Records rec, Collection<String> fields, Criteria criteria, Integer offset, Integer limit, String order) {
        if (Utils.isEmpty(criteria)) {
            criteria = Criteria.in("base.company_id", rec.getEnv().getCompanies().getIds());
        }
        criteria.and(Criteria.in("base.status", Utils.asList("stocking", "new")).and(Criteria.equal("detail.status", "to-stock")));
        Environment env = rec.getEnv();
        Cursor cursor = rec.getEnv().getCursor();
        SqlFormat where = Expression.toWhereSql(criteria);
        String condition = where.getSql();
        SqlTemplate template = env.getSql("stock_in_report");
        SqlFormat sql = template.process(new KvMap().set("WHERE", "WHERE " + condition));
        String querySql = cursor.getSqlDialect().getPaging(sql.getSql(), limit, offset);
        cursor.execute(querySql, where.getParmas());
        List<Map<String, Object>> onhandList = cursor.fetchMapAll();
        Records StockIn = rec.getEnv().get("stock.stock_in");
        SelectionField sf = (SelectionField) StockIn.getMeta().getField("type");
        onhandList.forEach(e -> {
                e.put("name_spec", getNameSpec((String) e.get("material_name"), (String) e.get("spec")));
                e.put("type", sf.getOptions(StockIn).get(e.get("type")));
            }
        );
        return onhandList;
    }

    @Override
    public List<Map<String, Object>> read(Records rec, Collection<String> fields) {
        // 入参的id, 是stock_in_details 的id
        List<Map<String, Object>> search = search(rec, fields, Criteria.equal("detail.id", rec.getId()), 0, 1, null);
        // 查数量
        Records stockInDetail = rec.getEnv().get("stock.stock_in_details",rec.getId());
        Criteria criteria = Criteria.equal("detail.material_id", stockInDetail.getRec("material_id").getId())
            .and(Criteria.equal("detail.stock_in_id", stockInDetail.getRec("stock_in_id").getId()))
            .and(Criteria.equal("detail.status", "to-stock"));
        Cursor cursor = rec.getEnv().getCursor();
        SqlFormat where = Expression.toWhereSql(criteria);
        String condition = where.getSql();
        SqlTemplate template = rec.getEnv().getSql("stock_in_report_material_qty");
        SqlFormat sql = template.process(new KvMap().set("WHERE", "WHERE " + condition));
        cursor.execute(sql.getSql(), where.getParmas());
        search.get(0).put("qty", cursor.fetchOne()[0]);
        return search;
    }

    @Override
    public Map<String, Object> searchByField(Records rec, String relatedField, Criteria criteria, Integer offset, Integer limit, Collection<String> fields, String order) {
        String detailId = null;
        for (Object item : criteria) {
            BinaryOp binaryOp = (BinaryOp) item;
            Object field = binaryOp.getField();
            if ("stock_in_report_id".equals(field)) {
                criteria.remove(binaryOp);
                detailId = (String) binaryOp.getValue();
                break;
            }
        }
        if (Utils.isEmpty(detailId)) {
            // 列表查询
            return (Map<String, Object>) rec.callSuper(StockInReport.class, "searchByField", relatedField, criteria, offset, limit, fields, order);
        } else {
            // 查详情,进去查子表,才有这个值
            Records stockInDetail = rec.getEnv().get("stock.stock_in_details", detailId);
            criteria.and(Criteria.equal("material_id", stockInDetail.getRec("material_id").getId())
                .and(Criteria.equal("stock_in_id", stockInDetail.getRec("stock_in_id").getId())))
                .and(Criteria.equal("status", "to-stock"));
            return (Map<String, Object>) rec.getEnv().get("stock.stock_in").call("searchByField", relatedField, criteria, offset, limit, fields, order);
        }
    }

    @Override
    public long count(Records rec, Criteria criteria) {
        if (Utils.isEmpty(criteria)) {
            Criteria.in("base.company_id", rec.getEnv().getCompanies().getIds());
        }
        criteria.and(Criteria.equal("base.status", "stocking")).and(Criteria.equal("detail.status", "to-stock"));
        Cursor cursor = rec.getEnv().getCursor();
        SqlFormat where = Expression.toWhereSql(criteria);
        String condition = where.getSql();
        SqlTemplate template = rec.getEnv().getSql("stock_in_report_count");
        SqlFormat sql = template.process(new KvMap().set("WHERE", "WHERE " + condition));
        cursor.execute(sql.getSql(), where.getParmas());
        return Utils.toLong(cursor.fetchOne()[0]);
    }
}
