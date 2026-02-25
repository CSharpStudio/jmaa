package jmaa.modules.label.manager.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;

@Model.Meta(name = "lbl.material_label_print_dialog", label = "标签打印", inherit = "mixin.material")
public class MaterialLabelPrintDialog extends ValueModel {
    static Field stock_rule = Field.Selection().related("material_id.stock_rule");
    static Field template_id = Field.Many2one("print.template").label("标签模板").required();
    static Field min_packages = Field.Float().label("标签数量").required().help("每张标签的标准数量");
    static Field print_qty = Field.Float().label("打印数量").required().help("需要打印的物料总数量");
    static Field label_count = Field.Integer().label("标签张数").readonly().help("标签张数=打印数量/标签数量");
    static Field product_date = Field.Date().label("生产日期").required();
    static Field product_lot = Field.Char().label("生产批次");
    static Field lot_attr = Field.Char().label("批次属性");
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商").required();
    static Field customer_id = Field.Many2one("md.customer").label("客户");
    static Field lpn = Field.Char().label("最外层标签条码");

    @ActionMethod
    public Action onMaterialChange(Records rec) {
        AttrAction action = Action.attr();
        Records material = rec.getRec("material_id");
        Records unit = material.getRec("unit_id");
        action.setAttr("min_packages", "data-decimals", unit.getInteger("accuracy"));
        action.setAttr("print_qty", "data-decimals", unit.getInteger("accuracy"));
        action.setValue("unit_id", unit);
        action.setValue("material_name_spec", material.getString("name_spec"));
        action.setValue("template_id", material.getRec("print_tpl_id"));
        action.setValue("min_packages", material.getDouble("min_packages"));
        action.setValue("material_category", material.get("category"));
        action.setValue("stock_rule", material.get("stock_rule"));
        action.setVisible("lot_attr", !"num".equals(material.get("stock_rule")));
        return action;
    }
}
