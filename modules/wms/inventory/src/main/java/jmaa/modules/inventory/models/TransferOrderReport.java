package jmaa.modules.inventory.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.ValueModel;

@Model.Meta(name = "wms.transfer_order_report", label = "调拨明细查询", table = "wms_transfer_order_line", inherit = "wms.transfer_order_line")
public class TransferOrderReport extends ValueModel {
    static Field code = Field.Char().related("transfer_order_id.code");
    static Field target_warehouse_id =  Field.Many2one("md.warehouse").related("transfer_order_id.target_warehouse_id");
    static Field source_warehouse_ids = Field.Many2many("md.warehouse", "wms_transfer_warehouse", "transfer_id", "warehouse_id").related("transfer_order_id.source_warehouse_ids");
    static Field required_time = Field.DateTime().related("transfer_order_id.required_time");
}
