package jmaa.modules.wip.models;

import org.jmaa.sdk.BoolState;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Selection;

@Model.Meta(name = "wip.trace_code", label = "采集条码", logAccess = BoolState.False, authModel = "wip.trace_production", inherit = "wip.code")
@Model.Service(remove = "@edit")
public class TraceCode extends Model {
    static Field product_id = Field.Many2one("wip.trace_production");
    static Field code_type = Field.Selection(Selection.related("md.work_process_step", "code_type"));
}
