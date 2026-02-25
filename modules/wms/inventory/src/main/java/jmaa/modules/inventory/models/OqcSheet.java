package jmaa.modules.inventory.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;

@Model.Meta(inherit = "oqc.sheet")
public class OqcSheet extends Model {
    static Field details_ids = Field.One2many("oqc.material_details", "oqc_id").label("送检明细");
    static Field related_model = Field.Char().label("相关单据模型");
    static Field related_id = Field.Many2oneReference("related_model").label("相关单据ID");

    @OnSaved("status")
    public void onSubmit(Records records) {
        for (Records record : records) {
            String status = record.getString("status");
            if ("done".equals(status)) {
                record.getRec("details_ids").set("status", "done");
            }
        }
    }
}
