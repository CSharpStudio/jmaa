package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

@Model.Meta(name = "wms.transfer_order_details", label = "仓库调拨物料明细", authModel = "wms.transfer_order", table = "stock_stock_out_details", inherit = "stock.stock_out_details")
public class TransferOrderDetails extends Model {
    static Field transfer_order_id = Field.Many2one("wms.transfer_order").label("调拨单号");

    @ServiceMethod(auth = "delete", label = "删除已扫描的标签")
    public void deleteDetails(Records records) {
        Environment env = records.getEnv();
        // 可能删除多条
        for (Records rec : records) {
            Records stockOutId = rec.getRec("stock_out_id");
            if (stockOutId.any()) {
                throw new ValidationException(rec.l10n("当前标签已执行出库,不能删除"));
            }
            Records material = rec.getRec("material_id");
            String stockRule = material.getString("stock_rule");
            double qty = rec.getDouble("qty");
            Records transferOrder = rec.getRec("transfer_order_id");
            if ("sn".equals(stockRule)) {
                // 标签变为正常
                Records labelId = rec.getRec("label_id");
                updateTransferQtyBySn(rec, labelId, qty, material, transferOrder);
            } else if ("lot".equals(stockRule)) {
                String lotNum = rec.getString("lot_num");
                Records warehouse = rec.getRec("warehouse_id");
                Records location = rec.getRec("location_id");
                updateTransferQtyByLotAndNum(rec, lotNum, material, warehouse, location, qty, transferOrder);
                if (env.getConfig().getBoolean("lot_out_qty")) {
                    env.get("lbl.lot_status").find(Criteria.equal("order_id", transferOrder.getId())
                        .and(Criteria.equal("type", "wms.transfer_order"))
                        .and(Criteria.equal("lot_num", lotNum)).and(Criteria.equal("detail_id",rec.getId()))).delete();
                }
            } else {
                Records warehouse = rec.getRec("warehouse_id");
                Records location = rec.getRec("location_id");
                updateTransferQtyByLotAndNum(rec, null, material, warehouse, location, qty, transferOrder);
            }
            rec.delete();
        }
    }

    public void updateTransferQtyByLotAndNum(Records record, String lotNum, Records material, Records warehouse, Records location, double qty, Records transferOrder) {
        Records stockOnhand = record.getEnv().get("stock.onhand");
        Records transferOrderLine = record.getEnv().get("wms.transfer_order_line");
        stockOnhand = stockOnhand.find(
            Criteria.equal("material_id", material.getId())
                .and(Criteria.equal("lot_num", lotNum))
                .and(Criteria.equal("warehouse_id", warehouse.getId()))
                .and(Criteria.equal("location_id", location.getId())));
        double allotQty = stockOnhand.getDouble("allot_qty");
        double usableQty = stockOnhand.getDouble("usable_qty");
        stockOnhand.set("allot_qty", allotQty - qty);
        stockOnhand.set("usable_qty", usableQty + qty);
        transferOrderLine = transferOrderLine.find(Criteria.equal("material_id", material.getId())
            .and(Criteria.equal("transfer_order_id", transferOrder.getId())));
        double transferQty = transferOrderLine.getDouble("transfer_qty");
        transferOrderLine.set("transfer_qty", Utils.round(transferQty - qty));
        if ("transfered".equals(transferOrderLine.getString("status"))) {
            transferOrderLine.set("status", "transfering");
        }
    }

    public void updateTransferQtyBySn(Records record, Records label, Double qty, Records material, Records transferOrder) {
        Records stockOnhand = record.getEnv().get("stock.onhand");
        Records transferOrderLine = record.getEnv().get("wms.transfer_order_line");
        stockOnhand = stockOnhand.find(Criteria.equal("label_id", label.getId()));
        stockOnhand.set("allot_qty", 0);
        stockOnhand.set("usable_qty", qty);
        stockOnhand.set("status", "onhand");
        transferOrderLine = transferOrderLine.find(Criteria.equal("material_id", material.getId())
            .and(Criteria.equal("transfer_order_id", transferOrder.getId())));
        double transferQty = transferOrderLine.getDouble("transfer_qty");
        transferOrderLine.set("transfer_qty", Utils.round(transferQty - qty));
        if ("transfered".equals(transferOrderLine.getString("status"))) {
            transferOrderLine.set("status", "transfering");
        }
    }

}
