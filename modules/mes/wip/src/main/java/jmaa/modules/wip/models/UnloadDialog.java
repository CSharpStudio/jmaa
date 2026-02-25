package jmaa.modules.wip.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "mfg.unload_dialog", label = "下料对话框", inherit = "mixin.material")
public class UnloadDialog extends ValueModel {
    static Field station_id = Field.Many2one("md.work_station").label("工位").required().readonly();
    static Field lot_num = Field.Char().label("物料批次号").readonly();
    static Field qty = Field.Float().label("工位数量").readonly();
}
