package jmaa.modules.wip.models;

import org.jmaa.sdk.BoolState;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Selection;

@Model.Meta(name = "wip.trace_production", table = "wip_production", label = "产品履历", logAccess = BoolState.False, inherit = {"wip.production"})
@Model.Service(remove = "@edit")
public class TraceProduction extends Model {
    static Field process_ids = Field.One2many("wip.trace_process", "product_id");
    static Field code_ids = Field.One2many("wip.trace_code", "product_id");
    static Field status = Field.Selection(Selection.related("wip.production", "status"));
    static Field defect_ids = Field.One2many("wip.trace_defect", "product_id");
    static Field repair_ids = Field.One2many("wip.trace_repair", "product_id");
    static Field module_ids = Field.One2many("wip.trace_module", "product_id");
}
