package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;
import java.util.stream.Collectors;

@Model.Meta(inherit = "mfg.product_order")
public class ProductOrder extends Model {
    static Field bom_version_id = Field.Many2one("md.bom_version").label("BOM编码");
    static Field bom_details_ids = Field.One2many("mfg.product_order_bom", "order_id").label("BOM");
    static Field parent_id = Field.Many2one("mfg.product_order").label("父订单");
    static Field child_ids = Field.One2many("mfg.product_order", "parent_id").label("子订单");

    @OnSaved("bom_version_id")
    public void onBomVersionSave(Records records) {
        for (Records record : records) {
            Records bom = record.getRec("bom_details_ids");
            if (!bom.any()) {
                Records ver = record.getRec("bom_version_id");
                if (ver.any()) {
                    Records details = record.getEnv().get("md.bom_details").find(Criteria.equal("version_id", ver.getId()));
                    List<Map<String, Object>> data = new ArrayList<>();
                    double planQty = record.getDouble("plan_qty");
                    for (Records detail : details) {
                        boolean isAlternative = Utils.toBoolean(detail.getBoolean("is_alternative"), false);
                        double qty = detail.getDouble("qty");
                        Map<String, Object> row = new HashMap<>();
                        row.put("material_id", detail.getRec("material_id").getId());
                        row.put("qty", isAlternative ? 0d : qty);
                        row.put("is_alternative", detail.get("is_alternative"));
                        row.put("main_material_id", detail.getRec("main_material_id").getId());
                        row.put("order_id", record.getId());
                        data.add(row);
                    }
                    bom.createBatch(data);
                }
            }
        }
    }

    @Constrains("bom_details_ids")
    public void onBomSave(Records records) {
        for (Records record : records) {
            Records bomIds = record.getRec("bom_details_ids");
            if (bomIds.any()) {
                // 先获取替代料对应主料的集合
                Set<String> mainMaterialCodeSet = bomIds.stream().filter(e -> Utils.toBoolean(e.getBoolean("is_alternative"), false))
                    .map(e -> e.getRec("main_material_id").getString("code")).collect(Collectors.toSet());
                Set<String> baseMaterialCodeSet = bomIds.stream().filter(e -> !Utils.toBoolean(e.getBoolean("is_alternative"), false))
                    .map(e -> e.getRec("material_id").getString("code")).collect(Collectors.toSet());
                mainMaterialCodeSet.removeAll(baseMaterialCodeSet);
                if (!mainMaterialCodeSet.isEmpty()) {
                    throw new ValidationException(records.l10n("替代料对应的主料不存在于当前bom列表,异常主料编码为: {%s}", String.join(",", mainMaterialCodeSet)));
                }
            }
        }
    }
}
