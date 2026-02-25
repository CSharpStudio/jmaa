package jmaa.modules.wip.models;

import org.jmaa.sdk.DeleteMode;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "mfg.work_order_route_node", label = "工单工艺节点", present = "process_id", order = "seq", inherit = "pr.route_node", authModel = "mfg.work_order")
@Model.Service(remove = "@edit")
public class WorkOrderRouteNode extends Model{
    static Field version_id = Field.Many2one("pr.route_version").ondelete(DeleteMode.SetNull);
    static Field route_id = Field.Many2one("mfg.work_order_route").ondelete(DeleteMode.Cascade);
    static Field ok_id = Field.Many2one("mfg.work_order_route_node");
    static Field ng_id = Field.Many2one("mfg.work_order_route_node");
    static Field parent_id = Field.Many2one("mfg.work_order_route_node");
}
