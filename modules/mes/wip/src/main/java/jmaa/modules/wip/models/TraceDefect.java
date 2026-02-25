package jmaa.modules.wip.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Selection;

@Model.Meta(name = "wip.trace_defect", label = "产品缺陷", authModel = "wip.trace_production", inherit = "wip.defect")
@Model.Service(remove = "@edit")
public class TraceDefect extends Model {
    static Field product_id = Field.Many2one("wip.trace_production");
    static Field repair_ids = Field.One2many("wip.trace_repair", "defect_id").label("维修记录");
    static Field status = Field.Selection(Selection.related("wip.defect", "status")).label("状态").defaultValue("new");
}
