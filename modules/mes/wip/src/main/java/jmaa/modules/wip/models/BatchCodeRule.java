package jmaa.modules.wip.models;

import org.jmaa.sdk.AbstractModel;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;

import java.util.LinkedHashMap;
import java.util.Map;

@Model.Meta(name = "code.rule.batch", label = "批次编码规则", inherit = "code.rule")
public class BatchCodeRule extends AbstractModel {
    public Map<String, String> getParts(Records rec) {
        return new LinkedHashMap<String, String>(16) {
            {
                put("code.part.chars_code", "固定编码");
                put("code.part.date_template", "日期编码算法");
                put("code.part.global_sequence", "普通序列号算法");
                put("code.part.day_sequence", "按日期序列号算法");
                put("code.part.month_sequence", "编码规则月流水");
                put("${org_code}", "组织代码");
            }
        };
    }
}
