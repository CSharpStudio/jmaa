package jmaa.modules.code.rule.models;

import org.jmaa.sdk.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Model.Meta(name = "code.matcher", label = "编码匹配")
public class CodeMatcher extends Model {
    static Field name = Field.Char().label("名称").required(true);
    // static Field type = Field.Char().label("类型").required(true);
    static Field type = Field.Selection(new Options() {{
        put("lbl.material_label", "物料标签条码");
        put("md.store_location", "库位条码");
        put("packing.package", "包装标签条码");
        put("lbl.product_label", "成品标签条码");
    }}).label("来源").defaultValue("lbl.material_label").required();
    static Field length = Field.Integer().label("条码长度").defaultValue(1).min(1).help("分割后,数据数量");
    static Field expression = Field.Char().label("表达式").required(true).help("字段只能使用驼峰命名，不要使用下划线！");
    static Field remark = Field.Char().label("备注").help("表达式中,序列号必须使用sn,物料必须使用materialCode,批次号必须使用lotNum,数量必须使用qty");
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);
    static Field is_create = Field.Boolean().label("是否生成条码").defaultValue(false);
    static Field source = Field.Selection(new Options() {
        {
            put("system", "内置");
            put("manual", "自建");
        }
    }).label("来源").defaultValue("manual");

    public static class Rule {
        public String type;
        public int length;
        public String expression;
        public String remark;
        public Boolean isCreate;
    }


    @OnSaved("name")
    public void onSaved(Records record) {
        // 数据会被缓存，添加的时候清除缓存
        record.getEnv().setTenantData("code.matcher", null);
    }

    /**
     * 根据条码匹配类型
     *
     * @param record
     * @param code
     * @return
     */
    public String match(Records record, String code) {
        if (!Utils.isEmpty(code)) {
            List<Rule> rules = (List<Rule>) record.getEnv().getTenantData("code.matcher");
            if (rules == null) {
                rules = new ArrayList<>();
                record.getEnv().setTenantData("code.matcher", rules);
                Records records = record.find(Criteria.equal("active", true));
                for (Records row : records) {
                    Rule rule = new Rule();
                    rule.expression = row.getString("expression");
                    rule.type = row.getString("type");
                    rule.length = row.getInteger("length");
                    rules.add(rule);
                }
            }
            // 避免使用当前接口地方会报错,适配一下
            for (Rule rule : rules) {
                Pattern r = Pattern.compile(rule.expression);
                Matcher m = r.matcher(code);
                if (!m.matches()) {
                    continue;
                }
                if (rule.length == 1) {
                    return rule.type;
                } else if (rule.length > 1 && Utils.equals(rule.length, m.groupCount())) {
                    // 可能存在长度不同,匹配规则相似的数据, 判断控制一下
                    return rule.type;
                }
            }
        }
        return null;
    }

    /**
     * 匹配标签条码
     *
     * @param record
     * @param code
     * @return
     */
    public Map<String, Object> labelMatch(Records record, String code) {
        if (!Utils.isEmpty(code)) {
            List<Rule> rules = (List<Rule>) record.getEnv().getTenantData("code.matcher");
            if (rules == null) {
                rules = new ArrayList<>();
                record.getEnv().setTenantData("code.matcher", rules);
                Records records = record.find(Criteria.equal("active", true));
                for (Records row : records) {
                    Rule rule = new Rule();
                    rule.expression = row.getString("expression");
                    rule.type = row.getString("type");
                    rule.length = row.getInteger("length");
                    rule.remark = row.getString("remark");
                    rule.isCreate = row.getBoolean("is_create");
                    rules.add(rule);
                }
            }
            Map<String, Object> map = new HashMap<>();
            for (Rule rule : rules) {
                // 这里需要匹配一次,直接将数据封装好了返回,不然调用地方还是得解析一次
                Pattern r = Pattern.compile(rule.expression);
                Matcher m = r.matcher(code);
                if (!m.matches()) {
                    // 提前下一轮
                    continue;
                }
                // 匹配成功了,可能有类似的,不一定全对
                if (rule.length > 1 && Utils.equals(rule.length, m.groupCount())) {
                    String expression = rule.expression;
                    List<String> matchCodeList = extractNamedGroups(expression);
                    for (String param : matchCodeList) {
                        String value = m.group(param);
                        map.put(param, value);
                    }
                    map.put("type", rule.type);
                    map.put("isCreate", rule.isCreate);
                    return map;
                } else if (rule.length == 1) {
                    // 其他标签规则
                    map.put("type", rule.type);
                    map.put("isCreate", rule.isCreate);
                    map.put("code", code);
                    return map;
                }
            }
        }
        return null;
    }

    /**
     * 从正则表达式中提取所有命名捕获组的名称
     */
    public List<String> extractNamedGroups(String regex) {
        List<String> groupNames = new ArrayList<>();
        // 匹配命名捕获组的正则表达式: (?<name>...)
        Pattern namedGroupPattern = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9_]*)>");
        Matcher matcher = namedGroupPattern.matcher(regex);
        while (matcher.find()) {
            groupNames.add(matcher.group(1));
        }
        return groupNames;
    }
}
