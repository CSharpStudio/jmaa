package jmaa.modules.label.manager.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(name = "lbl.label_split", label = "标签拆分记录", authModel = "lbl.material_label")
@Model.Service(remove = "@edit")
public class LabelSplit extends Model {
    static Field old_label_id = Field.Many2one("lbl.material_label").label("原标签").help("被拆分的原标签").required();
    static Field old_qty = Field.Float().label("原数量").help("原标签数量").required();
    static Field new_label_id = Field.Many2one("lbl.material_label").label("新标签").help("新标签id").required();
    static Field new_qty = Field.Float().label("新数量").help("新标签数量");
}
