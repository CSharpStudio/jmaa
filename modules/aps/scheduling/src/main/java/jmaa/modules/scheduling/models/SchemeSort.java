package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "as.scheme_sort", label = "排产方案排序", authModel = "as.task_scheduling")
public class SchemeSort extends Model {
    static Field seq = Field.Integer().label("序号").required();
    static Field property = Field.Char().label("字段").required();
    static Field asc = Field.Selection(new Options() {{
        put("asc", "升序");
        put("desc", "降序");
    }}).label("排序").defaultValue("asc");
    static Field detail_id = Field.Many2one("as.scheme_details").label("方案明细");
}
