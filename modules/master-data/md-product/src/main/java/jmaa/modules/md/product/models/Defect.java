package jmaa.modules.md.product.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.defect", label = "不良代码", order = "code")
public class Defect extends Model {
    static Field code = Field.Char().label("不良代码").required().unique();
    static Field name = Field.Char().label("不良名称").required();
    static Field grade = Field.Selection(new Options() {{
        put("a", "A");
        put("b", "B");
        put("c", "C");
        put("d", "D");
    }}).label("不良等级").required().useCatalog();
    static Field type = Field.Selection(new Options() {{
        put("appearance", "外观不良");
        put("structure", "结构不良");
        put("performance", "性能不良");
        put("packaging", "包装不良");
        put("other", "其他不良");
    }}).label("不良分类").required().useCatalog();
    static Field remark = Field.Char().label("备注");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
}
