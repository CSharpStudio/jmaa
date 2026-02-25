package jmaa.modules.wip.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "wip.defect", label = "产品缺陷")
@Model.Service(remove = "@edit")
public class WipDefect extends Model {
    static Field product_id = Field.Many2one("wip.production").label("产品");
    static Field process_id = Field.Many2one("md.work_process").label("工序");
    static Field qty = Field.Float().label("不良数量").defaultValue(0);
    static Field defect_id = Field.Many2one("md.defect").label("不良代码");
    static Field defect_type = Field.Selection().related("defect_id.type");
    static Field defect_grade = Field.Selection().related("defect_id.grade");
    static Field repair_ids = Field.One2many("wip.repair", "wip_defect_id").label("维修记录");
    static Field status = Field.Selection(new Options(){{
        put("new", "未维修");
        put("suspend", "挂起");
        put("done", "完成");
    }}).label("状态").defaultValue("new");
}
