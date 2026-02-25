package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.data.Cursor;

/**
 * @author eric
 */
@Model.Meta(name = "wms.sales_delivery_dialog", label = "发货对话框", authModel = "wms.sales_delivery")
public class SalesDeliveryDialog extends ValueModel {
    static Field material_id = Field.Many2one("md.material").label("物料编码").readonly();
    static Field unit_id = Field.Many2one("md.unit").label("单位").related("material_id.unit_id");
    static Field material_name_spec = Field.Char().label("名称规格").related("material_id.name_spec");
    static Field stock_rule = Field.Selection().label("库存规则").related("material_id.stock_rule").help("数量管控物料，输入数量发货。序列号管控物料，通过扫描条码进行发货");
    static Field request_qty = Field.Float().label("需求数量").readonly();
    static Field deficit_qty = Field.Float().label("待发数量").readonly();
    static Field commit_qty = Field.Float().label("发货数量").readonly();
    static Field warehouse_id = Field.Many2one("md.warehouse").label("发料仓库").required().lookup("searchWarehouse");
    static Field location_id = Field.Many2one("md.store_location").label("库位");
    static Field auto_confirm = Field.Boolean().label("自动确认");
    static Field onhand_qty = Field.Float().label("库存数量").readonly();
    static Field sn = Field.Char().label("条码");
    static Field message = Field.Text().label("提示信息");

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
            onhand = (Records) record.getEnv().get("wms.sales_delivery").call("findOnhandByRule", material, warehouse);
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
