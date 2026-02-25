package jmaa.modules.wip.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Selection;

@Model.Meta(name = "wip.trace_repair", label = "维修记录", authModel = "wip.trace_production", inherit = "wip.repair")
@Model.Service(remove = "@edit")
public class TraceRepair extends Model {
    static Field product_id = Field.Many2one("wip.trace_production");
    static Field wip_defect_id = Field.Many2one("wip.trace_defect");
    static Field defect_cause = Field.Selection(Selection.related("wip.repair", "defect_cause"));
    static Field status = Field.Selection(Selection.related("wip.repair", "status"));
    static Field result = Field.Selection(Selection.related("wip.repair", "result"));
}
