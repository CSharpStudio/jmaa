package jmaa.modules.account.models;

import org.jmaa.sdk.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Model.Meta(inherit = "purchase.order_line")
public class PurchaseOrderLine extends Model {
    static Field price = Field.Float().label("单价").compute("computePrice");
    static Field tax_rate = Field.Float().label("税率(%)");
    static Field price_tax = Field.Float().label("含税单价");
    static Field total_price = Field.Float().label("总价");

    public Double computePrice(Records record) {
        return Utils.round(record.getDouble("price_tax") / (1 + record.getDouble("tax_rate") / 100));
    }

    @ServiceMethod(auth = "read", label = "读取采购价格", ids = false)
    public Object readPrice(Records records, String supplierId, String type, String materialId, double qty) {
        Records prices = records.getEnv().get("purchase.price_details").find(
            Criteria.equal("purchase_price_id.supplier_id", supplierId).and("purchase_price_id.type", "=", type)
                .and("material_id", "=", materialId).and("status", "=", "approve")
                .and("min", "<=", qty).and("max", ">=", qty)
                .and("begin_date", "<=", new Date()).and("end_date", ">=", new Date()));
        Map<String, Object> result = new HashMap<>();
        if (prices.any()) {
            prices = prices.first();
            double priceTax = prices.getDouble("price_tax");
            result.put("price", prices.get("price"));
            result.put("tax_rate", prices.get("tax_rate"));
            result.put("price_tax", prices.get("price_tax"));
            result.put("total_price", Utils.round(priceTax * qty));
            Records currency = prices.getRec("purchase_price_id").getRec("currency_id");
            result.put("accuracy", currency.get("accuracy"));
            result.put("price_accuracy", currency.get("price_accuracy"));
        }
        return result;
    }
}
