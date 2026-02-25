package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "qsd.aql_custom", label = "自定义AQL抽样标准", order = "name,limit_lower", inherit = "mixin.ac_re", authModel = "qsd.sampling_plan")
@Model.UniqueConstraint(name = "aql_unique", fields = {"plan_id", "name", "limit_lower", "limit_upper"})
public class AqlCustom extends Model {
    static Field name = Field.Char().label("AQL").required();
    static Field strictness = Field.Selection(new Options() {{
        put("normal", "正常");
        put("tightened", "加严");
        put("reduced", "放宽");
    }}).label("严格度").defaultValue("normal");
    static Field limit_lower = Field.Integer().label("批量下限").required().greaterThen(0);
    static Field limit_upper = Field.Integer().label("批量上限").required().greaterThen(0);
    static Field sample_size = Field.Integer().label("样本量").required().greaterThen(0);
    static Field ac = Field.Integer().required();
    static Field plan_id = Field.Many2one("qsd.sampling_plan").label("抽样方案").ondelete(DeleteMode.Cascade);
}
