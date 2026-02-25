package jmaa.modules.wip.models;

import org.jmaa.sdk.*;

import java.util.List;
import java.util.Map;

@Model.Meta(name = "mfg.work_order_route", label = "工单工艺路线", authModel = "mfg.work_order")
@Model.Service(remove = {"create", "delete", "copy"})
public class WorkOrderRoute extends Model {
    static Field work_order_id = Field.Many2one("mfg.work_order").ondelete(DeleteMode.Cascade).label("工单");
    static Field node_ids = Field.One2many("mfg.work_order_route_node", "route_id").label("工艺节点详情");
    static Field canvas_width = Field.Integer().label("画布宽度").defaultValue(2000).min(500).required();
    static Field canvas_height = Field.Integer().label("画布高度").defaultValue(1600).min(500).required();
    static Field start_x = Field.Integer().label("开始X坐标").defaultValue(60);
    static Field start_y = Field.Integer().label("开始Y坐标").defaultValue(60);
    static Field end_x = Field.Integer().label("结束X坐标").defaultValue(260);
    static Field end_y = Field.Integer().label("结束Y坐标").defaultValue(60);

    @ServiceMethod(auth = "update", label = "添加工艺节点")
    public Map<String, Object> createNode(Records record, Map<String, Object> values, List<String> fields) {
        values.put("route_id", record.getId());
        Records node = record.getEnv().get("mfg.work_order_route_node").create(values);
        return node.read(fields).get(0);
    }
}
