package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Model.Meta(name = "wip.container", label = "生产容器")
public class Container extends Model {
    static Field code = Field.Char().label("编码").unique();
    static Field status = Field.Selection(new Options() {{
        put("in-use", "使用中");
        put("idle", "空闲");
        put("unusable", "不可用");
    }}).required().defaultValue("idle").label("状态");
    static Field capacity = Field.Float().label("标准容量").min(0d);
    static Field qty = Field.Float().label("数量").defaultValue(0).min(0d);
    static Field work_order_id = Field.Many2one("mfg.work_order").label("工单");
    static Field material_id = Field.Many2one("md.material").related("work_order_id.material_id").label("产品");
    static Field material_name_spec = Field.Char().related("work_order_id.material_id.name_spec").label("规格名称");

    @ServiceMethod(label = "解绑")
    public Object unbind(Records records) {
        Cursor cr = records.getEnv().getCursor();
        List<String> codes = records.stream().map(r -> r.getString("code")).collect(Collectors.toList());
        cr.execute("DELETE FROM wip_bind_code WHERE code in %s AND code_type=%s", Arrays.asList(codes, "container"));
        records.set("status", "idle");
        records.set("qty", 0);
        records.set("work_order_id", null);
        return Action.success();
    }

    @ServiceMethod(label = "设置是否可用")
    public Object setUsable(Records records) {
        for (Records record : records) {
            if ("idle".equals(record.getString("status"))) {
                record.set("status", "unusable");
            } else if ("unusable".equals(record.getString("status"))) {
                record.set("status", "idle");
            }
        }
        return Action.success();
    }
}
