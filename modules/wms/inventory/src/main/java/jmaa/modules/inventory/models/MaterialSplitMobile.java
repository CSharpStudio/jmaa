package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;

@Model.Meta(name = "wms.material_split_mobile", label = "标签拆分")
@Model.Service(remove = "@edit")
public class MaterialSplitMobile extends ValueModel {
    static Field code = Field.Char().label("旧标签");
    static Field qty = Field.Float().label("标签数量");
    static Field sn = Field.Char().label("新标签");
    static Field label_qty = Field.Float().label("新标签数量");
    static Field message = Field.Char().label("信息");
    static Field print_old = Field.Boolean().label("打印旧标签").defaultValue(false);

    @ServiceMethod(label = "扫码", auth = "read")
    public Map<String, Object> scanMaterialCode(Records record, String code) {
        Environment env = record.getEnv();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records label = null;
        if (codes.length > 1) {
            Records material = env.get("md.material").find(Criteria.equal("code", codes[1]));
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                label = env.get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
            } else {
                throw new ValidationException(record.l10n("标签非序列号管控,请检查数据"));
            }
        } else {
            label = env.get("lbl.material_label").find(Criteria.equal("sn", code));
        }
        if (!label.any()) {
            throw new ValidationException(record.l10n("标签识别成功,但系统无标签数据,请检查数据"));
        }
        String id = label.getId();
        Records onhand = env.get("stock.onhand").find(Criteria.equal("label_id", id));
        if (!onhand.any()) {
            throw new ValidationException(record.l10n("标签[%s]识别成功,但标签无库存数据", code));
        }
        double usableQty = onhand.getDouble("usable_qty");
        resultMap.put("qty", usableQty);
        resultMap.put("message", record.l10n("标签[%s]识别成功", code));
        return resultMap;
    }

    @ServiceMethod(label = "扫码确认", auth = "read")
    public Map<String, Object> materialSplitConfirm(Records record, String code, Double splitQty, Boolean printOld) {
        Environment env = record.getEnv();
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records label = null;
        if (codes.length > 1) {
            Records material = env.get("md.material").find(Criteria.equal("code", codes[1]));
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                label = env.get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
            } else {
                throw new ValidationException(record.l10n("标签非序列号管控,请检查数据"));
            }
        } else {
            label = env.get("lbl.material_label").find(Criteria.equal("sn", code));
        }
        if (!label.any()) {
            throw new ValidationException(record.l10n("标签识别成功,但系统无标签数据,请检查数据"));
        }
        String id = label.getId();
        Records onhand = env.get("stock.onhand").find(Criteria.equal("label_id", id));
        if (!onhand.any()) {
            throw new ValidationException(record.l10n("标签[%s]识别成功,但标签无库存数据", code));
        }
        Map<String, Object> resultMap = (Map<String, Object>) label.call("printSplitLabel", label.getString("sn"), splitQty, printOld);
        resultMap.put("message", record.l10n("标签[%s]拆分成功,拆分数量[%s]", code, splitQty));
        return resultMap;
    }

    @ServiceMethod(label = "扫描包装标签", auth = "read")
    public Map<String, Object> scanPackageCode(Records record, String code) {
        Environment env = record.getEnv();
        Map<String, Object> resultMap = new HashMap<>();
        Records pkg = env.get("packing.package").find(Criteria.equal("code", code));
        if (!pkg.any()) {
            throw new ValidationException(record.l10n("包装标签[%s]不存在,请检查数据", code));
        }
        String stockRule = pkg.getRec("material_id").getString("stock_rule");
        if (!"sn".equals(stockRule)) {
            throw new ValidationException(record.l10n("包装标签[%s]对应库存规则非序列号管控,无需拆分", code));
        }
        resultMap.put("qty", pkg.getDouble("qty"));
        resultMap.put("message", record.l10n("包装标签[%s]识别成功", code));
        return resultMap;
    }

    @ServiceMethod(label = "扫描物料标签", auth = "read")
    public Map<String, Object> scanPackageSn(Records record, String sn) {
        Environment env = record.getEnv();
        Map<String, Object> resultMap = new HashMap<>();
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", sn);
        Records label = null;
        if (codes.length > 1) {
            Records material = env.get("md.material").find(Criteria.equal("code", codes[1]));
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                label = env.get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
            } else {
                throw new ValidationException(record.l10n("标签非序列号管控,请检查数据"));
            }
        } else {
            label = env.get("lbl.material_label").find(Criteria.equal("sn", sn));
        }
        if (!label.any()) {
            throw new ValidationException(record.l10n("标签识别成功,但系统无标签数据,请检查数据"));
        }
        resultMap.put("label_qty", label.getDouble("qty"));
        resultMap.put("message", record.l10n("物料标签[%s]识别成功", sn));
        return resultMap;
    }

    @ServiceMethod(label = "拆箱", auth = "read")
    public Map<String, Object> packageSplit(Records record, String code, String sn) {
        return (Map<String, Object>) record.getEnv().get("packing.package").call("splitLabel", code, sn);
    }

    @ServiceMethod(label = "合箱", auth = "read")
    public Map<String, Object> packageMerge(Records record, String code, String sn) {
        return (Map<String, Object>) record.getEnv().get("packing.package").call("mergeLabel", code, sn);
    }
}
