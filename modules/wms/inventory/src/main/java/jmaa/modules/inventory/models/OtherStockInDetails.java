package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;

@Model.Meta(name = "wms.other_stock_in_details", label = "其它入库扫码明细", table = "stock_stock_in_details", authModel = "wms.other_stock_in", inherit = "stock.stock_in_details")
public class OtherStockInDetails extends Model {
    static Field other_stock_in_id = Field.Many2one("wms.other_stock_in").label("其它入库单");

    @Override
    public boolean delete(Records records) {
        Environment env = records.getEnv();
        boolean lotInQtyFlag = env.getConfig().getBoolean("lot_in_qty");
        for (Records rec : records) {
            if ("done".equals(rec.getString("status"))) {
                throw new ValidationException(rec.l10n("选择的明细已入库,请检查数据"));
            }
            Records otherStockIn = rec.getRec("other_stock_in_id");
            Records material = rec.getRec("material_id");
            Records line = rec.getEnv().get("wms.other_stock_in_line").find(Criteria.equal("other_stock_in_id", otherStockIn.get("id")).and(Criteria.equal("material_id", material.get("id"))));
            line.set("scan_qty", Utils.round(line.getDouble("scan_qty") - rec.getDouble("qty")));
            line.set("status", "stocking");
            String sn = rec.getString("sn");
            Records materialLabel = rec.getEnv().get("lbl.material_label").find(Criteria.equal("sn", sn));
            if (materialLabel.any()) {
                materialLabel.set("status", "new");
                Map<String, Object> log = new HashMap<>();
                log.put("operation", "wms.other_stock_in:delete");
                log.put("related_id", otherStockIn.getId());
                log.put("related_code", otherStockIn.getString("code"));
                materialLabel.call("logStatus", log);
            }
            if (lotInQtyFlag && "lot".equals(material.getString("stock_rule"))){
                env.get("lbl.lot_status").find(Criteria.equal("order_id", otherStockIn.getId())
                    .and(Criteria.equal("type", "wms.other_stock_in"))
                    .and(Criteria.equal("lot_num", rec.getString("lot_num")))
                    .and(Criteria.equal("material_id",material.getId()))
                    .and(Criteria.equal("detail_id", rec.getId()))).delete();
            }
        }
        return (Boolean) records.callSuper(OtherStockInDetails.class, "delete");
    }
}
