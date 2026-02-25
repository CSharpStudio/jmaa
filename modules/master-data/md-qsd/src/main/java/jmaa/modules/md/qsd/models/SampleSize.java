package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;


@Model.Meta(name = "qsd.sample_size", label = "样本量", order = "letter", authModel = "qsd.sampling_plan")
@Model.UniqueConstraint(name = "sample_unique", fields = {"plan_id", "letter", "strictness"})
public class SampleSize extends Model {
    static Field letter = Field.Char().label("字码").required().length(1);
    static Field size = Field.Integer().label("样本量").required().greaterThen(0);
    static Field strictness = Field.Selection(Selection.related("qsd.aql_table", "strictness")).label("严格度").defaultValue("normal");
    static Field plan_id = Field.Many2one("qsd.sampling_plan").label("抽样方案").ondelete(DeleteMode.Cascade);
}
