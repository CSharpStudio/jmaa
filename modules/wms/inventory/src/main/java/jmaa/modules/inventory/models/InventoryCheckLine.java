package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;

import java.util.LinkedHashMap;

@Model.Meta(name = "wms.inventory_check_line", label = "盘点单物料明细", authModel = "wms.inventory_check", inherit = "mixin.material")
public class InventoryCheckLine extends Model {
    static Field inventory_check_id = Field.Many2one("wms.inventory_check").label("盘点单");
    static Field blind = Field.Boolean().label("盲盘").related("inventory_check_id.blind").store(false);
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库");
    static Field blind_qty = Field.Char().label("*数量").compute("computeBlindQty").store(false);
    static Field qty = Field.Float().label("账面数量").readonly().defaultValue(0d);
    static Field first_qty = Field.Float().label("初盘数量").readonly().defaultValue(0d);
    static Field second_qty = Field.Float().label("复盘数量").readonly().defaultValue(0d);
    static Field diff_qty = Field.Float().label("差异数量").compute("computeDiffQty").readonly();
    static Field first_sn_num = Field.Integer().label("已初盘标签数量").defaultValue(0);
    static Field second_sn_num = Field.Integer().label("已复盘标签数量").defaultValue(0);
    static Field stock_rule = Field.Selection().related("material_id.stock_rule");
    static Field abc_type = Field.Selection().label("ABC分类").related("material_id.abc_type");
    static Field category = Field.Selection().label("物料类型").related("material_id.category");
    static Field status = Field.Selection(new LinkedHashMap<String, String>() {{
        put("create", "创建");
        put("first_running", "初盘中");
        put("first_done", "初盘完成");
        put("second_running", "复盘中");
        put("second_done", "复盘完成");
        put("done", "已完成");
    }}).label("状态").defaultValue("create");
    static Field inventory_balance_id = Field.Many2one("wms.inventory_balance").label("平账单");

    @Model.ActionMethod
    public Action onMaterialChange(Records record) {
        AttrAction action = Action.attr();
        Records material = record.getRec("material_id");
        action.setValue("material_name_spec", material.get("name_spec"));
        action.setValue("category", material.get("category"));
        action.setValue("abc_type", material.get("abc_type"));
        action.setValue("style", material.get("style"));
        action.setValue("stock_rule", material.get("stock_rule"));
        return action;
    }

    @OnSaved("first_qty")
    public void onFirstQty(Records records) {
        for (Records record : records) {
            String status = record.getString("status");
            Double first_qty = record.getDouble("first_qty");
            if ("create".equals(status) && Utils.large(first_qty, 0d)) {
                record.set("status", "first_running");
            }
        }
    }

    @OnSaved("second_qty")
    public void onSecondQty(Records records) {
        for (Records record : records) {
            String status = record.getString("status");
            Double second_qty = record.getDouble("second_qty");
            if ("first_done".equals(status) && Utils.large(second_qty, 0d)) {
                record.set("status", "second_running");
            }
        }
    }

    public String computeBlindQty(Records record) {
        boolean aBoolean = record.getBoolean("blind");
        if (aBoolean) {
            return "***";
        }
        return Utils.toString(record.get("qty"));
    }

    public Double computeDiffQty(Records rec) {
        Double physicalCountQty = rec.getDouble("qty");
        Double firstCountQty = rec.getDouble("first_qty");
        Double secondCountQty = rec.getDouble("second_qty");
        if (Utils.isNotEmpty(secondCountQty) && Utils.large(secondCountQty, 0d)) {
            return Utils.round(secondCountQty - physicalCountQty);
        }
        // 有复盘用复盘,复盘没返回,找初盘
        if (Utils.isNotEmpty(firstCountQty) && Utils.large(firstCountQty, 0d)) {
            return Utils.round(firstCountQty - physicalCountQty);
        }
        return 0d;
    }
}
