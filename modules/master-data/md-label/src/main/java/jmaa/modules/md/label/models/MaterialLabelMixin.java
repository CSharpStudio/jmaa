package jmaa.modules.md.label.models;

import org.jmaa.sdk.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Model.Meta(name = "mixin.material_label", label = "物料标签插件")
public class MaterialLabelMixin extends AbstractModel {
    static Field label_id = Field.Many2one("lbl.material_label").label("物料标签");
    static Field sn = Field.Char().label("标签序列号");
    static Field product_lot = Field.Char().label("生产批次").related("label_id.product_lot");
    static Field product_date = Field.Date().label("生产日期").related("label_id.product_date");

    @Override
    public Records createBatch(Records rec, List<Map<String, Object>> valuesList) {
        List<String> labelIds = new ArrayList<>();
        for (Map<String, Object> values : valuesList) {
            String labelId = Utils.toString(values.get("label_id"));
            if (Utils.isNotEmpty(labelId)) {
                labelIds.add(labelId);
            }
        }
        Records label = rec.getEnv().get("lbl.material_label", labelIds);
        for (Map<String, Object> values : valuesList) {
            String labelId = Utils.toString(values.get("label_id"));
            if (Utils.isNotEmpty(labelId)) {
                values.put("sn", label.filter(r -> Utils.equals(r.getId(), labelId)).get("sn"));
            }
        }
        return (Records) callSuper(rec, valuesList);
    }
}
