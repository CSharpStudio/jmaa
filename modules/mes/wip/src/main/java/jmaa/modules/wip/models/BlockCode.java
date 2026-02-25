package jmaa.modules.wip.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "wip.block_code", label = "拼板条码")
@Model.UniqueConstraint(name = "code_label_unique", fields = {"code", "label_id"})
@Model.Service(remove = "@edit")
public class BlockCode extends Model {
    static Field code = Field.Char().label("编码").index().required();
    static Field label_id = Field.Many2one("lbl.product_label").label("产品标签").required();
    static Field product_code = Field.Char().label("生产条码").related("label_id.sn");
}
