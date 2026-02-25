package jmaa.modules.wip.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "wip.trace_module", label = "装配件", authModel = "wip.trace_production", inherit = "wip.module")
@Model.Service(remove = "@edit")
public class TraceModule extends Model {
    static Field product_id = Field.Many2one("wip.trace_production");
}
