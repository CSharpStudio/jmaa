package jmaa.modules.bbs.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.fields.RelationalField;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Model.Meta(name = "bbs.template", label = "消息")
public class Template extends Model {
    static Field name = Field.Char().label("名称").required();
    static Field model = Field.Char().label("应用于");
    static Field subject = Field.Char().label("标题").length(2000);
    static Field content = Field.Text().label("内容");
    static Field fields = Field.Text().label("字段说明").compute("computeFields");

    public String computeFields(Records record) {
        String model = record.getString("model");
        if (record.getEnv().getRegistry().contains(model)) {
            MetaModel meta = record.getEnv().getRegistry().get(model);
            return meta.getFields().values().stream().filter(f -> !f.isAuto() && !(f instanceof RelationalField))
                .map(f -> f.getName() + ":" + record.l10n(f.getLabel())).collect(Collectors.joining(";"));
        }
        return "";
    }

    @ActionMethod
    public Action onModelChange(Records record) {
        AttrAction action = new AttrAction();
        action.setValue("fields", computeFields(record));
        return action;
    }

    public Map<String, String> evaluate(Records record, Map<String, String> values) {
        Map<String, String> result = new HashMap<>();
        String subject = record.getString("subject");
        if (Utils.isNotEmpty(subject)) {
            result.put("subject", replacePlaceholders(subject, values));
        }
        String content = record.getString("content");
        if (Utils.isNotEmpty(content)) {
            result.put("content", replacePlaceholders(content, values));
        }
        return result;
    }

    public static String replacePlaceholders(String input, Map<String, String> replacements) {
        if (input == null || input.isEmpty() || replacements == null || replacements.isEmpty()) {
            return input;
        }
        // 创建正则表达式模式，匹配${key}格式的占位符
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(input);

        // 使用StringBuffer存储替换结果
        StringBuffer result = new StringBuffer();

        // 遍历匹配结果
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = replacements.get(placeholder);

            // 如果找到了替换值，则替换占位符
            if (replacement != null) {
                // 处理替换值中的转义字符
                replacement = replacement.replace("\\", "\\\\")
                    .replace("$", "\\$");
                matcher.appendReplacement(result, replacement);
            } else {
                // 如果没有找到替换值，则保留原占位符
                matcher.appendReplacement(result, matcher.group(0));
            }
        }
        // 将剩余部分添加到结果中
        matcher.appendTail(result);
        return result.toString();
    }
}
