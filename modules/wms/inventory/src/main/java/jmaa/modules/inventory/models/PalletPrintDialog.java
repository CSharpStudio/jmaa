package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wangjian
 * <p>
 * &#064;date 2025/3/29 10:46
 */
@Model.Meta(name = "wms.pallet_print_dialog", label = "码盘标签打印", authModel = "wms.material_receipt")
@Model.Service(remove = "@edit")
public class PalletPrintDialog extends ValueModel {
    static Field material_id = Field.Many2one("md.material").label("物料").required();
    static Field qty = Field.Float().label("数量").required();
    static Field package_qty = Field.Float().label("包装数量").readonly();
    static Field packing_level_id = Field.Many2one("md.packing_level").label("包装规则");
    static Field template_id = Field.Many2one("print.template").label("标签模板").required();

    @ActionMethod
    public Action onPackingLevelChange(Records record) {
        AttrAction action = Action.attr();
        Records packing = record.getRec("packing_level_id");
        action.setValue("package_qty", packing.getInteger("package_qty"));
        action.setValue("template_id", packing.getRec("print_template_id").getPresent());
        return action;
    }

    @ServiceMethod(auth = "read", label = "读取码盘包装信息")
    public Object searchPalletPacking(Records record, List<String> fields, String materialId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> values = new ArrayList<>();
        result.put("hasNext", false);
        result.put("values", values);
        Records material = record.getEnv().get("md.material", materialId);
        Records packingRule = material.getRec("packing_rule_ids");
        for (Records rule : packingRule) {
            Records packing = rule.getRec("rule_id").getRec("level_ids").filter(p -> p.getBoolean("is_in"));
            values.addAll(packing.read(fields));
        }
        return result;
    }
}
