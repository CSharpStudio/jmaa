package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "qsd.aql_table", label = "AQL表格", order = "name", authModel = "qsd.sampling_plan")
@Model.UniqueConstraint(name = "aql_unique", fields = {"plan_id", "name", "strictness"})
public class AqlTable extends Model {
    static Field name = Field.Char().label("AQL").required();
    static Field strictness = Field.Selection(new Options() {{
        put("normal", "正常");
        put("tightened", "加严");
        put("reduced", "放宽");
    }}).label("严格度").defaultValue("normal");
    static Field plan_id = Field.Many2one("qsd.sampling_plan").label("抽样方案").ondelete(DeleteMode.Cascade);
    static Field a = Field.Integer().label("A").required();
    static Field b = Field.Integer().label("B").required();
    static Field c = Field.Integer().label("C").required();
    static Field d = Field.Integer().label("D").required();
    static Field e = Field.Integer().label("E").required();
    static Field f = Field.Integer().label("F").required();
    static Field g = Field.Integer().label("G").required();
    static Field h = Field.Integer().label("H").required();
    static Field j = Field.Integer().label("J").required();
    static Field k = Field.Integer().label("K").required();
    static Field l = Field.Integer().label("L").required();
    static Field m = Field.Integer().label("M").required();
    static Field n = Field.Integer().label("N").required();
    static Field p = Field.Integer().label("P").required();
    static Field q = Field.Integer().label("Q").required();
    static Field r = Field.Integer().label("R").required();
    static Field s = Field.Integer().label("S");
}
