package jmaa.modules.label.manager.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.util.KvMap;

import java.util.Collections;
import java.util.Date;

@Model.Meta(name = "lbl.lot_num", label = "批次号", inherit = "mixin.material")
public class LotNum extends Model {
    static Field code = Field.Char().label("批次").required().unique();
    static Field product_date = Field.Date().label("生产日期");
    static Field lot_attr = Field.Char().label("批次属性");
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商");
    static Field seq = Field.Integer().label("序号").defaultValue(0).required();

    public String getLotNum(Records records, String materialId, Date productDate, String lotAttr, String supplierId) {
        if (Utils.equals("", lotAttr)) {
            lotAttr = null;
        }
        Records lot = records.find(Criteria.equal("product_date", productDate).and("material_id", "=", materialId)
            .and("lot_attr", "=", lotAttr).and("supplier_id", "=", supplierId));
        if (lot.any()) {
            return lot.getString("code");
        }
        Records material = records.getEnv().get("md.material", materialId);
        Records coding = material.getRec("lot_coding_id");
        if (!coding.any()) {
            coding = coding.find(Criteria.equal("code", "SYS-LOT"));
        }
        String code = (String) coding.call("createCode", Collections.emptyMap());
        try (Cursor cr = records.getEnv().getDatabase().openCursor()) {
            Environment env = new Environment(records.getEnv().getRegistry(), cr, records.getEnv().getUserId());
            env.get("lbl.lot_num").create(new KvMap()
                .set("code", code)
                .set("material_id", materialId)
                .set("product_date", productDate)
                .set("lot_attr", lotAttr)
                .set("supplier_id", supplierId));
            cr.commit();
            return code;
        }
    }
}
