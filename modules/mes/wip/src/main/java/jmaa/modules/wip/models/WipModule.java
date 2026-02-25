package jmaa.modules.wip.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "wip.module", label = "装配件", inherit = "mixin.material")
@Model.Service(remove = "@edit")
public class WipModule extends Model {
    static Field product_id = Field.Many2one("wip.production").label("产品");
    static Field process_id = Field.Many2one("md.work_process").label("工序");
    static Field qty = Field.Float().label("用料数量");
    static Field lot_num = Field.Char().label("物料批次号");
    static Field code = Field.Char().label("组件条码");
}
