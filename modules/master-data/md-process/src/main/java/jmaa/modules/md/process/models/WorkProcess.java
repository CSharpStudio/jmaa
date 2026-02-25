package jmaa.modules.md.process.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.work_process", label = "工序",inherit = {"mixin.companies"})
public class WorkProcess extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称").required();
    static Field process_type = Field.Selection(new Options() {{
        put("assemble", "装配");
        put("inspect", "检验");
        put("test", "测试");
        put("repair", "维修");
        put("package", "包装");
        put("process", "加工");
    }}).required().label("工序分类");
    static Field product_family_id = Field.Many2one("md.product_family").label("产品族").help("空值表示通用工序，非空表示产品族专用工序");
    static Field step_ids = Field.One2many("md.work_process_step", "process_id").label("过站参数");
    static Field work_step_ids = Field.One2many("md.work_step", "process_id").label("工步");
    static Field collection_result = Field.Selection(new Options() {{
        put("ok", "通过");
        put("ok/ng", "OK/NG");
    }}).required().label("采集结果");
    static Field company_ids = Field.Many2many("res.company", "mfg_process_company", "process_id", "company_id");
    static Field defect_ids = Field.Many2many("md.defect", "md_work_process_defect", "process_id", "defect_id").label("缺陷");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
}
