package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;

import java.util.Collections;
import java.util.List;

@Model.Meta(name = "lbl.material_label_status_mobile", inherit = "lbl.material_label_status", label = "标签条码状态-移动端", table = "lbl_material_label_status")
public class MaterialLabelStatusMobile extends Model {
    @ServiceMethod(auth = "read", label = "按条码查询标签状态")
    public Object searchByCode(Records records, String code, List<String> fields, int offset, int limit) {
        Environment env = records.getEnv();
        if (Utils.isBlank(code)) {
            return new KvMap().set("values", Collections.emptyList()).set("hasNext", false);
        }
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        if (codes.length > 1) {
            Records material = env.get("md.material").find(Criteria.equal("code", codes[1]));
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                return records.searchLimit(fields, Criteria.equal("sn", codes[0]), offset, limit, null);
            }
            throw new ValidationException(records.l10n("不支持[%s]的物料标签状态查询", material.getSelection("stock_rule")));
        }
        return records.searchLimit(fields, Criteria.equal("sn", code), offset, limit, null);
    }
}
