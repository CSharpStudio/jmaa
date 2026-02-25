package jmaa.modules.wip.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Selection;

@Model.Meta(name = "wip.trace_process", label = "生产工序", order = "create_date", authModel = "wip.trace_production", inherit = "wip.process")
@Model.Service(remove = "@edit")
public class TraceProcess extends Model {
    static Field product_id = Field.Many2one("wip.trace_production");
    static Field status = Field.Selection(Selection.related("wip.process", "status")).label("状态");
    static Field op_ids = Field.Many2many("md.staff", "wip_trace_process_staff", "process_id", "staff_id").label("操作员");
}
