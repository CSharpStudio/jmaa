package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author eric
 */
@Model.Meta(name = "wms.return_supplier_details", label = "退供应商明细", table = "stock_stock_out_details", authModel = "wms.return_supplier", inherit = "stock.stock_out_details")
public class ReturnSupplierDetails extends Model {
    static Field return_id = Field.Many2one("wms.return_supplier").label("退供应商");

    @ServiceMethod(auth = "delete", label = "删除已扫描的标签")
    public void deleteDetails(Records records) {
        Environment env = records.getEnv();
        // 可能删除多条
        for (Records record : records) {
            String status = record.getString("status");
            if ("done".equals(status)) {
                throw new ValidationException(record.l10n("明细已出库,不能删除"));
            }
            Records returnSupplier = record.getRec("return_id");
            String returnId = returnSupplier.getId();
            String returnSupplierCode = returnSupplier.getString("code");
            Records warehouse = record.getRec("warehouse_id");
            Records material = record.getRec("material_id");
            String stockRule = material.getString("stock_rule");
            double qty = record.getDouble("qty");
            if (warehouse.any()) {
                Records location = record.getRec("location_id");
                String lotNum = record.getString("lot_num");
                // 在库的删除
                // 获取对应的物料明细,将退货数量返回
                if ("sn".equals(stockRule)) {
                    // 序列号 这种要处理库存标签
                    Records materialLabel = record.getRec("label_id");
                    Records stockOnhand = env.get("stock.onhand").find(Criteria.equal("label_id", materialLabel.getId()));
                    // 挑选过来的标签,状态已经改了,不能让他删除,手动建单, 标签只能是在库,才能扫码了,其他情况不考虑了,太多了, 不然每个删除的地方还要记录历史状态
                    if (stockOnhand.any()) {
                        stockOnhand.set("status", "onhand");
                        materialLabel.set("status", "onhand");
                        double allotQty = stockOnhand.getDouble("allot_qty");
                        stockOnhand.set("usable_qty", allotQty);
                        stockOnhand.set("ok_qty", allotQty);
                        stockOnhand.set("allot_qty", 0);
                    }
                    Map<String, Object> log = new HashMap<>();
                    log.put("operation", "wms.return_supplier:delete");
                    log.put("related_id", returnId);
                    log.put("related_code", returnSupplierCode);
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

            Records returnSupplierLine = env.get("wms.return_supplier_line")
                .find(Criteria.equal("return_id", returnId).and(Criteria.equal("material_id", material.getId())));
            // 存在多条相同物料记录
            for (Records line : returnSupplierLine) {
                double returnQty = line.getDouble("return_qty");
                // 正常应该处理状态,但是 "wms.return_supplier_line"模型中 @OnSaved("return_qty") 会处理,这里去掉
                // returnSupplierLine.set("status","returning");
                if (Utils.large(returnQty, qty)) {
                    line.set("return_qty", Utils.round(returnQty - qty));
                    line.set("status", "returning");
                    qty = 0d;
                } else {
                    // 一个标签对应到多条物料明细
                    line.set("return_qty", 0);
                    qty = Utils.round(qty - returnQty);
                    line.set("status", "new");
                }
                if (Utils.equals(qty, 0d)) {
                    break;
                }
            }
            if (env.getConfig().getBoolean("lot_in_qty") && "lot".equals(stockRule)) {
                env.get("lbl.lot_status").find(Criteria.equal("order_id", returnSupplier.getId())
                    .and(Criteria.equal("type", "wms.return_supplier"))
                    .and(Criteria.equal("material_id",material.getId()))
                    .and(Criteria.equal("lot_num", record.getString("lot_num"))) ).delete();
            }
            record.delete();
        }
    }
}
