package jmaa.modules.wip.models;

import org.jmaa.sdk.BoolState;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "wip.route", label = "在制品工艺", logAccess = BoolState.False)
public class WipRoute extends Model {
    static Field current_node_id = Field.Many2one("mfg.work_order_route_node").label("当前路由");
    static Field current_ok_qty = Field.Float().label("当前合格数量").defaultValue(0);
    static Field current_ng_qty = Field.Float().label("当前不合格数量").defaultValue(0);
    static Field current_process_times = Field.Integer().label("当前工序采集次数").defaultValue(1);
    static Field next_node_ids = Field.Many2many("mfg.work_order_route_node", "wip_route_next_node", "wip_route_id", "node_id").label("后续路由");
    static Field route_id = Field.Many2one("mfg.work_order_route").label("工艺流程");
    static Field group_child_ids = Field.Many2many("mfg.work_order_route_node", "wip_route_group_child", "wip_route_id", "node_id").label("工序组子工序");
}
