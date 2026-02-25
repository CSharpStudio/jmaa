package jmaa.modules.md.enterprise.models;

import org.jmaa.sdk.*;

/**
 * @author 梁荣振
 */
@Model.Meta(name = "md.enterprise_tpl", label = "企业层级", authModel = "md.enterprise_model")
public class EnterpriseTemplate extends Model {
    static Field name = Field.Char().label("名称").required(true);
    static Field parent_id = Field.Many2one("md.enterprise_tpl").label("父层级");
    static Field child_ids = Field.One2many("md.enterprise_tpl", "parent_id").label("子层级");
    static Field type = Field.Selection(new Options() {{
        put("group", "集团");
        put("company", "公司");
        put("factory", "工厂");
        put("department", "部门");
        put("workshop", "车间");
        put("warehouse", "仓库");
        put("line", "生产线");
    }}).label("类型");
}
