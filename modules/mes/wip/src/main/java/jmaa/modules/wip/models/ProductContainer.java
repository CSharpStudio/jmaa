package jmaa.modules.wip.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "mfg.product_container", label = "生产容器")
public class ProductContainer extends Model {
    static Field code = Field.Char().label("容器编码").unique();
    static Field status = Field.Selection(new Options() {{
        put("ready", "可用");
        put("inuse", "使用中");
    }}).label("状态");
    static Field active = Field.Boolean().label("是否有效");
}
