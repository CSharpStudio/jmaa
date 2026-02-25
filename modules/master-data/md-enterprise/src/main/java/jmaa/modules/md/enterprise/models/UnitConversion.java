package jmaa.modules.md.enterprise.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;

@Model.Meta(name = "md.unit_conversion", label = "单位转换", authModel = "md.unit")
@Model.UniqueConstraint(name = "unique_unit_conversion", fields = {"base_id", "derived_id"})
public class UnitConversion extends Model {
    static Field base_id = Field.Many2one("md.unit").label("主单位").required();
    static Field derived_id = Field.Many2one("md.unit").label("衍生单位").required();
    static Field ratio = Field.Float().label("转换比例").required().help("比例=1衍生单位/1主单位");
    static Field derived_type = Field.Selection().related("derived_id.type").label("衍生单位类型").required();

    @ActionMethod
    public Action onDerivedChange(Records record) {
        Records derived = record.getRec("derived_id");
        AttrAction action = new AttrAction();
        if (derived.any()) {
            action.setValue("derived_type", derived.get("type"));
        } else {
            action.setValue("derived_type", null);
        }
        return action;
    }
}
