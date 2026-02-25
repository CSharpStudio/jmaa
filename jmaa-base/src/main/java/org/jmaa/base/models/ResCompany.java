package org.jmaa.base.models;

import org.jmaa.sdk.*;

/**
 * 公司组织
 *
 * @author Eric Liang
 */
@Model.Meta(name = "res.company", label = "公司")
public class ResCompany extends Model {
    static Field name = Field.Char().label("名称").required().unique();
    static Field inv_org = Field.Char().label("库存组织").required().help("对应ERP库存组织编码").unique();
    static Field org_code = Field.Char().label("组织代码").required().length(3).help("长度不超过三位，可以配置到编码规则组成中，建议使用英文或数字");
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);
}
