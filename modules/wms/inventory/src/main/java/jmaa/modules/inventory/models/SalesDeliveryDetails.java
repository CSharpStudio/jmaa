package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author eric
 */
@Model.Meta(name = "wms.sales_delivery_details", label = "销售出库明细", table = "stock_stock_out_details", authModel = "wms.sales_delivery", inherit = "stock.stock_out_details")
public class SalesDeliveryDetails extends Model {
    static Field delivery_id = Field.Many2one("wms.sales_delivery").label("销售发货");

    @ServiceMethod(label = "删除", auth = "delete")
    public void deleteDetails(Records records) {
        Records salesDelivery = records.first().getRec("delivery_id");
        Environment env = records.getEnv();
        Records salesDeliveryLine = env.get("wms.sales_delivery_line");
        for (Records rec : records) {
            if (rec.getRec("stock_out_id").any()) {
                throw new ValidationException(records.l10n("标签[%s]已生成出库单,不能删除",rec.getString("sn")));
            }
            Records material = rec.getRec("material_id");
            Records line = salesDeliveryLine.find(Criteria.equal("delivery_id", salesDelivery.getId()).and(Criteria.equal("material_id", material.getId())));
            line.getDouble("request_qty");
            double deliveredQty = line.getDouble("delivered_qty");
            double qty = rec.getDouble("qty");
            deliveredQty = Utils.round(deliveredQty - qty);
            line.set("delivered_qty", deliveredQty);
            line.set("status", "delivering");
            Records materialLabel = env.get("lbl.material_label");
            materialLabel = materialLabel.find(Criteria.equal("material_id", material.getId()).and(Criteria.equal("sn", rec.getString("sn"))));
            if (materialLabel.any()){
                Map<String, Object> log = new HashMap<>();
                log.put("operation", "wms.sales_delivery:delete");
                log.put("related_id", salesDelivery.getId());
                log.put("related_code", salesDelivery.getString("code"));
                materialLabel.call("logStatus", log);
            }
            // 查库存  现在只管sn标签
            Records stockOnhand = env.get("stock.onhand").find(Criteria.equal("sn", rec.getString("sn")));
            stockOnhand.set("status", "onhand");
            // 可用数
            stockOnhand.set("usable_qty", rec.get("qty"));
            // 分配数
            stockOnhand.set("allot_qty", 0);
            rec.delete();
        }
    }
}
