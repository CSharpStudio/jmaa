package jmaa.modules.print.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.MetaModel;

import java.util.*;

/**
 * @author eric
 */
@Model.Meta(name = "print.template", label = "标签模板")
public class PrintTemplate extends Model {
    static Field name = Field.Char().label("名称").required().unique();
    static Field adapter = Field.Selection(new LinkedHashMap<String, String>() {{
        put("print.adapter.btw", "BarTender");
        put("print.adapter.client", "客户端打印");
        put("print.adapter.stimulsoft", "网页打印");
    }}).label("模板引擎").required();
    static Field file = Field.Binary().label("模板").attachment(false);
    static Field file_name = Field.Char().label("模板名称");
    static Field rule = Field.Selection(Selection.method("getRules")).label("打印规则").required();
    static Field category = Field.Selection().label("分类");
    static Field check_sum = Field.Char().label("哈希值").length(40);
    static Field upload_status = Field.Boolean().label("上传状态").defaultValue(false).readonly();
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);

    /**
     * 获取打印规则
     */
    public Map<String, String> getRules(Records rec) {
        Map<String, String> rules = new HashMap<>();
        for (MetaModel meta : rec.getEnv().getRegistry().getModels().values()) {
            if(meta.hasBase("print.rule")){
                rules.put(meta.getName(), meta.getLabel());
            }
        }
        return rules;
    }

    /**
     * 打印，根据打印规则生成打印数据，调用打印模板引擎生成打印内容
     */
    public Object print(Records rec, Map<String, Object> data) {
        Records rule = rec.getEnv().get(rec.getString("rule"));
        Object printData = rule.call("getData", data);
        Object fields = rule.call("getFields");
        return rec.getEnv().get(rec.getString("adapter")).call("print", rec.getId(), fields, printData);
    }

    @ServiceMethod(auth = "read")
    public Object designate(Records rec) {
        rec.ensureOne();
        Records rule = rec.getEnv().get(rec.getString("rule"));
        Object fields = rule.call("getFields");
        return rec.getEnv().get(rec.getString("adapter")).call("design", rec.getId(), fields, Collections.emptyList());
    }
}
