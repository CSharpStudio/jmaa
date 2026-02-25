package jmaa.modules.code.rule.models;

import org.jmaa.sdk.AbstractModel;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 编码规则结构定义，通过getParts方法提供编码的组成部分code.part。
 * 使用抽象模型不生成表，继承code.rule模型，重写getParts可以实现自定义的编码规则结构。
 * @author eric
 */
@Model.Meta(name = "code.rule", label = "通用")
public class CodeRule extends AbstractModel {
    public Map<String, String> getParts(Records rec) {
        return new LinkedHashMap<String, String>(16) {
            {
                put("code.part.chars_code", "固定编码");
                put("code.part.date_template", "日期编码");
                put("code.part.global_sequence", "全局序列号");
                put("code.part.day_sequence", "按日序列号");
                put("code.part.month_sequence", "按月序列号");
                put("${org_code}", "组织代码");
            }
        };
    }
}
