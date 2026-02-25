package jmaa.modules.md.resource.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;

@Model.Meta(name = "md.equipment_model", label = "设备型号")
public class EquipmentModel extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称");
    static Field specification = Field.Char().label("技术规格");
    static Field manufacturer = Field.Char().label("生产厂商");
    static Field type_id = Field.Many2one("md.equipment_type").label("设备类型").required();
    static Field type_category = Field.Selection().related("type_id.category").label("设备类别");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);

    @ActionMethod
    public Action onTypeChange(Records record) {
        AttrAction action = new AttrAction();
        Records type = record.getRec("type_id");
        action.setValue("type_category", type.get("category"));
        return action;
    }
}
