package jmaa.modules.md.packing.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(name = "md.packing_rule", label = "包装规则", present = {"code", "name"}, presentFormat = "{code}({name})")
public class PackingRule extends Model {
    static Field code = Field.Char().label("规则编码").required().unique();
    static Field name = Field.Char().label("规则名称").required();
    static Field description = Field.Char().label("规则描述");
    static Field level_ids = Field.One2many("md.packing_level", "rule_id").label("包装层级");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
}
