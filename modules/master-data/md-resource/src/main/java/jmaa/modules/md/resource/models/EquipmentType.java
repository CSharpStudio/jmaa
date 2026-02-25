package jmaa.modules.md.resource.models;

import org.jmaa.sdk.*;

import java.util.LinkedHashMap;

@Model.Meta(name = "md.equipment_type", label = "设备类型")
@Model.UniqueConstraint(name = "name_category_unique", fields = {"name", "category"})
public class EquipmentType extends Model {
    static Field name = Field.Char().label("设备类型").required();
    static Field category = Field.Selection(new LinkedHashMap<String, String>() {{
        put("production", "生产设备");
        put("testing", "检测设备");
        put("auxiliary", "辅助设备");
        put("facility", "厂务设备");
        put("instrument", "仪器仪表");
        put("office", "办公设备");
        put("it", "IT设备");
    }}).required().label("设备类别").useCatalog();
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
}
