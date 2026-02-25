package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "qsd.inspect_item", label = "检验项目")
public class InspectItem extends AbstractModel {
    static Field name = Field.Char().label("名称").required();
    static Field mark = Field.Selection(new Options() {{
        put("qual", "定性");
        put("quan", "定量");
    }}).label("检验标识").required(true);
    static Field category = Field.Selection(new Options() {{
        put("appearance", "外观");
        put("performance", "性能");
        put("reliability", "可靠性");
        put("durability", "耐久性");
        put("function", "功能");
    }}).label("检验类别").required().useCatalog();
    static Field tools = Field.Char().label("检验工具");
    static Field methods = Field.Char().label("检验方法");
    static Field standards = Field.Char().label("检验依据").length(1000);
    static Field limit_upper = Field.Char().label("规格上限");
    static Field limit_lower = Field.Char().label("规格下限");
    static Field unit = Field.Char().label("单位");
    static Field sampling_process_id = Field.Many2one("qsd.sampling_process").required().label("抽样过程");
    static Field remark = Field.Char().label("备注");
}
