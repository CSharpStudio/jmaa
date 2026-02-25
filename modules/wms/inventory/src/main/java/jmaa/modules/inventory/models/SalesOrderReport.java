package jmaa.modules.inventory.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.ValueModel;

/**
 * 采购入库查询
 *
 * @author eric
 */

@Model.Meta(name = "wms.sales_order_report", label = "销售出库查询", inherit = "sales.order_line", table = "sales_order_line", order = "so_id,line_no")
@Model.Service(remove = "@edit")
public class SalesOrderReport extends ValueModel {
    static Field customer_id = Field.Many2one("md.customer").label("客户").related("so_id.customer_id");
    static Field type = Field.Selection().label("订单类型").related("so_id.type");
    static Field order_date = Field.Date().label("订单日期").related("so_id.order_date");
    static Field company_id = Field.Many2one("res.company").label("组织").related("so_id.company_id");
    static Field order_status = Field.Selection().label("订单状态").related("so_id.status");
}
