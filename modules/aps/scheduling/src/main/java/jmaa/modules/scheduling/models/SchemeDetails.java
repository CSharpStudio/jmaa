package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "as.scheme_details", label = "排产方案明细", authModel = "as.task_scheduling")
public class SchemeDetails extends Model {
    static Field seq = Field.Integer().label("序号").required();
    static Field resource_rule = Field.Selection(new Options() {{
        put("utilize-rate", "利用率优先");
        put("efficiency", "效率优先");
    }}).label("资源分配").defaultValue("efficiency").help("效率优先：相似产品安排在相同资源，减少转产");
    static Field auxiliary_rule = Field.Selection(new Options() {{
        put("auto", "自动切换");
        put("ignore", "忽略");
    }}).label("辅助资源").defaultValue("ignore");
    static Field direction = Field.Selection(new Options() {{
        put("forward", "正排");
        put("backward", "倒排");
    }}).label("排程方向").defaultValue("forward");
    static Field sort_ids = Field.One2many("as.scheme_sort", "detail_id").label("排序");
    static Field filter_ids = Field.One2many("as.scheme_filter", "detail_id").label("筛选");
    static Field scheme_id = Field.Many2one("as.scheme").label("排产方案").required();
}
