package jmaa.modules.label.manager.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;

import java.util.HashMap;
import java.util.Map;

/**
 * @author eric
 */
@Model.Meta(inherit = "packing.package")
public class PackingPackage extends Model {
    static Field print_template_id = Field.Many2one("print.template").label("打印模板");
    static Field stock_rule = Field.Selection().related("material_id.stock_rule");
    static Field category = Field.Selection().label("基本分类").related("material_id.category");

    @ServiceMethod(label = "打印")
    public Map<String, Object> print(Records records) {
        if (!records.any()) {
            throw new ValidationException("请选择要打印的标签");
        }
        Records printTemplate = records.first().getRec("print_template_id");
        for (Records rec : records) {
            if (!printTemplate.equals(rec.getRec("print_template_id"))) {
                throw new ValidationException("补打的标签模板不一致，不能一起打印");
            }
        }
        return (Map<String, Object>) printTemplate.call("print", new KvMap().set("package", records));
    }

    @ServiceMethod(label = "拆分")
    public Map<String, Object> splitLabel(Records record, String code, String sn) {
        Environment env = record.getEnv();
        Map<String, Object> resultMap = new HashMap<>();
        Records pkg = record.find(Criteria.equal("code", code));
        String baseId = pkg.getId();
        Records label = env.get("lbl.material_label");
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", sn);
        if (codes.length > 1) {
            Records material = env.get("md.material").find(Criteria.equal("code", codes[1]));
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                label = label.find(Criteria.equal("sn", codes[0]));
            } else {
                throw new ValidationException(record.l10n("标签非序列号管控,请检查数据"));
            }
        } else {
            label = label.find(Criteria.equal("sn", sn));
        }
        if (!label.any()) {
            throw new ValidationException(record.l10n("标签识别成功,但系统无标签数据,请检查数据"));
        }
        Records packageId = label.getRec("package_id");
        if (!packageId.any()) {
            throw new ValidationException(record.l10n("标签[%s]无关联包装,请检查数据", sn));
        }
        // 需要校验当前标签不是这个包装里面的
        if (recursion(record, packageId.getString("code"), baseId, label.getDouble("qty"), true)) {
            throw new ValidationException(record.l10n("当前标签非此包装数据,请检查数据"));
        }
        label.set("package_id", null);
        Map<String, Object> log = new HashMap<>();
        log.put("operation", "packing.package:split");
        log.put("related_id", pkg.getId());
        log.put("related_code", pkg.getString("code"));
        label.call("logStatus", log);
        resultMap.put("qty", label.getDouble("qty"));
        resultMap.put("message", record.l10n("标签[%s]拆分成功", sn));
        return resultMap;
    }

    public boolean recursion(Records record, String code, String packageId, Double qty, boolean flag) {
        if (Utils.isEmpty(code)) {
            return flag;
        }
        Records pkg = record.find(Criteria.equal("code", code));
        if (!pkg.any()) {
            throw new ValidationException(record.l10n("包装标签[%s]异常,层级关系错误,请检查数据", code));
        } else {
            if (pkg.getId().equals(packageId)) {
                flag = false;
            }
            pkg.set("qty", Utils.round(pkg.getDouble("qty") - qty));
            boolean upperCode = recursion(record, pkg.getString("upper_code"), packageId, qty, flag);
            if (!upperCode) {
                flag = false;
            }
        }
        return flag;
    }

    @ServiceMethod(label = "合并")
    public Map<String, Object> mergeLabel(Records record, String code, String sn) {
        Environment env = record.getEnv();
        Map<String, Object> resultMap = new HashMap<>();
        Records pkg = record.find(Criteria.equal("code", code));
        // 如果不是最下级,则提示
        Records parentId = record.find(Criteria.equal("parent_id", pkg.getId()));
        if (parentId.any()) {
            throw new ValidationException(record.l10n("当前包装标签,非最下级,请重新选择"));
        }
        String baseId = pkg.getId();
        Records label = env.get("lbl.material_label");
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", sn);
        if (codes.length > 1) {
            Records material = env.get("md.material").find(Criteria.equal("code", codes[1]));
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                label = label.find(Criteria.equal("sn", codes[0]));
            } else {
                throw new ValidationException(record.l10n("标签非序列号管控,请检查数据"));
            }
        } else {
            label = label.find(Criteria.equal("sn", sn));
        }
        if (!label.any()) {
            throw new ValidationException(record.l10n("标签识别成功,但系统无标签数据,请检查数据"));
        }
        Records packageId = label.getRec("package_id");
        if (packageId.any()) {
            throw new ValidationException(record.l10n("标签[%s]存在关联包装,请检查数据", sn));
        }
        if (recursion(record, code, baseId, -label.getDouble("qty"), true)) {
            throw new ValidationException(record.l10n("当前标签非此包装数据,请检查数据"));
        }
        // 需要校验当前标签不是这个包装里面的
        label.set("package_id", baseId);
        Map<String, Object> log = new HashMap<>();
        log.put("operation", "packing.package:merge");
        log.put("related_id", pkg.getId());
        log.put("related_code", pkg.getString("code"));
        label.call("logStatus", log);
        resultMap.put("qty", label.getDouble("qty"));
        resultMap.put("message", record.l10n("标签[%s]合并成功", sn));
        return resultMap;
    }
}
