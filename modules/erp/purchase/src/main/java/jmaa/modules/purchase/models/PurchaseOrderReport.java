package jmaa.modules.purchase.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.ValueModel;

/**
 * 采购明细视图模型，扩展采购订单相关字段，用于查询采购入库的详情
 *
 * @author eric
 */

@Model.Meta(name = "purchase.order_report", label = "采购入库查询", inherit = "purchase.order_line", table = "purchase_order_line", order = "po_id,line_no")
@Model.Service(remove = "@edit")
public class PurchaseOrderReport extends ValueModel {
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商").related("po_id.supplier_id");
    static Field type = Field.Selection().label("订单类型").related("po_id.type");
    static Field order_date = Field.Date().label("订单日期").related("po_id.order_date");
    static Field company_id = Field.Many2one("res.company").label("组织").related("po_id.company_id");
    static Field order_status = Field.Selection().label("订单状态").related("po_id.status");
}
