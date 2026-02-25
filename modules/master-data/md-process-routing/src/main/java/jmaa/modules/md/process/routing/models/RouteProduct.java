package jmaa.modules.md.process.routing.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "pr.route_product", label = "产品工艺路线", inherit = "mixin.material", authModel = "pr.route")
public class RouteProduct extends Model {
    static Field material_id = Field.Many2one("md.material").label("产品");
    static Field route_id = Field.Many2one("pr.route").label("工艺路线");
    static Field work_order_type = Field.Selection(new Options() {{
        put("volume", "量产");
        put("trial", "试产");
        put("rework", "返工");
        put("outsource", "委外");
    }}).label("工单类型").required();
}
