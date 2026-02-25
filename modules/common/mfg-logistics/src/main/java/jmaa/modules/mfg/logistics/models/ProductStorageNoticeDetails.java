package jmaa.modules.mfg.logistics.models;

import org.jmaa.sdk.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author eric
 */
@Model.Meta(name = "mfg.product_storage_notice_details", label = "成品入库通知明细", table = "stock_stock_in_details", authModel = "mfg.product_storage_notice", inherit = "stock.stock_in_details")
public class ProductStorageNoticeDetails extends Model {
    static Field product_storage_notice_id = Field.Many2one("mfg.product_storage_notice").label("成品入库通知");

    @ServiceMethod(label = "删除", auth = "delete")
    public Object deleteDetails(Records records) {
        Records productStockInId = records.first().getRec("product_storage_notice_id");
        double scanQty = productStockInId.getDouble("scan_qty");
        for (Records record : records) {
            Records label = record.getRec("label_id");
            if (label.any()){
                Records productStockIn = record.getRec("product_storage_notice_id");
                Map<String, Object> log = new HashMap<>();
                log.put("operation", "mfg.product_storage_notice:delete");
                log.put("related_id", productStockIn.getId());
                log.put("related_code", productStockIn.getString("code"));
                label.call("logStatus", log);
            }
            double qty = record.getDouble("qty");
            scanQty = Utils.round(scanQty-qty);
            record.delete();
        }
        productStockInId.set("scan_qty", scanQty);
        return scanQty;
    }
}
