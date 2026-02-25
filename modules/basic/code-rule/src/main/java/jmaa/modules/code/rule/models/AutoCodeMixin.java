package jmaa.modules.code.rule.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;

/**
 * @author 梁荣振
 */
@Model.Meta(name = "code.auto_code", label = "自动编码Mixin")
public class AutoCodeMixin extends AbstractModel {
    /**
     * 重写添加默认值方法，生成的编码放进值的Map中
     *
     * @param rec
     * @param values
     * @return
     */
    @Override
    public Map<String, Object> addMissingDefaultValues(Records rec, Map<String, Object> values) {
        Map<String, Object> result = (Map<String, Object>) callSuper(rec, values);
        Map<String, Object> data = new HashMap<>();
        //处理组织代码
        Object companyId = result.get("company_id");
        if (companyId instanceof List) {
            companyId = ((List<Object>) companyId).get(0);
        }
        if (companyId != null) {
            Records records = rec.getEnv().get("res.company", companyId.toString());
            records.ensureOne();
            String invOrg = (String) records.get("org_code");
            data.put("org_code", invOrg);
        } else {
            String invOrg = rec.getEnv().getUser().getRec("company_id").getString("org_code");
            data.put("org_code", invOrg);
        }
        //获取编码规则的配置
        List<Object> codeRule = getCodeRule(rec, data);
        if (codeRule.size() > 1) {
            String value = (String) result.get(codeRule.get(0));
            if (Utils.isEmpty(value)) {
                Records coding = rec.getEnv().get("code.coding").find(Criteria.equal("code", codeRule.get(1)));
                if (!coding.any()) {
                    throw new ValidationException(rec.l10n("请配置编码规则：%s", codeRule.get(1)));
                }
                value = (String) coding.call("createCode", data);
                result.put((String) codeRule.get(0), value);
            }
        }
        return result;
    }

    /**
     * 获取编码规则的配置，[字段，编码规则的编码]
     *
     * @param rec
     * @param data 生成编码所需要的业务数据
     * @return
     */
    public List<Object> getCodeRule(Records rec, Map<String, Object> data) {
        MetaField code = rec.getMeta().findField("code");
        if (code != null) {
            return Arrays.asList("code", rec.getMeta().getName());
        }
        return Collections.emptyList();
    }
}
