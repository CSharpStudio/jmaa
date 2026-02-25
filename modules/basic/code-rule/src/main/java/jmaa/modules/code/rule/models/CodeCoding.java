package jmaa.modules.code.rule.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.core.Registry;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;

/**
 * @author eric
 */
@Model.Meta(name = "code.coding", label = "编码规则")
public class CodeCoding extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称").required();
    static Field rule = Field.Selection(Selection.method("getRules")).label("类型").required(true).help("规则适用类型").defaultValue("code.rule");
    static Field source = Field.Selection(new Options() {{
        put("system", "内置");
        put("manual", "自建");
    }}).label("来源").defaultValue("manual").copy(false);
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);
    static Field description = Field.Char().label("描述");
    static Field code_part_ids = Field.One2many("code.part", "coding_id").label("规则组成");

    @ServiceMethod(auth = "read", doc = "根据编码规则类型获取编制组成部分")
    public Map<String, String> getCodeParts(Records rec, String rule) {
        return (Map<String, String>) rec.getEnv().get(rule).call("getParts");
    }

    public Map<String, String> getRules(Records rec) {
        Map<String, String> rules = new HashMap<>();
        Registry reg = rec.getEnv().getRegistry();
        rules.put("code.rule", reg.get("code.rule").getLabel());
        for (MetaModel meta : reg.getModels().values()) {
            if (meta.hasBase("code.rule")) {
                rules.put(meta.getName(), meta.getLabel());
            }
        }
        return rules;
    }

    /**
     * 批量生成编码
     *
     * @param rec
     * @param qty 生成数量
     * @return
     */
    public List<String> createCodes(Records rec, int qty, Map<String, String> data) {
        Records parts = rec.getRec("code_part_ids");
        if (!parts.any()) {
            throw new ValidationException(rec.l10n("编码规则[%s]未配置规则组成", rec.get("code")));
        }
        List<List<String>> codeParts = new ArrayList<>();
        for (Records part : parts) {
            String partId = (String) part.get("id");
            String code = (String) part.get("code");
            if (Utils.isNotEmpty(code)) {
                if (code.startsWith("$")) {
                    //如果是占位符直接使用占位符${xxx}
                    String key = code.substring(2, code.length() - 1);
                    String value = null;
                    if ("org_code".equals(key)) {
                        value = rec.getEnv().getUser().getRec("company_id").getString("org_code");
                    } else {
                        value = data.get(key);
                    }
                    List<String> codePart = Collections.nCopies(qty, value);
                    codeParts.add(codePart);
                } else {
                    if (rec.getEnv().getRegistry().contains(code)) {
                        List<String> codePart = (List<String>) rec.getEnv().get(code).call("getPartCode", partId, qty);
                        codeParts.add(codePart);
                    } else {
                        throw new ValidationException(rec.l10n("找不到编码规则[%s]规则组成[%s]", rec.get("code"), code));
                    }
                }
            }
        }
        List<String> result = new ArrayList<>(qty);
        for (int i = 0; i < qty; i++) {
            StringJoiner code = new StringJoiner("");
            for (List<String> part : codeParts) {
                code.add(part.get(i));
            }
            result.add(code.toString());
        }
        return result;
    }

    /**
     * 生成编码
     *
     * @param rec
     * @param data
     * @return
     */
    public String createCode(Records rec, Map<String, String> data) {
        return createCodes(rec, 1, data).get(0);
    }

    @Override
    public boolean delete(Records rec) {
        if ("system".equals(rec.get("source"))) {
            throw new ValidationException(rec.l10n("系统默认编码规则不能删除"));
        }
        return (Boolean) rec.callSuper(CodeCoding.class, "delete");
    }
}
