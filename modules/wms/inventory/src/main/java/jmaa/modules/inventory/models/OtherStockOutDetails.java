package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;

@Model.Meta(name = "wms.other_stock_out_details", label = "其它出库-扫码明细", table = "stock_stock_out_details", authModel = "wms.other_stock_out", inherit = "stock.stock_out_details")
public class OtherStockOutDetails extends Model {
    static Field other_stock_out_id = Field.Many2one("wms.other_stock_out").label("其它出库单");

    @Override
    public boolean delete(Records records) {
        Environment env = records.getEnv();
        for (Records rec : records) {
            if ("done".equals(rec.getString("status"))) {
                throw new ValidationException(rec.l10n("选择的明细已出库,请检查数据"));
            }
            Records otherStockOut = rec.getRec("other_stock_out_id");
            Records material = rec.getRec("material_id");
            double qty = rec.getDouble("qty");
            Records line = rec.getEnv().get("wms.other_stock_out_line").find(Criteria.equal("other_stock_out_id", otherStockOut.getId()).and(Criteria.equal("material_id", material.get("id"))));
            line.set("scan_qty", Utils.round(line.getDouble("scan_qty") - qty));
            line.set("status", "delivering");
            Records warehouse = rec.getRec("warehouse_id");
            String stockRule = material.getString("stock_rule");
            Records location = rec.getRec("location_id");
            String lotNum = rec.getString("lot_num");
            // 在库的删除
            // 获取对应的物料明细,将数量返回
            if ("sn".equals(stockRule)) {
                // 序列号 这种要处理库存标签
                Records materialLabel = rec.getRec("label_id");
                Records stockOnhand = env.get("stock.onhand").find(Criteria.equal("label_id", materialLabel.getId()));
                if (stockOnhand.any()) {
                    stockOnhand.set("status", "onhand");
                    materialLabel.set("status", "onhand");
                    double allotQty = stockOnhand.getDouble("allot_qty");
                    stockOnhand.set("usable_qty", allotQty);
                    stockOnhand.set("ok_qty", allotQty);
                    stockOnhand.set("allot_qty", 0);
                }
                Map<String, Object> log = new HashMap<>();
                log.put("operation", "wms.other_stock_out:delete");
                log.put("related_id", otherStockOut.getId());
                log.put("related_code", otherStockOut.getString("code"));
                materialLabel.call("logStatus", log);
            } else if ("lot".equals(stockRule)) {
                Records stockOnhand = env.get("stock.onhand").find(Criteria.equal("lot_num", lotNum)
                    .and(Criteria.equal("material_id",material.getId()))
                    .and(Criteria.equal("warehouse_id", warehouse.getId()))
                    .and(Criteria.equal("location_id", location.getId()))
                    .and(Criteria.equal("label_id", null)));
                stockOnhand.set("usable_qty", Utils.round(stockOnhand.getDouble("usable_qty") + qty));
                stockOnhand.set("allot_qty", Utils.round(stockOnhand.getDouble("allot_qty") - qty));
                if ("allot".equals(stockOnhand.getString("status"))) {
                    stockOnhand.set("status", "onhand");
                }
                if (env.getConfig().getBoolean("lot_out_qty")) {
                    env.get("lbl.lot_status").find(Criteria.equal("order_id", otherStockOut.getId())
                        .and(Criteria.equal("type", "wms.other_stock_out"))
                        .and(Criteria.equal("lot_num", lotNum))
                        .and(Criteria.equal("material_id",material.getId()))
                        .and(Criteria.equal("detail_id", rec.getId()))).delete();
                }
            } else {
                Records stockOnhand = env.get("stock.onhand").find(Criteria.equal("warehouse_id", warehouse.getId())
                    .and(Criteria.equal("material_id", material.getId()))
                    .and(Criteria.equal("location_id", location.getId())));
                stockOnhand.set("usable_qty", Utils.round(stockOnhand.getDouble("usable_qty") + qty));
                stockOnhand.set("allot_qty", Utils.round(stockOnhand.getDouble("allot_qty") - qty));
                if ("allot".equals(stockOnhand.getString("status"))) {
                    stockOnhand.set("status", "onhand");
                }
            }
        }
        return (Boolean) records.callSuper(OtherStockOutDetails.class, "delete");
    }
}
