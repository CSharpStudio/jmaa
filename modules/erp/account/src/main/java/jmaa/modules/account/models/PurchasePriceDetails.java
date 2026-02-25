package jmaa.modules.account.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "purchase.price_details", label = "采购价目明细", inherit = "mixin.material", authModel = "purchase.price")
public class PurchasePriceDetails extends Model {
    static Field purchase_price_id = Field.Many2one("purchase.price").label("采购价目");
    static Field min = Field.Float().label("最小数量").defaultValue(0);
    static Field max = Field.Float().label("最大数量").defaultValue(999999);
    static Field price = Field.Float().label("单价").compute("computePrice");
    static Field tax_rate = Field.Float().label("税率(%)").min(0d).required();
    static Field price_tax = Field.Float().label("含税单价").required().greaterThen(0d);
    static Field begin_date = Field.Date().label("生效日期").required();
    static Field end_date = Field.Date().label("失效日期").required();
    static Field status = Field.Selection(new Options() {{
        put("new", "新建");
        put("approve", "已审核");
        put("reject", "驳回");
        put("close", "关闭");
    }}).label("状态").defaultValue("new");

    public Double computePrice(Records record) {
        return Utils.round(record.getDouble("price_tax") / (1 + record.getDouble("tax_rate") / 100));
    }

    @ServiceMethod(label = "审核")
    public Object approve(Records records) {
        records.set("status", "approve");
        return Action.success();
    }

    @ServiceMethod(label = "反审核")
    public Object reject(Records records) {
        records.set("status", "reject");
        return Action.success();
    }

    @ServiceMethod(label = "关闭")
    public Object close(Records records) {
        records.set("status", "close");
        return Action.success();
    }
}
