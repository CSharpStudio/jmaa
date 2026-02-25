package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author eric
 */
@Model.Meta(name = "wms.sales_return_details", label = "销售退货明细", table = "stock_stock_in_details", authModel = "wms.sales_return", inherit = "stock.stock_in_details")
public class SalesReturnDetails extends Model {
    static Field return_id = Field.Many2one("wms.sales_return").label("销售退货");

    @Override
    public boolean delete(Records records) {
        for (Records rec : records) {
            Records salesReturn = rec.getRec("return_id");
            Records material = rec.getRec("material_id");
            Records line = rec.getEnv().get("wms.sales_return_line").find(Criteria.equal("return_id", salesReturn.get("id")).and(Criteria.equal("material_id", material.get("id"))));
            line.set("return_qty", Utils.round(line.getDouble("return_qty") - rec.getDouble("qty")));
            line.set("status","returning");
            String sn = rec.getString("sn");
            Records materialLabel = rec.getEnv().get("lbl.material_label").find(Criteria.equal("sn", sn));
            if (materialLabel.any()) {
                Map<String, Object> log = new HashMap<>();
                log.put("operation", "wms.sales_return:delete");
                log.put("related_id", salesReturn.getId());
                log.put("related_code", salesReturn.getString("code"));
                materialLabel.call("logStatus", log);
            }
        }
        return (Boolean) records.callSuper(SalesReturnDetails.class, "delete");
    }
}
