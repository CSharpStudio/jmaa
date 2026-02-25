package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

import java.util.LinkedHashMap;

/**
 * @author eric
 */
@Model.Meta(inherit = "sales.order")
public class SalesOrder extends Model {
    static Field delivery_status = Field.Selection(new LinkedHashMap<String, String>() {{
        put("new", "未发货");
        put("delivering", "发货中");
        put("done", "已完成");
    }}).label("发货状态").defaultValue("new");
}
