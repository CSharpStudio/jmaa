package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "qsd.level_letter_table", label = "样本量字码表", authModel = "qsd.sampling_plan", order = "id")
public class LevelLetterTable extends Model {
    static Field limit_lower = Field.Integer().label("批量区间下限");
    static Field limit_upper = Field.Integer().label("批量区间上限");
    static Field limit = Field.Char().label("批量").compute("computeLimit");
    static Field s1 = Field.Char().label("特殊检验水平1");
    static Field s2 = Field.Char().label("特殊检验水平2");
    static Field s3 = Field.Char().label("特殊检验水平3");
    static Field s4 = Field.Char().label("特殊检验水平4");
    static Field g1 = Field.Char().label("一般检验水平1");
    static Field g2 = Field.Char().label("一般检验水平2");
    static Field g3 = Field.Char().label("一般检验水平3");

    public String computeLimit(Records record) {
        Integer lower = record.getInteger("limit_lower");
        if (Utils.large(lower, 500000)) {
            return "500001及以上";
        }
        return String.format("%s~%s", lower, record.get("limit_upper"));
    }
}
