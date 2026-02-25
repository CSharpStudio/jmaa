package jmaa.modules.md.inventory.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.stock_class", label = "物料仓储分类", inherit = "mixin.companies")
public class StockClass extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称").required();
    static Field company_ids = Field.Many2many("res.company", "md_stock_class_company", "class_id", "company_id");
    static Field remark = Field.Char().label("备注");
    static Field active = Field.Boolean().defaultValue(true).label("是否有效");
    static Field store_limit = Field.Selection(new Options() {{
        put("none", "任意");
        put("warehouse", "仓库");
        put("location", "库位");
    }}).defaultValue("none").label("存储位置");
    static Field user_ids = Field.Many2many("rbac.user", "md_stock_class_user", "class_id", "user_id").label("仓管员");
    static Field material_ids = Field.One2many("md.material", "stock_class_id").label("物料");
    static Field warehouse_ids = Field.Many2many("md.warehouse", "md_stock_class_warehouse", "class_id", "warehouse_id").label("仓库");
    static Field store_location_ids = Field.Many2many("md.store_location", "md_stock_class_location", "class_id", "store_location_id").label("库位");
}
