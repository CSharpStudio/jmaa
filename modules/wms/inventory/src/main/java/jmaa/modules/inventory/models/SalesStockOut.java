package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(name = "wms.sales_stock_out", label = "销售出库", inherit = {"code.auto_code", "mixin.company", "mixin.order_status"})
public class SalesStockOut extends Model {
    static Field code = Field.Char().label("单号").readonly();
    static Field customer_id = Field.Many2one("md.customer").label("客户").required();
    static Field details_ids = Field.One2many("wms.sales_stock_out_details", "sales_stock_out_id").label("退货明细");
    static Field remark = Field.Char().label("备注");
    @ServiceMethod(label = "审核")
    public Object approve(Records records, @Doc(doc = "审核意见") String comment) {
        callSuper(records, comment);
        //TODO 扣减库存
        records.set("status", "done");
        return Action.reload(records.l10n("审核成功"));
    }
}
