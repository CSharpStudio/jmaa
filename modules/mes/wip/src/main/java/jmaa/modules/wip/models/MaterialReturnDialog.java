package jmaa.modules.wip.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.ValueModel;

@Model.Meta(name = "mfg.material_return_dialog", label = "生产退料对话框", authModel = "mfg.material_return")
public class MaterialReturnDialog extends ValueModel {
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库").store(false);
    static Field material_id = Field.Many2one("md.material").store(false).label("物料编码").readonly();
    static Field stock_rule = Field.Selection().related("material_id.stock_rule");
    static Field print_flag = Field.Boolean().label("打印标签").store(false).defaultValue(false);
    static Field template_id = Field.Many2one("print.template").label("标签模板").store(false);
}
