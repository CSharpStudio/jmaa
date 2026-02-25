package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;

@Model.Meta(name = "wms.inventory_check_details", label = "盘点单标签明细", authModel = "wms.inventory_check", inherit = {"mixin.material", "mixin.material_label"})
public class InventoryCheckDetails extends Model {
    static Field inventory_check_id = Field.Many2one("wms.inventory_check").label("盘点单");
    static Field blind = Field.Boolean().label("盲盘").related("inventory_check_id.blind").store(false);
    static Field qty = Field.Float().label("数量").defaultValue(0d);
    static Field blind_qty = Field.Char().label("*数量").compute("computeBlindQty").store(false);
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库");
    static Field location_id = Field.Many2one("md.store_location").label("库位");
    static Field area_id = Field.Many2one("md.store_area").label("库区").related("location_id.area_id");
    static Field lot_num = Field.Char().label("物料批次号");
    static Field material_stock_rule = Field.Selection().related("material_id.stock_rule").label("库存规则");
    static Field first_qty = Field.Float().label("初盘数量").readonly();
    static Field second_qty = Field.Float().label("复盘数量").readonly();
    static Field diff_qty = Field.Float().label("差异数量").compute("computeDiffQty").readonly();
    static Field status = Field.Selection(new Options() {{
        put("create", "草稿");
        put("first_running", "初盘中");
        put("first_done", "初盘完成");
        put("second_running", "复盘中");
        put("second_done", "复盘完成");
        put("done", "完成");
    }}).label("状态").defaultValue("create").readonly(true);
    static Field inventory_balance_id = Field.Many2one("wms.inventory_balance").label("平账单");

    public String computeBlindQty(Records record){
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
            return  Utils.round(secondCountQty - physicalCountQty) ;
        }
        // 有复盘用复盘,复盘没返回,找初盘
        if (Utils.isNotEmpty(firstCountQty) && Utils.large(firstCountQty, 0d)) {
            return  Utils.round(firstCountQty - physicalCountQty)  ;
        }
        return 0d;
    }

    @Override
    public boolean delete(Records records) {
        Environment env = records.getEnv();
        Records inventoryCheckLine = env.get("wms.inventory_check_line");
        Records inventoryCheck = records.first().getRec("inventory_check_id");
        for (Records rec : records) {
            Records material = rec.getRec("material_id");
            inventoryCheckLine = inventoryCheckLine.find(Criteria.equal("material_id", material.getId())
                .and(Criteria.equal("inventory_check_id", inventoryCheck.getId())));
            // 必有 且唯一,多个就异常
            inventoryCheckLine.ensureOne();
            inventoryCheckLine.set("first_qty", Utils.round(inventoryCheckLine.getDouble("first_qty") - rec.getDouble("first_qty")));
            // 状态不变了, 盘点完成就控制按钮
        }
        return (Boolean) records.callSuper(InventoryCheckDetails.class, "delete");
    }
}
