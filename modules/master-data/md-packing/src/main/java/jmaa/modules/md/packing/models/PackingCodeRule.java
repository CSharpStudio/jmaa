package jmaa.modules.md.packing.models;

import org.jmaa.sdk.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Model.Meta(name = "code.rule.packing", label = "包装编码规则", inherit = "code.rule")
public class PackingCodeRule extends AbstractModel {
    public Map<String, String> getParts(Records rec) {
        return new Options() {{
            put("code.part.chars_code", "固定编码");
            put("code.part.date_template", "日期编码算法");
            put("code.part.global_sequence", "普通序列号算法");
            put("code.part.day_sequence", "按日期序列号算法");
            put("code.part.month_sequence", "编码规则月流水");
            put("${org_code}", "组织代码");
        }};
    }
}
