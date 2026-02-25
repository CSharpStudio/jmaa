package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "qsd.mrb_inspect_item", label = "MRB检验项目", inherit = {"qsd.inspect_item", "mixin.ac_re"}, authModel = "qsd.mrb")
@Model.Service(remove = "@all")
public class MrbInspectItem extends ValueModel {
    static Field mrb_id = Field.Many2one("qsd.mrb").label("MRB单");
    static Field sample_size = Field.Integer().label("样本数").min(1);
    static Field test_values = Field.Char().label("测试值").help("记录样本的测试值").length(2000);
    static Field result = Field.Selection(new Options() {{
        put("ok", "合格");
        put("ng", "不合格");
    }}).label("检验结果");
    static Field ng_qty = Field.Integer().label("不良数").min(0);
}
