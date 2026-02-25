package jmaa.modules.md.inventory.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(name = "md.warehouse", label = "仓库", present = {"code", "name"}, presentFormat = "{code}({name})", inherit = {"mixin.company"}, order = "code")
@Model.UniqueConstraint(name = "code_company_unique", fields = {"code", "company_id"})
public class Warehouse extends Model {
    static Field name = Field.Char().label("仓库名称").required();
    static Field code = Field.Char().label("仓库编码").required();
    static Field type = Field.Selection(new Options() {{
        put("physical", "实体仓");
        put("virtual", "虚拟仓");
    }}).label("类型").defaultValue("physical").required();
    static Field category = Field.Selection(new Options() {{
        put("raw", "原材料");
        put("semi-finished", "半成品");
        put("finished", "成品仓");
        put("auxiliary", "辅料仓");
        put("spare", "备品备件");
        put("tooling", "工装治具");
        put("line", "线边仓");
    }}).label("分类").defaultValue("raw").required();
    static Field frozen = Field.Boolean().label("冻结").help("冻结状态下的仓库，需要管理只允许进，不允许出").defaultValue(false);
    static Field active = Field.Boolean().label("是否有效").help("禁用状态下的仓库不允许进行任何业务数据的操作").defaultValue(true);
    static Field over_send = Field.Boolean().label("允许超发").defaultValue(true);
    static Field location_manage = Field.Boolean().label("库位管理").defaultValue(false);
    static Field company_id = Field.Many2one("res.company").label("组织").readonly();
    static Field address = Field.Char().label("仓库地址");
    static Field zip = Field.Char().label("邮编");
    static Field contact = Field.Char().label("联系人");
    static Field phone = Field.Char().label("联系电话");
    static Field location_ids = Field.One2many("md.store_location", "warehouse_id").label("库位");
    static Field area_ids = Field.One2many("md.store_area", "warehouse_id").label("库区");
    static Field user_ids = Field.Many2many("rbac.user", "md_warehouse_user", "wh_id", "user_id").label("用户");
    static Field workshop_ids = Field.Many2many("md.enterprise_model", "md_warehouse_workshop", "warehouse_id", "workshop_id")
        .label("供料车间").help("发料供应的车间").lookup(Criteria.equal("tpl_id.type", "workshop"));
}
