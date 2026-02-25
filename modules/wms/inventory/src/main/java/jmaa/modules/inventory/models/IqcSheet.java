package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = "iqc.sheet")
public class IqcSheet extends Model {
    static Field details_ids = Field.One2many("iqc.material_details", "iqc_id").label("送检明细");
    static Field related_model = Field.Char().label("相关单据模型");
    static Field related_id = Field.Many2oneReference("related_model").label("相关单据ID");

    @OnSaved("status")
    public void onSubmit(Records records) {
        for (Records record : records) {
            String status = record.getString("status");
            if ("done".equals(status)) {
                String result = record.getString("result");
                if ("ok".equals(result)) {
                    record.getRec("details_ids").set("status", "to-stock");
                } else {
                    record.getRec("details_ids").set("status", "inspect-ng");
                }
            } else if ("exempted".equals(status)) {
                record.getRec("details_ids").set("status", "to-stock");
            }
        }
    }
}
