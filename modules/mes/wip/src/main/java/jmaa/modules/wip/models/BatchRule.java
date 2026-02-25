package jmaa.modules.wip.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "wip.batch_rule", label = "批次规则", present = {"name", "qty"}, presentFormat = "{name} (×{qty})")
public class BatchRule extends Model {
    static Field name = Field.Char().label("名称").required();
    static Field qty = Field.Float().label("数量").required().greaterThen(0d);
    static Field template_id = Field.Many2one("print.template").label("打印模板").required();
    static Field coding_id = Field.Many2one("code.coding").label("编码规则").required();
}
