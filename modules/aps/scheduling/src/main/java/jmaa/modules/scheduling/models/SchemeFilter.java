package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "as.scheme_filter", label = "排产方案筛选", authModel = "as.task_scheduling")
public class SchemeFilter extends Model {
    static Field seq = Field.Integer().label("序号").required();
    static Field property = Field.Char().label("字段").required();
    static Field logical = Field.Selection(new Options() {{
        put("and", "并且");
        put("or", "或者");
    }}).label("关系").defaultValue("and").required();
    static Field operator = Field.Selection(new Options() {{
        put(">", "大于");
        put(">=", "大于等于");
        put("<", "小于");
        put("<=", "小于等于");
        put("==", "等于");
        put("!=", "不等于");
        put("in", "包含");
        put("not-in", "不包含");
    }}).label("运算符").defaultValue("=").required();
    static Field detail_id = Field.Many2one("as.scheme_details").label("方案明细");
}
