package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;
@Model.Meta(name = "qsd.quality_class", label = "质量分类")
public class QualityClass extends Model {
    static Field name = Field.Char().label("名称").required().unique();
    static Field remark = Field.Char().label("备注");
    static Field active = Field.Boolean().defaultValue(true).label("是否有效");
    static Field material_ids = Field.One2many("md.material", "quality_class_id").label("物料");
}
