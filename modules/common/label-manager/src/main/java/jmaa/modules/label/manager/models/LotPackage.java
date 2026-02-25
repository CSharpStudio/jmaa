package jmaa.modules.label.manager.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "md.lot_package", label = "批次包装", inherit = "mixin.material")
public class LotPackage extends Model {
    static Field lot_num = Field.Char().label("批次号").required();
    static Field package_id = Field.Many2one("packing.package").label("包装标签编码").required();
    static Field package_code = Field.Char().label("LPN").related("package_id.code");
    static Field qty = Field.Float().label("数量").defaultValue(0).required();
}
