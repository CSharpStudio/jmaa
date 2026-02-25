package jmaa.modules.sales.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

@Model.Meta(name = "sales.order", label = "销售订单", inherit = {"code.auto_code", "mixin.company", "mixin.order_status"})
public class SalesOrder extends Model {
    static Field code = Field.Char().label("销售订单号").unique();
    static Field customer_id = Field.Many2one("md.customer").label("客户编码").required();
    static Field customer_po = Field.Char().label("客户PO");
    static Field type = Field.Selection(new Options() {{
        put("common", "普通");
    }}).label("订单类型").required().defaultValue("common");
    static Field order_date = Field.Date().label("订单日期").required();
    static Field line_ids = Field.One2many("sales.order_line", "so_id").label("订单明细");
    static Field remark = Field.Char().label("备注");

    @Constrains("status")
    public void onStatusCommit(Records records) {
        for (Records record : records) {
            if ("commit".equals(record.getString("status"))) {
                Records lines = record.getRec("line_ids");
                if (!lines.any()) {
                    throw new ValidationException(records.l10n("订单[%s]没有订单明细,请添加订单明细", record.get("code")));
                }
            }
        }
    }
}
