package jmaa.modules.stock.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "stock.onhand", label = "库存", inherit = {"mixin.material", "mixin.material_label"})
@Model.Service(remove = "@edit")
public class Onhand extends ValueModel {
    static Field lot_num = Field.Char().label("物料批次号");
    static Field usable_qty = Field.Float().label("可用数");
    static Field ok_qty = Field.Float().label("合格数").defaultValue(0).required();
    static Field ng_qty = Field.Float().label("不合格数").defaultValue(0).required();
    static Field allot_qty = Field.Float().label("分配数").defaultValue(0).required();
    static Field frozen_qty = Field.Float().label("冻结数").defaultValue(0).required();
    static Field location_id = Field.Many2one("md.store_location").label("库位");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库").required();
    static Field company_id = Field.Many2one("res.company").label("组织");
    static Field type_id = Field.Many2one("md.material_type").related("material_id.type_id");
    static Field abc_type = Field.Selection().label("ABC分类").related("material_id.abc_type");
    static Field status = Field.Selection(new Options(){{
        put("onhand", "在库");
        put("allot", "已分配");
        put("frozen", "冻结");
    }}).label("状态").defaultValue("onhand");
    static Field total_qty = Field.Float().label("总数量").compute("computeTotalQty");
    static Field stock_in_time = Field.DateTime().label("入库时间");
    public Double computeTotalQty(Records rec) {
        return rec.getDouble("ok_qty") + rec.getDouble("ng_qty");
    }
}
