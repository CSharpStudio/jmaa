package jmaa.modules.md.craft.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.craft_route_node", label = "制程路线节点", present = "process_id")
public class CraftRouteNode extends Model {
    static Field route_id = Field.Many2one("md.craft_route").label("制程路线").ondelete(DeleteMode.Cascade);
    static Field next_id = Field.Many2one("md.craft_route_node").label("后工序").ondelete(DeleteMode.SetNull);
    static Field process_id = Field.Many2one("md.craft_process").label("制程工艺");
    static Field craft_type_id = Field.Many2one("md.craft_type").related("process_id.craft_type_id").label("制程类型");
    static Field next_relationship = Field.Selection(new Options() {{
        put("es", "ES(结束->开始)");
        put("ss", "SS(开始->开始)");
    }}).label("前后关系").required().defaultValue("es");
    static Field transfer_batch = Field.Integer().label("转运批量");
    static Field transfer_time = Field.Integer().label("转运时间(秒)");
    static Field interval_days = Field.Integer().label("间隔天数");
    static Field x = Field.Integer().label("X坐标");
    static Field y = Field.Integer().label("Y坐标");
    static Field is_start = Field.Boolean().label("开始");
    static Field is_end = Field.Boolean().label("结束");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);

    @ActionMethod
    public Action processChangeAction(Records record) {
        return Action.attr().setValue("craft_type_id", record.getRec("process_id").get("craft_type_id"));
    }
}
