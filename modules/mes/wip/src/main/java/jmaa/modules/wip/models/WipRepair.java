package jmaa.modules.wip.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "wip.repair", label = "维修记录")
@Model.Service(remove = "@edit")
public class WipRepair extends Model {
    static Field product_id = Field.Many2one("wip.production").label("产品");
    static Field wip_defect_id = Field.Many2one("wip.defect").label("不良信息");
    static Field defect_id = Field.Many2one("md.defect").related("wip_defect_id.defect_id").label("不良代码");
    static Field defect_cause = Field.Selection(new Options() {{
        put("process", "工艺不良");
        put("material", "物料不良");
        put("operation", "操作损坏");
    }}).label("缺陷原因").required();
    static Field method = Field.Char().label("维修方法");
    static Field status = Field.Selection(new Options() {{
        put("suspend", "挂起");
        put("doing", "维修中");
        put("done", "完成");
    }}).label("维修状态");
    static Field tooling = Field.Char().label("维修工具");
    static Field result = Field.Selection(new Options() {{
        put("ok", "合格");
        put("scrap", "报废");
    }}).label("维修结果").required();

    static Field module_code = Field.Char().label("组件条码");
    static Field material_id = Field.Many2one("md.material").label("物料编码");
    static Field material_name_spec = Field.Char().related("material_id.name_spec").label("物料名称规格");
    static Field remark = Field.Char().label("备注");
}
