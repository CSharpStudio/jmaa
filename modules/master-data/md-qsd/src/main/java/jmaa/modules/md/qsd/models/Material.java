package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = "md.material")
public class Material extends Model {
    static Field quality_class_id = Field.Many2one("qsd.quality_class").label("质量分类");
}
