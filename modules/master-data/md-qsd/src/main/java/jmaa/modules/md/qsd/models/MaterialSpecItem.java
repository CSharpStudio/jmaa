package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "qsd.material_spec_item", label = "料号检验项目", inherit = "qsd.inspect_item", authModel = "qsd.material_spec")
public class MaterialSpecItem extends Model {
    static Field mode = Field.Selection(Selection.related("qsd.quality_class_spec_item", "mode")).label("检验方式").required();
    static Field spec_id = Field.Many2one("qsd.material_spec").label("料号检验标准").ondelete(DeleteMode.Cascade);
    static Field category = Field.Selection(Selection.related("qsd.inspect_item", "category")).useCatalog(false);
}
