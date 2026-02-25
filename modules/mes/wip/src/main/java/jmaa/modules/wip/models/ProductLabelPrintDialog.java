package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;

@Model.Meta(name = "lbl.product_label_print_dialog", inherit = "lbl.material_label_print_dialog")
public class ProductLabelPrintDialog extends ValueModel {
    static Field work_order_id = Field.Many2one("mfg.work_order").label("工单");

    @ActionMethod
    public Object onWorkOrderChange(Records record) {
        AttrAction action = Action.attr();
        Records wo = record.getRec("work_order_id");
        Records material = wo.getRec("material_id");
        Records unit = material.getRec("unit_id");
        action.setAttr("min_packages", "data-decimals", unit.getInteger("accuracy"));
        action.setAttr("print_qty", "data-decimals", unit.getInteger("accuracy"));
        action.setValue("unit_id", unit);
        action.setValue("material_id", material);
        action.setValue("material_name_spec", material.getString("name_spec"));
        action.setValue("template_id", material.getRec("print_tpl_id"));
        action.setValue("min_packages", material.getDouble("min_packages"));
        action.setValue("material_category", material.get("category"));
        action.setValue("stock_rule", material.get("stock_rule"));
        //TODO 客户
        return action;
    }
}
