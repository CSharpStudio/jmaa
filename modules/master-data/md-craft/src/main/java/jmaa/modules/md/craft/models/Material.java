package jmaa.modules.md.craft.models;

import org.jmaa.sdk.Criteria;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.util.KvMap;

@Model.Meta(inherit = "md.material")
public class Material extends Model {
    static Field craft_route_id = Field.Many2one("md.craft_route").label("制程路线");

    @OnSaved
    public void onCraftRouteSave(Records records) {
        Records productRoute = records.getEnv().get("md.craft_route_product");
        for (Records record : records) {
            Records route = record.getRec("craft_route_id");
            if (route.any()) {
                productRoute = productRoute.find(Criteria.equal("material_id", record.getId())
                    .and("craft_route_id", "=", route.getId()));
                if (productRoute.any()) {
                    productRoute.find(Criteria.equal("material_id", record.getId())).set("is_default", false);
                    productRoute.set("is_default", true);
                } else {
                    productRoute.create(new KvMap().set("material_id", record.getId())
                            .set("craft_route_id", route.getId()))
                        .set("is_default", true);
                }
            }
        }
    }
}
