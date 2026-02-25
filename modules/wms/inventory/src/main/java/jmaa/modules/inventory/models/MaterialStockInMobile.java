package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.core.Environment;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Model.Meta(name = "wms.material_stock_in_mobile", table = "stock_stock_in", label = "来料入库移动端", inherit = "stock.stock_in")
@Model.Service(remove = "@edit")
public class MaterialStockInMobile extends ValueModel {
    @ActionMethod
    public Object onWarehouseChange(Records record) {
        AttrAction action = new AttrAction();
        Records warehouse = record.getRec("warehouse_id");
        Records material = record.getRec("material_id");
        Records details = record.getEnv().get("stock.stock_in_details").find(Criteria.equal("material_id", material.getId())
            .and("status", "=", "to-stock").and("warehouse_id", "=", warehouse.getId()));
        if (details.any()) {
            action.setValue("qty", details.stream().mapToDouble(r -> r.getDouble("qty")).sum());
            action.setValue("suggest_location", record.call("findSuggestLocation", warehouse.getId(), material.getId()));
        } else {
            action.setValue("qty", null);
            action.setValue("suggest_location", null);
        }
        return action;
    }

    @ServiceMethod(label = "扫码入库", auth = "read")
    public Object stockIn(Records record, String code, String materialId, Double confirmQty, String location, boolean submit) {
        return callSuper(record, code, materialId, confirmQty, location, submit);
    }

    @ServiceMethod(label = "入库查询", auth = "read")
    public List<Map<String, Object>> searchByCode(Records rec, String code, Collection<String> fields, Criteria criteria, Integer offset, Integer limit, String order) {
        Environment env = rec.getEnv();
        Records warehouses = env.getUser().getRec("warehouse_ids");
        //按明细仓库和状态查入库单
        Criteria filter = Criteria.in("warehouse_id", warehouses.getIds());
        if (Utils.isNotBlank(code)) {
            String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
            if (codes.length > 1) {
                Records material = rec.getEnv().get("md.material").find(Criteria.equal("code", codes[1]));
                String stockRule = material.getString("stock_rule");
                if ("sn".equals(stockRule)) {
                    filter.and("sn", "=", codes[0]).and("status", "!=", "to-inspect");
                } else if ("lot".equals(stockRule) && codes.length > 2) {
                    filter.and("lot_num", "=", codes[2]).and("status", "=", "to-stock");
                } else {
                    filter.and("material_id", "=", material.getId()).and("status", "=", "to-stock");
                }
                Records details = env.get("stock.stock_in_details").find(filter);
                criteria.and("details_ids", "in", details.getIds());
                return rec.search(fields, criteria, offset, limit, order);
            }
            String type = (String) env.get("code.matcher").call("match", code);
            if (Utils.isEmpty(type) || "packing.package".equals(type)) {
                Records pkg = env.get("packing.package").find(Criteria.equal("code", code));
                if (pkg.any()) {
                    Records material = pkg.getRec("material_id");
                    String stockRule = material.getString("stock_rule");
                    if ("sn".equals(stockRule)) {
                        Records labels = env.get("lbl.material_label").find(Criteria.equal("package_id", pkg.getId()));
                        filter.and("label_id", "in", labels.getIds()).and("status", "!=", "to-inspect");
                    } else if ("lot".equals(stockRule) && codes.length > 2) {
                        Records lotPackage = env.get("md.lot_package").find(Criteria.equal("package_id", pkg.getId())
                            .and(Criteria.equal("material_id", material.getId())));
                        List<String> lotNum = lotPackage.stream().map(r -> r.getString("lot_num")).collect(Collectors.toList());
                        filter.and("lot_num", "in", lotNum).and("status", "=", "to-stock");
                    } else {
                        filter.and("material_id", "=", material.getId()).and("status", "=", "to-stock");
                    }
                    Records details = env.get("stock.stock_in_details").find(filter);
                    criteria.and("details_ids", "in", details.getIds());
                    return rec.search(fields, criteria, offset, limit, order);
                }
            }
            if (Utils.isEmpty(type) || "lbl.material_label".equals(type) || "lbl.product_label".equals(type)) {
                Records label = env.get("lbl.material_label").find(Criteria.equal("sn", code));
                if (label.any()) {
                    filter.and("sn", "=", code).and("status", "!=", "to-inspect");
                    Records details = env.get("stock.stock_in_details").find(filter);
                    criteria.and("details_ids", "in", details.getIds());
                    return rec.search(fields, criteria, offset, limit, order);
                }
            }
            if ("md.material".equals(type)) {
                Records material = env.get("md.material").find(Criteria.equal("code", code));
                if (material.any()) {
                    filter.and("material_id", "=", material.getId()).and("status", "=", "to-stock");
                    Records details = env.get("stock.stock_in_details").find(filter);
                    criteria.and("details_ids", "in", details.getIds());
                    return rec.search(fields, criteria, offset, limit, order);
                }
            }
            criteria.and(Criteria.equal("code", code).or("related_code", "=", code));
            Records details = env.get("stock.stock_in_details").find(filter);
            criteria.and("details_ids", "in", details.getIds());
            return rec.search(fields, criteria, offset, limit, order);
        }
        filter.and("status", "=", "to-stock");
        Records details = env.get("stock.stock_in_details").find(filter);
        criteria.and("details_ids", "in", details.getIds());
        return rec.search(fields, criteria, offset, limit, order);
    }
}
