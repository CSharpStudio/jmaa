package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.data.Cursor;

@Model.Meta(name = "wms.transfer_order_dialog", label = "调拨对话框", authModel = "wms.transfer_order")
public class TransferOrderDialog extends ValueModel {
    static Field warehouse_id = Field.Many2one("md.warehouse").label("调拨仓库").store(false);
    static Field location_id = Field.Many2one("md.store_location").label("调拨库位").store(false);
    static Field material_id = Field.Many2one("md.material").store(false).label("物料编码").readonly();
    static Field unit_accuracy = Field.Integer().related("material_id.unit_id.accuracy").label("单位精度").readonly();
    static Field stock_rule = Field.Selection().related("material_id.stock_rule");
    static Field print_flag = Field.Boolean().label("打印新标签").store(false).defaultValue(false);
    static Field template_id = Field.Many2one("print.template").label("标签模板").store(false);

    @ActionMethod
    public Action onWarehouseChange(Records record) {
        AttrAction action = new AttrAction();
        Records warehouse = record.getRec("warehouse_id");
        Records material = record.getRec("material_id");
        Records unit = material.getRec("unit_id");
        action.setValue("unit_accuracy", unit.get("accuracy"));
        Records onhand = record.getEnv().get("stock.onhand");
        Cursor cr = record.getEnv().getCursor();
        if (warehouse.any() && material.any()) {
            onhand = (Records) record.getEnv().get("mfg.material_issue").call("findOnhandByRule", material, warehouse);
        }
        action.setValue("suggest_location", onhand.getRec("location_id").get("code"));
        if ("sn".equals(material.getString("stock_rule"))) {
            action.setValue("suggest_sn", onhand.get("sn"));
        } else {
            action.setValue("suggest_sn", onhand.get("lot_num"));
        }
        if (onhand.any()) {
            cr.execute("select sum(usable_qty) from stock_onhand where status='onhand' and material_id=%s and warehouse_id=%s",
                Utils.asList(material.getId(), warehouse.getId()));
            action.setValue("onhand_qty", cr.fetchOne()[0]);
            action.setValue("location_id",onhand.getRec("location_id"));
        } else {
            action.setValue("onhand_qty", 0);
            action.setValue("location_id",null);
        }
        return action;
    }
}
