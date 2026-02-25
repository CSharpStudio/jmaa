package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.BinaryOp;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author eric
 */
@Model.Meta(name = "wms.sales_return_line", label = "销售退货物料明细", authModel = "wms.sales_return", inherit = "mixin.material")
public class SalesReturnLine extends Model {
    static Field return_id = Field.Many2one("wms.sales_return").label("销售退货");
    static Field so_id = Field.Many2one("sales.order").label("销售订单");
    static Field status = Field.Selection(new Options(){{
        put("new", "新建");
        put("returning", "收货中");
        put("returned", "已备齐");
        put("done", "已完成");
    }}).label("行状态").readonly(true).defaultValue("new");
    static Field request_qty = Field.Float().label("需退货数量").required().min(0D);
    static Field return_qty = Field.Float().label("已扫码数量").required().defaultValue(0);

    @OnSaved("request_qty")
    public void onRequestQtySaved(Records records) {
        for (Records record : records) {
            Double requestQty = record.getDouble("request_qty");
            if (Utils.less(requestQty, 0)) {
                throw new ValidationException(record.l10n("物料[%s]退货数量必须大于0", record.getRec("material_id").get("code")));
            }
            Double returnedQty = record.getDouble("return_qty");
            if(Utils.less(requestQty, returnedQty)){
                throw new ValidationException(record.l10n("物料[%s]退货数量不能小于已退数量[%s]", record.getRec("material_id").get("code"), returnedQty));
            }
            if (Utils.largeOrEqual(returnedQty, requestQty)) {
                record.set("status", "returned");
            }
        }
    }

    @OnSaved("return_qty")
    public void onReturnedQtySaved(Records records) {
        for (Records record : records) {
            Double returnedQty = record.getDouble("return_qty");
            Double requestQty = record.getDouble("request_qty");
            if (Utils.largeOrEqual(returnedQty, requestQty)) {
                record.set("status", "returned");
            } else if (Utils.large(returnedQty, 0)) {
                record.set("status", "returning");
            }
        }
    }

    @Override
    public Map<String, Object> searchByField(Records rec, String relatedField, Criteria criteria, Integer offset, Integer limit, Collection<String> fields, String order) {
        if("material_id".equals(relatedField)){
            Criteria soCriteria = new Criteria();
            for (Object item : criteria) {
                if (item instanceof BinaryOp) {
                    BinaryOp binary = (BinaryOp) item;
                    if ("so_id".equals(binary.getField())) {
                        soCriteria.add(binary);
                        criteria.remove(binary);
                        break;
                    }
                }
            }
            if(!soCriteria.isEmpty()){
                Records lines = rec.getEnv().get("sales.order_line").find(soCriteria);
                Set<String> materialIds = lines.stream().map(r->r.getRec("material_id").getId()).collect(Collectors.toSet());
                criteria.and(Criteria.in("id", materialIds));
            }
        }
        return (Map<String, Object>)callSuper(rec, relatedField, criteria, offset, limit, fields, order);
    }
}
