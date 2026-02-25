package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.data.Cursor;

/**
 * @author eric
 */
@Model.Meta(name = "mfg.material_issue_dialog", label = "发料对话框", authModel = "mfg.material_issue")
public class MaterialIssueDialog extends ValueModel {
    static Field warehouse_id = Field.Many2one("md.warehouse").label("发料仓库").lookup("searchWarehouse");
    static Field location_id = Field.Many2one("md.store_location").label("发料库位");
    static Field material_id = Field.Many2one("md.material").label("物料编码").readonly();
    static Field stock_rule = Field.Selection().related("material_id.stock_rule");
    static Field template_id = Field.Many2one("print.template").label("标签模板");

    public Criteria searchWarehouse(Records rec) {
        return Criteria.equal("user_ids", rec.getEnv().getUserId());
    }

    @ActionMethod
    public Action onWarehouseChange(Records record) {
        AttrAction action = new AttrAction();
        Records warehouse = record.getRec("warehouse_id");
        Records material = record.getRec("material_id");
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
        } else {
            action.setValue("onhand_qty", 0);
        }
        return action;
    }
}
