package jmaa.modules.wip.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "wip.process", label = "生产工序", order = "create_date")
@Model.Service(remove = "@edit")
public class WipProcess extends Model {
    static Field product_id = Field.Many2one("wip.production").label("产品");
    static Field process_id = Field.Many2one("md.work_process").label("工序");
    static Field route_id = Field.Char().label("节点路由");
    static Field station_id = Field.Many2one("md.work_station").label("工位");
    static Field resource_id = Field.Many2one("md.work_resource").ondelete(DeleteMode.Restrict).label("制造资源");
    static Field workshop_id = Field.Many2one("md.workshop").related("resource_id.workshop_id");
    static Field ok_qty = Field.Float().label("合格数").defaultValue(0);
    static Field ng_qty = Field.Float().label("不合格数").defaultValue(0);
    static Field status = Field.Selection(new Options() {{
        put("start", "开始");
        put("done", "完成");
    }}).label("状态");
    static Field code = Field.Char().label("采集条码").length(2000);
    static Field code_type = Field.Selection(Selection.related("md.work_process_step", "code_type")).label("过站条码类型");
    static Field op_ids = Field.Many2many("md.staff", "wip_process_staff", "process_id", "staff_id").label("操作员");
}
