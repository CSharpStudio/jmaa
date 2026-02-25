package jmaa.modules.md.packing.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.UniqueConstraint(name = "unique_packing_level", fields = {"rule_id", "packing_level"}, message = "同一个包装规则下层级不能重复!")
@Model.Meta(name = "md.packing_level", label = "包装层级", order = "seq", authModel = "md.packing_rule")
public class PackingLevel extends Model {
    static Field seq = Field.Integer().min(1).label("序号").required().help("由内到外的包装序号");
    static Field name = Field.Char().label("名称").compute("computeName");
    static Field packing_level = Field.Selection(new Options(){{
        put("pallet", "栈板");
        put("carton", "箱");
        put("box", "盒");
    }}).label("包装层级").required().useCatalog();
    static Field package_qty = Field.Float().label("包装规格").help("包装内主单位总数量").required().greaterThen(0d);
    static Field coding_id = Field.Many2one("code.coding").label("编码规则").required();
    static Field print_template_id = Field.Many2one("print.template").label("标签模板").required();
    static Field rule_id = Field.Many2one("md.packing_rule").label("包装规则").ondelete(DeleteMode.Cascade);
    static Field is_in = Field.Boolean().label("收料");
    static Field is_out = Field.Boolean().label("出库");

    public String computeName(Records record){
        return String.format("%s(*%s)",  record.getSelection("packing_level"), record.get("package_qty"));
    }
}
