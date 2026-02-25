package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

@Model.Service(remove = "@edit")
@Model.Meta(name = "wms.sales_stock_out_mobile", label = "销售出库-移动端", inherit = "wms.sales_stock_out", table = "wms_sales_stock_out")
public class SalesStockOutMobile extends Model {
}
