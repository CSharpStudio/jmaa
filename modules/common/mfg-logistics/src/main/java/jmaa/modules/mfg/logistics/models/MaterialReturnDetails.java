package jmaa.modules.mfg.logistics.models;

import org.jmaa.sdk.*;

import java.util.HashMap;
import java.util.Map;

@Model.Meta(name = "mfg.material_return_details", label = "退料明细", authModel = "mfg.material_return", table = "stock_stock_in_details", inherit = "stock.stock_in_details")
public class MaterialReturnDetails extends Model {
    static Field material_return_id = Field.Many2one("mfg.material_return").label("退料单");

    @Override
    public boolean delete(Records records) {
        Records materialReturn = records.first().getRec("material_return_id");
        // 删除前,处理一下日志
        for (Records rec : records) {
            Records label = rec.getRec("label_id");
            if (label.any()) {
                Map<String, Object> log = new HashMap<>();
                log.put("operation", "mfg.material_return:delete");
                log.put("related_id", materialReturn.getId());
                log.put("related_code", materialReturn.getString("code"));
                label.call("logStatus", log);
            }
        }
        Boolean returnFlag = (Boolean) records.callSuper(MaterialReturnDetails.class, "delete");
        Records detailsIds = materialReturn.getRec("details_ids");
        if (!detailsIds.any()){
            materialReturn.set("status","draft");
        }
        return returnFlag;
    }
}
