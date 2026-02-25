package jmaa.modules.md.inventory.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.store_area", label = "库区", inherit = {"mixin.company"}, authModel = "md.warehouse", order = "id asc")
@Model.UniqueConstraint(name = "code_warehouse_unique", fields = {"code", "warehouse_id"})
public class StoreArea extends Model {
    static Field code = Field.Char().label("库区编码").required(true);
    static Field description = Field.Char().label("描述");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库").ondelete(DeleteMode.Cascade).required();
}
