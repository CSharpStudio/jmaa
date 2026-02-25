package jmaa.modules.md.inventory.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(name = "md.store_location", label = "库位", authModel = "md.warehouse", order = "code asc")
@Model.UniqueConstraint(name = "code_warehouse_unique", fields = {"code", "warehouse_id"})
public class StoreLocation extends Model {
    static Field code = Field.Char().label("库位编码").required(true);
    static Field description = Field.Char().label("描述");
    static Field shelf_code = Field.Char().label("货架编码");
    static Field capacity = Field.Integer().label("库位容量");
    static Field area_id = Field.Many2one("md.store_area").label("库区").ondelete(DeleteMode.SetNull);
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库").ondelete(DeleteMode.Cascade).required();
    static Field material_ids = Field.Many2many("md.material", "md_material_location", "store_location_id", "material_id").label("物料编码");

    @Override
    public String computePresent(Records rec) {
        Records wh = rec.getRec("warehouse_id");
        return wh.get("present") + "/" + rec.get("code");
    }
}
