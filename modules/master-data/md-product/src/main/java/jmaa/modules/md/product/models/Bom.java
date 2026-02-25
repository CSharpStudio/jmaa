package jmaa.modules.md.product.models;

import org.jmaa.sdk.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Model.Meta(name = "md.bom", label = "产品BOM", inherit = {"mixin.companies", "mixin.material"}, present = {"material_id"})
public class Bom extends Model {
    static Field material_id = Field.Many2one("md.material").label("产品").unique();
    static Field version_ids = Field.One2many("md.bom_version", "bom_id").label("产品版本");
    static Field company_ids = Field.Many2many("res.company", "md_bom_company", "bom_id", "company_id");
    static Field remark = Field.Char().label("备注");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);

    /**
     * 根据物料编码查BOM id，用于导入数据
     */
    public Map presentToId(Records rec, Collection<Object> values) {
        Records bom = rec.find(Criteria.in("material_id.code", values));
        Map<Object, String> result = new HashMap<>();
        for (Records row : bom) {
            result.put(row.getRec("material_id").get("code"), row.getId());
        }
        return result;
    }
}
